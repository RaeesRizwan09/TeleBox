use grammers_client::Client;
use grammers_client::types::Peer;
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use tokio::sync::RwLock;

use crate::commands::TelegramState;

/// Resolve a folder_id to a Telegram Peer, using the cache for O(1) lookups.
///
/// - `folder_id == None` → returns the user's own peer (Saved Messages)
/// - Cache hit → returns immediately without any network call
/// - Cache miss → scans all dialogs, populates the cache, and returns
pub async fn resolve_peer(
    client: &Client,
    folder_id: Option<i64>,
    peer_cache: &Arc<RwLock<HashMap<i64, Peer>>>,
) -> Result<Peer, String> {
    if let Some(fid) = folder_id {
        // Fast path: check cache
        {
            let cache = peer_cache.read().await;
            if let Some(peer) = cache.get(&fid) {
                return Ok(peer.clone());
            }
        }

        // Slow path: scan dialogs and populate cache
        log::debug!("Peer cache miss for folder_id={}, scanning dialogs...", fid);
        let mut found: Option<Peer> = None;
        let mut dialogs = client.iter_dialogs();
        let mut cache = peer_cache.write().await;
        while let Some(dialog) = dialogs.next().await.map_err(|e| e.to_string())? {
            let peer_id = match &dialog.peer {
                Peer::Channel(c) => Some(c.raw.id),
                Peer::User(u) => Some(u.raw.id()),
                _ => None,
            };
            if let Some(id) = peer_id {
                cache.insert(id, dialog.peer.clone());
                if id == fid {
                    found = Some(dialog.peer.clone());
                    // Don't break — keep scanning to warm the cache
                }
            }
        }

        found.ok_or_else(|| format!("Folder/Chat {} not found", fid))
    } else {
        match client.get_me().await {
            Ok(me) => Ok(Peer::User(me)),
            Err(e) => Err(e.to_string()),
        }
    }
}

/// Clear the peer cache (called on logout)
pub async fn clear_peer_cache(peer_cache: &Arc<RwLock<HashMap<i64, Peer>>>) {
    peer_cache.write().await.clear();
}

/// The directory the Telegram session database is persisted to.
/// Falls back to the current working directory when the host never called
/// set_storage_paths (used by the desktop smoke-test binary).
pub fn data_dir(state: &TelegramState) -> Result<PathBuf, String> {
    let guard = state
        .data_dir
        .read()
        .map_err(|e| format!("Failed to lock data dir: {}", e))?;
    Ok(guard.clone().unwrap_or_else(|| {
        std::env::current_dir().unwrap_or_else(|_| PathBuf::from("."))
    }))
}

/// The directory used for transient media previews and thumbnails.
/// Falls back to the data directory when unset.
pub fn cache_dir(state: &TelegramState) -> Result<PathBuf, String> {
    let guard = state
        .cache_dir
        .read()
        .map_err(|e| format!("Failed to lock cache dir: {}", e))?;
    Ok(guard.clone().unwrap_or_else(|| {
        std::env::current_dir().unwrap_or_else(|_| PathBuf::from("."))
    }))
}

pub fn map_error(e: impl std::fmt::Display) -> String {
    let err_str = e.to_string();
    if err_str.contains("FLOOD_WAIT") {
        // Expected format: ... (value: 1234)
        if let Some(start) = err_str.find("(value: ") {
             let rest = &err_str[start + 8..];
             if let Some(end) = rest.find(')') {
                 if let Ok(seconds) = rest[..end].parse::<i64>() {
                     return format!("FLOOD_WAIT_{}", seconds);
                 }
             }
        }
        // Fallback if parsing fails but we know it's a flood wait
        return "FLOOD_WAIT_60".to_string();
    }
    err_str
}

pub const IO_ERR_PREFIX: &str = "IO:";

pub fn io_err(msg: impl Into<String>) -> String {
    format!("{}{}", IO_ERR_PREFIX, msg.into())
}

