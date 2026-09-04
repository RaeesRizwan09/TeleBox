use std::sync::Arc;
use std::collections::{HashMap, HashSet};
use std::path::PathBuf;
use tokio::sync::Mutex;
use grammers_client::{Client};
use grammers_client::types::{LoginToken, PasswordToken, Peer};

/// Native progress events are forwarded to the Android shell through this
/// callback interface (implemented in Kotlin). Upload/download transfers emit
/// ticks at ~250ms intervals, mirroring the events the legacy Tauri webview
/// consumed over the `upload-progress` / `download-progress` channels.
#[uniffi::export(callback_interface)]
pub trait TransferProgressListener: Send + Sync {
    fn on_upload_progress(
        &self,
        id: String,
        percent: u8,
        transferred_bytes: u64,
        total_bytes: u64,
        speed_bytes_per_sec: u64,
    );
    fn on_download_progress(
        &self,
        id: String,
        percent: u8,
        transferred_bytes: u64,
        total_bytes: u64,
        speed_bytes_per_sec: u64,
    );
}

/// Tracks the lifecycle of the Telegram connection.
///
/// IMPORTANT: The `runner_shutdown` field is critical for preventing stack overflow.
/// When reconnecting, we MUST shutdown the old runner before spawning a new one.
/// Without this, runner tasks accumulate and exhaust the thread stack.
#[derive(Clone)]
pub struct TelegramState {
    pub client: Arc<Mutex<Option<Client>>>,
    pub login_token: Arc<Mutex<Option<LoginToken>>>,
    pub password_token: Arc<Mutex<Option<PasswordToken>>>,
    pub api_id: Arc<Mutex<Option<i32>>>,
    /// Send to this channel to request runner shutdown.
    /// Uses std::sync::Mutex (not tokio) so it can be locked from synchronous
    /// contexts like the RunEvent::Exit handler.
    pub runner_shutdown: Arc<std::sync::Mutex<Option<tokio::sync::oneshot::Sender<()>>>>,
    /// Counter for debugging runner lifecycle
    pub runner_count: Arc<std::sync::atomic::AtomicU32>,
    /// Cache of folder_id → Peer to avoid O(N) dialog scanning on every operation.
    /// Populated lazily on first resolve_peer call, eagerly during cmd_scan_folders.
    /// Cleared on logout.
    pub peer_cache: Arc<tokio::sync::RwLock<HashMap<i64, Peer>>>,
    /// Set of transfer IDs that have been cancelled. Checked cooperatively
    /// in upload/download chunk loops. Cleared on logout.
    pub cancelled_transfers: Arc<tokio::sync::RwLock<HashSet<String>>>,
    /// Host-private storage directory (Android: context.filesDir).
    /// Populated by the engine via set_storage_paths before connect.
    pub data_dir: Arc<std::sync::RwLock<Option<PathBuf>>>,
    /// Host-private cache directory (Android: context.cacheDir).
    /// Populated by the engine via set_storage_paths before connect.
    pub cache_dir: Arc<std::sync::RwLock<Option<PathBuf>>>,
    /// Currently registered transfer progress callback, if any.
    pub progress_listener:
        Arc<tokio::sync::RwLock<Option<Arc<dyn TransferProgressListener>>>>,
}

pub mod auth;
pub mod fs;
pub mod preview;
pub mod utils;
pub mod network;
pub mod streaming;

pub use auth::*;
pub use fs::*;
pub use preview::*;
pub use utils::*;
pub use network::*;
pub use streaming::*;
