use serde::{Deserialize, Serialize};

/// Current Telegram authorization state machine.
/// Internal only — the Android shell consumes the lighter `AuthResult`.
#[derive(Debug, Serialize, Deserialize, Clone)]
#[serde(tag = "status", content = "data")]
pub enum AuthState {
    LoggedOut,
    AwaitingCode { phone: String, phone_code_hash: String },
    AwaitingPassword { phone: String },
    LoggedIn,
}

/// Result of any authentication step. `next_step` is one of:
/// "code", "password", "dashboard" or "waiting" (QR flow).
#[derive(Debug, Serialize, Deserialize, Clone, uniffi::Record)]
pub struct AuthResult {
    pub success: bool,
    pub next_step: Option<String>, // "code", "password", "dashboard"
    pub error: Option<String>,
}

/// A file stored inside a Telegram "drive" channel.
/// `id` is the Telegram message id, `folder_id` is the channel id (None = Saved Messages).
#[derive(Debug, Serialize, Deserialize, Clone, uniffi::Record)]
pub struct FileMetadata {
    pub id: i64,
    pub folder_id: Option<i64>,
    pub name: String,
    pub size: u64, // Updated to u64
    pub mime_type: Option<String>,
    pub file_ext: Option<String>, // Added field
    pub created_at: String,
    pub icon_type: String,
}

/// A Telegram channel that acts as a storage "folder".
#[derive(Debug, Serialize, Deserialize, Clone, uniffi::Record)]
pub struct FolderMetadata {
    pub id: i64,
    pub parent_id: Option<i64>,
    pub name: String,
}

/// One mounted drive (a chat/dialog that behaves as storage).
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Drive {
    pub chat_id: i64,
    pub name: String,
    pub icon: Option<String>,
}
