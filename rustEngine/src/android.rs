use std::collections::{HashMap, HashSet};
use std::path::PathBuf;
use std::sync::Arc;

use tokio::sync::Mutex as TokioMutex;

use crate::bandwidth::BandwidthManager;
use crate::commands::auth;
use crate::commands::fs;
use crate::commands::network;
use crate::commands::preview;
use crate::commands::streaming::{self, StreamConfig};
use crate::commands::TransferProgressListener;
use crate::commands::TelegramState;
use crate::models::{AuthResult, FileMetadata, FolderMetadata};

/// Namespaced error surfaced to the Android shell.
///
/// Every native method is fallible and returns `Result<T, EngineError>`.
/// UniFFI maps the variants to Kotlin exception subclasses, so the UI layer
/// catches them as plain `Exception` and reads `.message`.
#[derive(Debug, uniffi::Error)]
#[uniffi(flat_error)]
pub enum EngineError {
    /// Telegram/MTProto-level failure (auth, network, flood waits, ...).
    Telegram(String),
    /// Local/plumbing failure (I/O, missing session, internal invariant).
    Internal(String),
}

impl std::fmt::Display for EngineError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            EngineError::Telegram(msg) => write!(f, "Telegram error: {}", msg),
            EngineError::Internal(msg) => write!(f, "Engine error: {}", msg),
        }
    }
}

impl From<String> for EngineError {
    fn from(e: String) -> Self {
        EngineError::Telegram(e)
    }
}

impl From<&str> for EngineError {
    fn from(e: &str) -> Self {
        EngineError::Telegram(e.to_string())
    }
}

impl std::error::Error for EngineError {}

/// Generate a random 32-character hex token for streaming server auth
fn generate_stream_token() -> String {
    let mut rng = rand::thread_rng();
    let bytes: Vec<u8> = (0..16).map(|_| rng.gen()).collect();
    bytes.iter().map(|b| format!("{:02x}", b)).collect()
}

/// Unified native API bridge over the grammers MTProto client.
///
/// Thread-safety model:
/// - A single multi-threaded Tokio runtime is created in `new()` and owned by
///   the engine for its whole lifetime. The grammers `SenderPool` network
///   runner is spawned onto this runtime and keeps running between calls, so
///   the client is never torn down and re-instantiated per request.
/// - Every exported method is synchronous at the FFI boundary and internally
///   runs its async command on that persistent runtime via `block_on`. The
///   Android layer therefore simply dispatches to these methods on
///   `Dispatchers.IO` (see the ViewModels / EngineTelegramRepository).
///
/// Shared state (`TelegramState`) is reused across all method invocations and
/// is never recreated unless `logout`/`connect` explicitly clears it.
#[derive(uniffi::Object)]
pub struct TelegramDriveEngine {
    runtime: Arc<tokio::runtime::Runtime>,
    state: Arc<TelegramState>,
    bw: std::sync::Mutex<Option<Arc<BandwidthManager>>>,
    stream_token: String,
}

#[uniffi::export]
impl TelegramDriveEngine {
    #[uniffi::constructor]
    pub fn new() -> Arc<Self> {
        // Dedicated runtime: all grammers async code (including the SenderPool
        // runner) executes here and outlives any single FFI call.
        let runtime = Arc::new(
            tokio::runtime::Builder::new_multi_thread()
                .enable_all()
                .build()
                .expect("Failed to build Tokio runtime"),
        );

        let state = Arc::new(TelegramState {
            client: Arc::new(TokioMutex::new(None)),
            login_token: Arc::new(TokioMutex::new(None)),
            password_token: Arc::new(TokioMutex::new(None)),
            api_id: Arc::new(TokioMutex::new(None)),
            runner_shutdown: Arc::new(std::sync::Mutex::new(None)),
            runner_count: Arc::new(std::sync::atomic::AtomicU32::new(0)),
            peer_cache: Arc::new(tokio::sync::RwLock::new(HashMap::new())),
            cancelled_transfers: Arc::new(tokio::sync::RwLock::new(HashSet::new())),
            data_dir: Arc::new(std::sync::RwLock::new(None)),
            cache_dir: Arc::new(std::sync::RwLock::new(None)),
            progress_listener: Arc::new(tokio::sync::RwLock::new(None)),
        });

        let stream_token = generate_stream_token();

        Arc::new(TelegramDriveEngine {
            runtime,
            state,
            bw: std::sync::Mutex::new(None),
            stream_token,
        })
    }