pub fn is_io_error_message(msg: &str) -> bool {
    msg.starts_with(IO_ERR_PREFIX)
        || msg.contains("os error")
        || msg.contains("No such file")
        || msg.contains("Is a directory")
        || msg.contains("Permission denied")
        || msg.contains("content://")
}

fn hex_val(b: u8) -> Option<u8> {
    match b {
        b'0'..=b'9' => Some(b - b'0'),
        b'a'..=b'f' => Some(b - b'a' + 10),
        b'A'..=b'F' => Some(b - b'A' + 10),
        _ => None,
    }
}

fn percent_decode_path(input: &str) -> String {
    let bytes = input.as_bytes();
    let mut out = Vec::with_capacity(bytes.len());
    let mut i = 0;
    while i < bytes.len() {
        if bytes[i] == b'%' && i + 2 < bytes.len() {
            if let (Some(h), Some(l)) = (hex_val(bytes[i + 1]), hex_val(bytes[i + 2])) {
                out.push((h << 4) | l);
                i += 3;
                continue;
            }
        }
        out.push(bytes[i]);
        i += 1;
    }
    String::from_utf8_lossy(&out).into_owned()
}

fn strip_file_uri(uri: &str) -> String {
    let without_scheme = uri.get(5..).unwrap_or("");
    if let Some(rest) = without_scheme.strip_prefix("//") {
        if let Some(slash) = rest.find('/') {
            let host = &rest[..slash];
            let path = &rest[slash..];
            if host.is_empty() || host.eq_ignore_ascii_case("localhost") {
                return path.to_string();
            }
            return format!("/{}", rest);
        }
        return format!("/{}", rest);
    }
    if without_scheme.starts_with('/') {
        without_scheme.to_string()
    } else {
        format!("/{}", without_scheme)
    }
}

fn decode_document_tree_path(encoded: &str) -> Option<String> {
    let decoded = percent_decode_path(encoded);
    let decoded = decoded.replace(':', "/");
    let decoded = decoded.trim_start_matches('/');
    if decoded.is_empty() {
        return None;
    }
    if decoded.starts_with("primary/") || decoded == "primary" {
        let rest = decoded.trim_start_matches("primary").trim_start_matches('/');
        let mut path = PathBuf::from("/storage/emulated/0");
        if !rest.is_empty() {
            path.push(rest);
        }
        return Some(path.to_string_lossy().into_owned());
    }
    if let Some((volume, rest)) = decoded.split_once('/') {
        if !volume.is_empty() && volume != "primary" {
            let mut path = PathBuf::from("/storage").join(volume);
            if !rest.is_empty() {
                path.push(rest);
            }
            return Some(path.to_string_lossy().into_owned());
        }
    }
    None
}

fn map_android_content_uri(uri: &str) -> Result<PathBuf, String> {
    const TREE_MARKER: &str = "/tree/";
    const DOC_MARKER: &str = "/document/";
    if let Some(idx) = uri.find(TREE_MARKER) {
        let rest = &uri[idx + TREE_MARKER.len()..];
        let encoded = rest.split('/').next().unwrap_or(rest);
        if let Some(path) = decode_document_tree_path(encoded) {
            return Ok(PathBuf::from(path));
        }
    }
    if let Some(idx) = uri.find(DOC_MARKER) {
        let rest = &uri[idx + DOC_MARKER.len()..];
        let encoded = rest.split('/').next().unwrap_or(rest);
        if let Some(path) = decode_document_tree_path(encoded) {
            return Ok(PathBuf::from(path));
        }
    }
    Err(io_err(format!(
        "content:// URIs cannot be opened natively ({}); copy the file into app-private storage first",
        uri
    )))
}

