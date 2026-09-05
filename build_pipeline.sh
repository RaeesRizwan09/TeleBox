#!/usr/bin/env bash
#
# build_pipeline.sh — TelegramDrive native build & deploy pipeline.
#
# Bridges the Rust engine (rustEngine/) and the native Android front end
# (androidApp/). Executed from the repository root (or anywhere), it:
#
#   1. Installs the Android Rust targets used by the pipeline.
#   2. Cross-compiles rustEngine as a UniFFI `cdylib` for the Android ABIs
#      (arm64-v8a = physical devices, x86_64 = emulators) via `cargo ndk`,
#      excluding the legacy desktop streaming server
#      (`--no-default-features`).
#   3. Runs `uniffi-bindgen` against the produced library so the native
#      `TelegramDriveEngine` bridge is emitted as a modern Kotlin wrapper
#      (package com.telebox.app.engine).
#   4. Relocates the .so artifacts into androidApp/app/src/main/jniLibs/<abi>/
#      and the generated .kt wrapper into
#      androidApp/app/src/main/java/com/telebox/app/engine/.
#
# Prerequisites (installed by the caller once):
#   - Rust stable toolchain (rustup)
#   - Android NDK (ANDROID_NDK_HOME or --ndk-path)
#   - cargo-ndk        -> cargo install cargo-ndk
#   - uniffi-bindgen   -> cargo install uniffi_bindgen --version 0.28.*
#
set -euo pipefail

# ---------------------------------------------------------------------------
# Paths / configuration
# ---------------------------------------------------------------------------
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUST_DIR="${ROOT_DIR}/rustEngine"
JNI_LIBS_DIR="${ROOT_DIR}/androidApp/app/src/main/jniLibs"
KOTLIN_ENGINE_DIR="${ROOT_DIR}/androidApp/app/src/main/java/com/telebox/app/engine"
KOTLIN_PACKAGE="com.telebox.app.engine"

# Target ABIs + their cargo-ndk identifiers (same names as jniLibs dirs).
ABIS=(arm64-v8a x86_64)
RUST_TARGETS=(aarch64-linux-android x86_64-linux-android)

# The Rust [lib] name (Cargo.toml) — this is what cargo ndk emits as libapp_lib.so.
RUST_LIB_NAME="app_lib"
LIB_ARTIFACT="lib${RUST_LIB_NAME}.so"

# UniFFI namespace registered by uniffi::setup_scaffolding! in rustEngine/src/lib.rs.
UNIFFI_NAMESPACE="telegram_drive_engine"
UNIFFI_VERSION_REQUIRED="0.28"

echo "==> TelegramDrive build pipeline"
echo "    root    : ${ROOT_DIR}"
echo "    ABIs    : ${ABIS[*]}"
echo "    package : ${KOTLIN_PACKAGE}"

# ---------------------------------------------------------------------------
# Tool checks
# ---------------------------------------------------------------------------
command -v cargo >/dev/null 2>&1 || { echo "ERROR: cargo not found (install rustup first)." >&2; exit 1; }
command -v cargo-ndk >/dev/null 2>&1 || { echo "ERROR: cargo-ndk not found. Run: cargo install cargo-ndk" >&2; exit 1; }
command -v uniffi-bindgen >/dev/null 2>&1 || { echo "ERROR: uniffi-bindgen not found. Run: cargo install uniffi_bindgen --version ${UNIFFI_VERSION_REQUIRED}.*" >&2; exit 1; }

# Verify the installed uniffi-bindgen matches the Rust crate dependency.
UNIFFI_INSTALLED_VERSION="$(uniffi-bindgen --version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -n1)"
case "${UNIFFI_INSTALLED_VERSION}" in
    ${UNIFFI_VERSION_REQUIRED}.*) ;;
    "")
        echo "ERROR: could not read uniffi-bindgen version." >&2
        exit 1
        ;;
    *)
        echo "ERROR: uniffi-bindgen ${UNIFFI_INSTALLED_VERSION} != required ${UNIFFI_VERSION_REQUIRED}.*" >&2
        exit 1
        ;;
