**English** | [Українська](README.uk.md)

# STT-manager

## About

An application with a JNI wrapper around whisper.cpp for speech recognition, supporting acceleration via NVIDIA CUDA or Vulkan. Built with JavaFX (Liberica JDK).

## Assets NOT stored in git

Large binary files (models, native libraries) are not stored in the repository itself — they're distributed via **GitHub Releases**. Reason: git handles large binary files poorly — the repository balloons to gigabytes in size and becomes unwieldy to clone.

Instead, after cloning the repository, you run a script once to download everything needed.

### What gets downloaded

| What | From | Extracted to |
|---|---|---|
| Whisper models (`.bin`) | Release tagged `models` | `Dyplom/models/whisper/` |
| CUDA native libraries | `native-cuda.zip` from Release `native` | `Dyplom/native/cuda/` |
| Vulkan native libraries | `native-vulkan.zip` from Release `native` | `Dyplom/native/vulkan/` |

Link to all releases: https://github.com/q1wekl/SFX-RTX-TripleX/releases

### Models (`models`)

GGML-format files for whisper.cpp, in various sizes (larger file = more accurate but slower and heavier):

- `ggml-tiny.bin` — smallest, fastest
- `ggml-base.bin`
- `ggml-small.bin`
- `ggml-medium.bin`
- `ggml-large.bin`
- `ggml-large-v3.bin` — largest, most accurate

### Native libraries (`native`)

There are two archives here, because the CUDA build and the Vulkan build of the native libraries are **incompatible with each other** (they're different whisper.cpp builds targeting different hardware), so they can't share a single flat file set — hence splitting them into separate archives:

- `native-cuda.zip` — for running with NVIDIA GPU acceleration
- `native-vulkan.zip` — for running via Vulkan (more universal, works across different GPUs)

Both archives contain:
- `ggml.dll`, `whisper.dll`, `whisper-jni.dll` — the whisper.cpp libraries themselves (different builds for CUDA/Vulkan)
- `msvcp140.dll`, `vcruntime140.dll` — standard Visual C++ runtime libraries (identical in both archives)

Plus:
- `native-cuda.zip` additionally contains `cublasLt64_12.dll`, `cublas64_12.dll` (CUDA-specific)
- `native-vulkan.zip` additionally contains `glslangd.dll` (Vulkan-specific)

## Setting up the project after cloning

### 1. Install Liberica JDK (with JavaFX)

The project uses Liberica JDK Full (JavaFX is bundled in, no need to install it separately).

Download: https://bell-sw.com/pages/downloads/

After installation, set `JAVA_HOME` to point to your Liberica installation.

> Liberica JDK is **not stored in git** — it's a build tool, and each developer installs it locally, just like an IDE or a compiler.

### 2. Download the models and native libraries

Run `scripts/download-assets.sh` (detailed explanation below) from the repository root:

```bash
bash scripts/download-assets.sh
```

By default, the script downloads the CUDA variant of the native libraries. If you don't have an NVIDIA GPU, or you want the Vulkan variant, set the `VARIANT` variable before running:

```bash
VARIANT=vulkan bash scripts/download-assets.sh
```

### 3. Build the project

(add your build command here as needed — Gradle/Maven/CMake, depending on what's used)

## What `scripts/download-assets.sh` does (line by line)

The script is a bash file that runs a sequence of `curl` (download a file from a URL) and `unzip` (extract an archive) commands. The logic:

1. **Creates the target folders** (`mkdir -p`) — if a folder already exists, nothing breaks.
2. **Loops over the list of models** with a `for` loop and downloads each individual `.bin` file from the `models` Release, using a direct URL like:
   ```
   https://github.com/q1wekl/SFX-RTX-TripleX/releases/download/models/ggml-tiny.bin
   ```
3. **Downloads the native library archive** (`native-cuda.zip` or `native-vulkan.zip`, depending on `VARIANT`), extracts it (`unzip`) into the matching folder, and deletes the archive afterward (`rm`) so it doesn't take up disk space.

The script can be run again any number of times — it just overwrites files with fresh copies, nothing gets broken.

## Note on git history

If you clone the repository fresh, everything works transparently — there are no heavy files in it at all, and cloning is fast. The old version of the repository (before cleanup) had these files directly in the git history, which pushed its size to ~8 GB — this was fixed using `git filter-repo`.
