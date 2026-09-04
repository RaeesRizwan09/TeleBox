// Host-side smoke-test binary.
//
// The primary artifact of this crate is the `cdylib` produced for the mobile
// build pipeline (see build_pipeline.sh) — UniFFI generates the Kotlin bindings
// from it. This binary exists so developers on a desktop host can quickly
// validate that the engine constructs and that a Tokio runtime spins up.
//
// It is intentionally excluded from Android builds (`cargo ndk ... build --lib`).

use app_lib::android::TelegramDriveEngine;

fn main() {
    // Fix EGL_BAD_ALLOC on Linux distros (especially Arch) where the AppImage's
    // bundled Mesa conflicts with the host's GPU driver stack.
    // This must be set BEFORE any native windowing initializes.
    #[cfg(target_os = "linux")]
    {
        if std::env::var("WEBKIT_DISABLE_DMABUF_RENDERER").is_err() {
            std::env::set_var("WEBKIT_DISABLE_DMABUF_RENDERER", "1");
        }
    }

    env_logger::init();

    let engine = TelegramDriveEngine::new();
    let dirs = std::env::temp_dir().join("telegramdrive-smoke");
    engine.set_storage_paths(
        dirs.join("data").to_string_lossy().to_string(),
        dirs.join("cache").to_string_lossy().to_string(),
    );

    match engine.is_network_available() {
        Ok(true) => println!("TelegramDriveEngine: online"),
        Ok(false) => println!("TelegramDriveEngine: offline (engine healthy)"),
        Err(e) => println!("TelegramDriveEngine: network probe failed: {}", e),
    }
    println!("TelegramDriveEngine smoke test complete.");
}
