/// Holds the per-session streaming config (token + port).
/// The legacy desktop shell started an Actix server on a dedicated thread and
/// stored its token here. The token is generated once per engine instance.
#[derive(Clone)]
pub struct StreamConfig {
    pub token: String,
    pub port: u16,
}

/// Returned to the frontend so it can construct stream URLs dynamically
#[derive(Clone, serde::Serialize, uniffi::Record)]
pub struct StreamInfo {
    pub token: String,
    pub base_url: String,
}

/// Returns the streaming server's session token and base URL to the frontend.
/// The frontend must use the returned base_url to construct stream URLs,
/// never hardcoding the port.
pub fn get_stream_info(config: &StreamConfig) -> StreamInfo {
    StreamInfo {
        token: config.token.clone(),
        base_url: format!("http://localhost:{}", config.port),
    }
}