    // ------------------------------------------------------------------
    // Host plumbing
    // ------------------------------------------------------------------

    /// Configure host-private storage + cache directories. Must be called
    /// before the first `connect`. The Android app passes
    /// `context.filesDir.absolutePath` / `context.cacheDir.absolutePath`.
    pub fn set_storage_paths(&self, data_dir: String, cache_dir: String) {
        {
            let mut guard = self.state.data_dir.write().unwrap();
            *guard = Some(PathBuf::from(&data_dir));
        }
        {
            let mut guard = self.state.cache_dir.write().unwrap();
            *guard = Some(PathBuf::from(&cache_dir));
        }
        // Ensure both exist and (re)bind the bandwidth manager to data_dir.
        let _ = std::fs::create_dir_all(&data_dir);
        let _ = std::fs::create_dir_all(&cache_dir);
        let mut bw_guard = self.bw.lock().unwrap();
        *bw_guard = Some(Arc::new(BandwidthManager::new(
            &std::path::Path::new(&data_dir),
        )));
    }

    /// Register the object that receives upload/download progress ticks.
    /// Pass a fresh implementation whenever the UI registers a new collector.
    pub fn set_progress_listener(&self, listener: Box<dyn TransferProgressListener>) {
        let listener: Arc<dyn TransferProgressListener> = Arc::from(listener);
        let state = self.state.clone();
        self.drive(async move {
            *state.progress_listener.write().await = Some(listener);
        });
    }

