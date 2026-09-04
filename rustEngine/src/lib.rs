pub mod models;
pub mod bandwidth;
pub mod commands;
pub mod android;

// Local HTTP streaming server used by the legacy desktop shell. Only compiled
// when the `streaming-server` feature is enabled (mobile pipeline builds the
// library with --no-default-features so this stays out of the .so).
#[cfg(feature = "streaming-server")]
pub mod server;

// Kept as a crate-root re-export so the legacy command modules can continue to
// refer to `crate::TelegramState` (see commands/mod.rs for the definition).
pub use commands::TelegramState;

/// Single source of truth for the streaming server port.
/// Referenced by android.rs and exposed to the frontend via get_stream_info so
/// no component ever hardcodes the port.
pub const STREAM_PORT: u16 = 14201;

/// UniFFI scaffolding for the `telegram_drive_engine` namespace.
/// Exported types live in models.rs, bandwidth.rs, commands/streaming.rs and
/// the TelegramDriveEngine object in android.rs.
uniffi::setup_scaffolding!("telegram_drive_engine");