pub fn normalize_local_path(raw: &str) -> Result<PathBuf, String> {
    let trimmed = raw.trim().trim_matches('"').trim_matches('\'');
    if trimmed.is_empty() {
        return Err(io_err("empty file path"));
    }
    let lower = trimmed.to_ascii_lowercase();
    if lower.starts_with("content://") {
        return map_android_content_uri(trimmed);
    }
    let path_part = if lower.starts_with("file:") {
        strip_file_uri(trimmed)
    } else {
        trimmed.to_string()
    };
    Ok(PathBuf::from(percent_decode_path(&path_part)))
}

fn dir_is_writable(dir: &Path) -> bool {
    if std::fs::create_dir_all(dir).is_err() {
        return false;
    }
    let probe = dir.join(".td_write_probe");
    let ok = std::fs::write(&probe, b"ok").is_ok();
    let _ = std::fs::remove_file(&probe);
    ok
}

fn first_writable_dir(candidates: &[PathBuf]) -> Option<PathBuf> {
    candidates.iter().find(|p| dir_is_writable(p)).cloned()
}

pub fn sanitize_filename(name: &str) -> String {
    let trimmed = name.trim();
    let base = Path::new(trimmed)
        .file_name()
        .map(|n| n.to_string_lossy().into_owned())
        .filter(|n| !n.is_empty())
        .unwrap_or_else(|| trimmed.to_string());
    let cleaned: String = base
        .chars()
        .map(|c| match c {
            '/' | '\\' | '\0' => '_',
            c if c.is_control() => '_',
            c => c,
        })
        .collect();
    if cleaned.is_empty() || cleaned == "." || cleaned == ".." {
        "file".to_string()
    } else {
        cleaned
    }
}

pub fn unique_nonconflicting_path(path: PathBuf) -> PathBuf {
    if !path.exists() {
        return path;
    }
    let parent = match path.parent() {
        Some(p) => p.to_path_buf(),
        None => return path,
    };
    let stem = path
        .file_stem()
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_else(|| "file".to_string());
    let ext = path.extension().map(|s| s.to_string_lossy().into_owned());
    for n in 1..10_000 {
        let name = match &ext {
            Some(e) => format!("{} ({}).{}", stem, n, e),
            None => format!("{} ({})", stem, n),
        };
        let candidate = parent.join(name);
        if !candidate.exists() {
            return candidate;
        }
    }
    path
}

pub fn resolve_upload_path(raw: &str) -> Result<(PathBuf, u64), String> {
    let path = normalize_local_path(raw)?;
    let meta = std::fs::metadata(&path).map_err(|e| {
        io_err(format!("cannot open upload path '{}': {}", path.display(), e))
    })?;
    if meta.is_dir() {
        return Err(io_err(format!(
            "upload path is a directory: {}",
            path.display()
        )));
    }
    if !meta.is_file() {
        return Err(io_err(format!(
            "upload path is not a regular file: {}",
            path.display()
        )));
    }
    Ok((path, meta.len()))
}