esac

# ---------------------------------------------------------------------------
# Step 1 — Android Rust targets
# ---------------------------------------------------------------------------
echo "==> [1/4] Installing Android Rust targets"
for target in "${RUST_TARGETS[@]}"; do
    rustup target add "${target}"
done

# ---------------------------------------------------------------------------
# Step 2 — Cross-compile the UniFFI cdylib for every ABI
# ---------------------------------------------------------------------------
echo "==> [2/4] Cross-compiling rustEngine (cargo ndk, release)"
NDK_ARGS=()
if [[ -n "${ANDROID_NDK_HOME:-}" ]]; then
    NDK_ARGS+=(--ndk-path "${ANDROID_NDK_HOME}")
fi

# cargo ndk places libapp_lib.so straight into jniLibs/<abi>/, but clear any
# stale artifacts first so removed ABIs never linger in the APK.
rm -f "${JNI_LIBS_DIR}"/{arm64-v8a,x86_64}/${LIB_ARTIFACT}

cargo ndk "${NDK_ARGS[@]}" \
    --platform 21 \
    --target "${ABIS[0]}" --target "${ABIS[1]}" \
    --output-dir "${JNI_LIBS_DIR}" \
    -- build --release --lib --no-default-features --manifest-path "${RUST_DIR}/Cargo.toml"

echo "==> [2/4] Artifacts produced:"
for abi in "${ABIS[@]}"; do
    ls -lh "${JNI_LIBS_DIR}/${abi}/${LIB_ARTIFACT}"
done

# ---------------------------------------------------------------------------
# Step 3 — Emit the native Kotlin wrapper (UniFFI bindgen)
# ---------------------------------------------------------------------------
echo "==> [3/4] Generating Kotlin bindings (uniffi-bindgen)"

# The scaffolding metadata is identical across ABIs, so reading the arm64-v8a
# artifact is enough to emit the language bindings.
BINDINGS_TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${BINDINGS_TMP_DIR}"' EXIT

uniffi-bindgen generate \
    --language kotlin \
    --library "${JNI_LIBS_DIR}/${ABIS[0]}/${LIB_ARTIFACT}" \
    --out-dir "${BINDINGS_TMP_DIR}" \
    --no-format

# Relocate the generated wrapper into the Android engine package directory.
mkdir -p "${KOTLIN_ENGINE_DIR}"
echo "==> [3/4] Installing Kotlin wrapper into ${KOTLIN_ENGINE_DIR}"
rm -f "${KOTLIN_ENGINE_DIR}"/*.kt
find "${BINDINGS_TMP_DIR}" -name '*.kt' -exec cp {} "${KOTLIN_ENGINE_DIR}"/ \;

ls -lh "${KOTLIN_ENGINE_DIR}"

# ---------------------------------------------------------------------------
# Step 4 — Verify the deployment
# ---------------------------------------------------------------------------
echo "==> [4/4] Verifying deployment"
fail=0
for abi in "${ABIS[@]}"; do
    if [[ ! -s "${JNI_LIBS_DIR}/${abi}/${LIB_ARTIFACT}" ]]; then
        echo "    MISSING  jniLibs/${abi}/${LIB_ARTIFACT}" >&2
        fail=1
    else
        echo "    OK       jniLibs/${abi}/${LIB_ARTIFACT}"
    fi
done
for f in "${KOTLIN_ENGINE_DIR}"/${UNIFFI_NAMESPACE}.kt; do
    if [[ ! -s "${f}" ]]; then
        echo "    MISSING  ${f}" >&2
        fail=1
    else
        echo "    OK       ${f}"
    fi
done

if [[ "${fail}" -eq 0 ]]; then
    echo "==> Pipeline complete. Build the Android app with:"
    echo "    cd androidApp && ./gradlew assembleDebug"
else
    echo "==> Pipeline finished with errors." >&2
    exit 1
fi