    /// Forward a UI/log message to the native logging pipeline.
    pub fn log(&self, message: String) {
        log::info!("[NATIVE] {}", message);
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------

    /// Initialize the Telegram client (idempotent) and remember `api_id` so
    /// automatic reconnection can rebuild the session later.
    pub fn connect(&self, api_id: i32) -> Result<bool, EngineError> {
        let state = self.state.clone();
        self.drive(async move { auth::cmd_connect(&state, api_id).await })
            .map_err(Into::into)
    }

    /// Verify the live connection; attempts one automatic reconnect using the
    /// last stored `api_id` when the ping fails.
    pub fn check_connection(&self) -> Result<bool, EngineError> {
        let state = self.state.clone();
        self.drive(async move { auth::cmd_check_connection(&state).await })
            .map_err(Into::into)
    }

    /// Cheap socket probe against Telegram's DC to distinguish "offline" from
    /// "not authorized". Never touches the grammers client.
    pub fn is_network_available(&self) -> Result<bool, EngineError> {
        self.drive(network::is_network_available())
            .map_err(Into::into)
    }

    /// Start passwordless login: request an SMS/Telegram code for `phone`.
    pub fn request_login_code(
        &self,
        phone: String,
        api_id: i32,
        api_hash: String,
    ) -> Result<String, EngineError> {
        let state = self.state.clone();
        self.drive(async move {
            auth::cmd_auth_request_code(phone, api_id, api_hash, &state).await
        })
        .map_err(Into::into)
    }

    /// Confirm the received SMS code. Returns `next_step == "password"` when
    /// two-step verification is required.
    pub fn sign_in(&self, code: String) -> Result<AuthResult, EngineError> {
        let state = self.state.clone();
        self.drive(async move { auth::cmd_auth_sign_in(code, &state).await })
            .map_err(Into::into)
    }

    /// Submit the 2FA password when sign_in reported a password step.
    pub fn check_password(&self, password: String) -> Result<AuthResult, EngineError> {
        let state = self.state.clone();
        self.drive(async move { auth::cmd_auth_check_password(password, &state).await })
            .map_err(Into::into)
    }

    /// QR login step 1 — returns a `tg://login?token=...` URL to render.
    pub fn auth_qr_login(&self, api_id: i32, api_hash: String) -> Result<String, EngineError> {
        let state = self.state.clone();
        self.drive(async move { auth::cmd_auth_qr_login(api_id, api_hash, &state).await })
            .map_err(Into::into)
    }

    /// QR login step 2 — poll until the phone confirms the token.
    pub fn auth_qr_poll(&self) -> Result<AuthResult, EngineError> {
        let state = self.state.clone();
        self.drive(async move { auth::cmd_auth_qr_poll(&state).await })
            .map_err(Into::into)
    }

    /// Sign out, stop the network runner and wipe the local session.
    pub fn logout(&self) -> Result<bool, EngineError> {
        let state = self.state.clone();
        self.drive(async move { auth::cmd_logout(&state).await })
            .map_err(Into::into)
    }

    // ------------------------------------------------------------------
    // File storage system
    // ------------------------------------------------------------------

    /// List media messages inside a folder channel. `folder_id == null`
    /// targets the user's own Saved Messages.
    pub fn get_files(&self, folder_id: Option<i64>) -> Result<Vec<FileMetadata>, EngineError> {
        let state = self.state.clone();
        self.drive(async move { fs::cmd_get_files(folder_id, &state).await })
            .map_err(Into::into)
    }

    /// Scan every dialog for channels tagged as Telegram Drive folders
    /// (title suffix `[TD]` or about marker `[telegram-drive-folder]`).
    pub fn scan_folders(&self) -> Result<Vec<FolderMetadata>, EngineError> {
        let state = self.state.clone();
        self.drive(async move { fs::cmd_scan_folders(&state).await })
            .map_err(Into::into)
    }

    /// Create a new storage folder (broadcast channel) with TTL disabled.
    pub fn create_folder(&self, name: String) -> Result<FolderMetadata, EngineError> {
        let state = self.state.clone();
        self.drive(async move { fs::cmd_create_folder(name, &state).await })
            .map_err(Into::into)
    }

    /// Delete a storage folder channel entirely.
    pub fn delete_folder(&self, folder_id: i64) -> Result<bool, EngineError> {
        let state = self.state.clone();
        self.drive(async move { fs::cmd_delete_folder(folder_id, &state).await })
            .map_err(Into::into)
    }

    /// Upload a local file into the given folder.
    /// `transfer_id` is a client-generated correlation id (may be empty) that
    /// `cancel_transfer` uses to abort the transfer.
    pub fn upload_file(
        &self,
        path: String,
        folder_id: Option<i64>,
        transfer_id: String,
    ) -> Result<String, EngineError> {
        let state = self.state.clone();
        let bw = self.bw()?;
        let tid = if transfer_id.trim().is_empty() { None } else { Some(transfer_id) };
        self.drive(async move { fs::cmd_upload_file(path, folder_id, tid, &state, &bw).await })
            .map_err(Into::into)
    }

    /// Download a file (by message id) to `save_path`.
    /// `transfer_id` may be empty; when supplied it enables progress + cancel.
    pub fn download_file(
        &self,
        message_id: i32,
        folder_id: Option<i64>,
        save_path: String,
        transfer_id: String,
    ) -> Result<String, EngineError> {
        let state = self.state.clone();
        let bw = self.bw()?;
        let tid = if transfer_id.trim().is_empty() { None } else { Some(transfer_id) };
        self.drive(async move {
            fs::cmd_download_file(message_id, save_path, folder_id, tid, &state, &bw).await
        })
        .map_err(Into::into)
    }

    /// Request cooperative cancellation of an in-flight transfer.
    pub fn cancel_transfer(&self, transfer_id: String) -> Result<bool, EngineError> {
        let state = self.state.clone();
        self.drive(async move { fs::cmd_cancel_transfer(transfer_id, &state).await })
            .map_err(Into::into)
    }

    /// Delete a single media message from a folder.
    pub fn delete_file(
        &self,
        message_id: i32,
        folder_id: Option<i64>,
    ) -> Result<bool, EngineError> {
        let state = self.state.clone();
        self.drive(async move { fs::cmd_delete_file(message_id, folder_id, &state).await })
            .map_err(Into::into)
    }

    /// Move files between folders (forward to target, delete originals).
    pub fn move_files(
        &self,
        message_ids: Vec<i32>,
        source_folder_id: Option<i64>,
        target_folder_id: Option<i64>,
    ) -> Result<bool, EngineError> {
        let state = self.state.clone();
        self.drive(async move {
            fs::cmd_move_files(message_ids, source_folder_id, target_folder_id, &state).await
        })
        .map_err(Into::into)
    }

    /// Global document search across all chats.
    pub fn search_global(&self, query: String) -> Result<Vec<FileMetadata>, EngineError> {
        let state = self.state.clone();
        self.drive(async move { fs::cmd_search_global(query, &state).await })
            .map_err(Into::into)
    }

    // ------------------------------------------------------------------
    // Previews / thumbnails
    // ------------------------------------------------------------------

    /// Fetch (and cache) a small image thumbnail for a message. Returns a
    /// base64 data URL, or an empty string for non-image content.
    pub fn get_thumbnail(
        &self,
        message_id: i32,
        folder_id: Option<i64>,
    ) -> Result<String, EngineError> {
        let state = self.state.clone();
        self.drive(async move { preview::cmd_get_thumbnail(message_id, folder_id, &state).await })
            .map_err(Into::into)
    }

    /// Fetch a preview for a message (base64 data URL for images, absolute
    /// path for other media that was cached into the app cache dir).
    pub fn get_preview(
        &self,
        message_id: i32,
        folder_id: Option<i64>,
    ) -> Result<String, EngineError> {
        let state = self.state.clone();
        let bw = self.bw()?;
        self.drive(async move {
            preview::cmd_get_preview(message_id, folder_id, &state, &bw).await
        })
        .map_err(Into::into)
    }

    /// Wipe the cached preview directory.
    pub fn clean_cache(&self) -> Result<(), EngineError> {
        let state = self.state.clone();
        self.drive(async move { preview::cmd_clean_cache(&state).await })
            .map_err(Into::into)
    }

    // ------------------------------------------------------------------
    // Bandwidth + streaming info
    // ------------------------------------------------------------------

    /// Return today's upload/download usage counters.
    pub fn get_bandwidth(&self) -> Result<crate::bandwidth::BandwidthStats, EngineError> {
        Ok(self.bw()?.get_stats())
    }

    /// Session token + base URL for building media stream URLs.
    pub fn get_stream_info(&self) -> Result<streaming::StreamInfo, EngineError> {
        let config = StreamConfig {
            token: self.stream_token.clone(),
            port: crate::STREAM_PORT,
        };
        Ok(streaming::get_stream_info(&config))
    }

    /// Start the optional localhost HTTP streaming server. This is only
    /// meaningful on desktop hosts (the legacy webview shell). Mobile builds
    /// compile without the `streaming-server` feature and report that here.
    pub fn start_streaming_server(&self) -> Result<bool, EngineError> {
        #[cfg(feature = "streaming-server")]
        {
            let token = self.stream_token.clone();
            let state = self.state.clone();
            std::thread::spawn(move || {
                let sys = actix_rt::System::new();
                sys.block_on(async move {
                    match crate::server::start_server(state, crate::STREAM_PORT, token).await {
                        Ok(server) => {
                            log::info!("Streaming server started; awaiting shutdown...");
                            server.await.ok();
                        }
                        Err(e) => log::error!("Streaming server failed: {}", e),
                    }
                });
            });
            Ok(true)
        }
        #[cfg(not(feature = "streaming-server"))]
        {
            Err(EngineError::Internal(
                "streaming server is not compiled into this build".to_string(),
            ))
        }
    }
}

impl TelegramDriveEngine {
    /// Run a future to completion on the engine-owned Tokio runtime.
    /// Safe to call concurrently from multiple FFI threads; `block_on` on a
    /// multi-threaded runtime may be invoked from many threads at once.
    fn drive<F>(&self, future: F) -> F::Output
    where
        F: std::future::Future + Send,
    {
        self.runtime.block_on(future)
    }

    /// Lazily construct the bandwidth manager bound to the current data dir.
    fn bw(&self) -> Result<Arc<BandwidthManager>, EngineError> {
        let mut guard = self
            .bw
            .lock()
            .map_err(|_| EngineError::Internal("bandwidth lock poisoned".to_string()))?;
        if guard.is_none() {
            let dir = crate::commands::utils::data_dir(&self.state)
                .map_err(EngineError::Internal)?;
            let _ = std::fs::create_dir_all(&dir);
            *guard = Some(Arc::new(BandwidthManager::new(&dir)));
        }
        Ok(guard.as_ref().unwrap().clone())
    }
}