pub fn resolve_download_path(
    raw: &str,
    file_name: &str,
    fallback_dir: &Path,
) -> Result<PathBuf, String> {
    let trimmed = raw.trim();
    let trailing_sep = trimmed.ends_with('/') || trimmed.ends_with('\\');
    let (base, treat_as_dir) = if trimmed.is_empty() {
        (fallback_dir.to_path_buf(), true)
    } else {
        let path = normalize_local_path(trimmed)?;
        let is_dir = trailing_sep
            || path.is_dir()
            || (!path.exists() && path.extension().is_none());
        (path, is_dir)
    };

    let name = sanitize_filename(file_name);
    let mut target = if treat_as_dir {
        unique_nonconflicting_path(base.join(&name))
    } else {
        base
    };

    if let Some(parent) = target.parent().map(|p| p.to_path_buf()) {
        if !parent.as_os_str().is_empty() && !dir_is_writable(&parent) {
            let fallbacks = [
                fallback_dir.to_path_buf(),
                fallback_dir.join("downloads"),
                std::env::temp_dir(),
            ];
            if let Some(alt) = first_writable_dir(&fallbacks) {
                log::warn!(
                    "Download directory '{}' is not writable; using '{}'",
                    parent.display(),
                    alt.display()
                );
                target = unique_nonconflicting_path(alt.join(&name));
            } else {
                return Err(io_err(format!(
                    "cannot create parent directory '{}'",
                    parent.display()
                )));
            }
        } else if !parent.as_os_str().is_empty() && !parent.exists() {
            std::fs::create_dir_all(&parent).map_err(|e| {
                io_err(format!(
                    "cannot create parent directory '{}': {}",
                    parent.display(),
                    e
                ))
            })?;
        }
    }

    if target.exists() && target.is_dir() {
        target = unique_nonconflicting_path(target.join(name));
    }

    Ok(target)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn strips_file_uri_and_percent_encoding() {
        let p = normalize_local_path("file:///storage/emulated/0/Download/My%20File.pdf").unwrap();
        assert_eq!(p, PathBuf::from("/storage/emulated/0/Download/My File.pdf"));

        let p = normalize_local_path("file://localhost/storage/emulated/0/a.bin").unwrap();
        assert_eq!(p, PathBuf::from("/storage/emulated/0/a.bin"));

        let p = normalize_local_path("  /data/user/0/app/cache/x  ").unwrap();
        assert_eq!(p, PathBuf::from("/data/user/0/app/cache/x"));
    }

    #[test]
    fn maps_android_saf_and_media_uris() {
        let p = normalize_local_path(
            "content://com.android.externalstorage.documents/tree/primary%3ADownload",
        )
        .unwrap();
        assert_eq!(p, PathBuf::from("/storage/emulated/0/Download"));

        let p = normalize_local_path(
            "content://com.android.externalstorage.documents/document/primary%3ADownload%2Fphoto.jpg",
        )
        .unwrap();
        assert_eq!(p, PathBuf::from("/storage/emulated/0/Download/photo.jpg"));

        let err = normalize_local_path("content://media/external/file/12").unwrap_err();
        assert!(err.contains("content://"));
        assert!(is_io_error_message(&err));

        let err = normalize_local_path("content://com.example.provider/unknown").unwrap_err();
        assert!(err.contains("content://"));
        assert!(is_io_error_message(&err));
    }

    #[test]
    fn sanitizes_unsafe_names() {
        assert_eq!(sanitize_filename("a/b\\c.txt"), "c.txt");
        assert_eq!(sanitize_filename(".."), "file");
        assert_eq!(sanitize_filename(""), "file");
    }

    #[test]
    fn download_joins_directory_and_creates_parents() {
        let root = std::env::temp_dir().join(format!(
            "td_dl_{}_{}",
            std::process::id(),
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_nanos()
        ));
        std::fs::create_dir_all(&root).unwrap();
        let dest = resolve_download_path(root.to_str().unwrap(), "photo.jpg", &root).unwrap();
        assert_eq!(dest, root.join("photo.jpg"));

        std::fs::write(&dest, b"x").unwrap();
        let dest2 = resolve_download_path(root.to_str().unwrap(), "photo.jpg", &root).unwrap();
        assert_eq!(dest2, root.join("photo (1).jpg"));

        let nested = root.join("missing").join("out.bin");
        let dest3 = resolve_download_path(nested.to_str().unwrap(), "ignored.bin", &root).unwrap();
        assert_eq!(dest3, nested);
        assert!(dest3.parent().unwrap().is_dir());
    }

    #[test]
    fn upload_rejects_directory() {
        let root = std::env::temp_dir().join(format!(
            "td_ul_{}_{}",
            std::process::id(),
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_nanos()
        ));
        std::fs::create_dir_all(&root).unwrap();
        let err = resolve_upload_path(root.to_str().unwrap()).unwrap_err();
        assert!(err.contains("directory"));
    }
}
