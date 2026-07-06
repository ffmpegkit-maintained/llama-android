# llama.cpp for Android — On-Device LLM Inference AAR

[![Maven Central](https://img.shields.io/maven-central/v/dev.ffmpegkit-maintained/llama-android)](https://central.sonatype.com/artifact/dev.ffmpegkit-maintained/llama-android)
[![JitPack](https://jitpack.io/v/ffmpegkit-maintained/llama-android.svg)](https://jitpack.io/#ffmpegkit-maintained/llama-android)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Website](https://img.shields.io/badge/website-jokobee.com-blue.svg)](https://www.jokobee.com)

**Run LLMs on Android with one Gradle line.** No cloud, no API key, no per-token
billing. Your users' conversations never leave the phone.

A prebuilt [llama.cpp](https://github.com/ggml-org/llama.cpp) AAR with a clean Kotlin
coroutine API — chat completion, embeddings, GGUF models. No NDK, no CMake, no
Python. Wraps llama.cpp build `b9878`.

## Install

**A) Maven Central** (recommended)

```kotlin
dependencies {
    implementation("dev.ffmpegkit-maintained:llama-android:0.1.1")
}
```

**B) JitPack**

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories { google(); mavenCentral(); maven(url = uri("https://jitpack.io")) }
}
// build.gradle.kts
dependencies {
    implementation("com.github.ffmpegkit-maintained:llama-android:v0.1.1")
}
```

## Quick start

```kotlin
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig

lifecycleScope.launch {
    // 1. Load a GGUF model you shipped or downloaded (see "Models" below).
    val model = Llama.loadModel(
        modelPath = File(getExternalFilesDir("models"), "model.gguf").absolutePath,
        config = LlamaConfig(contextSize = 2048, threads = 4),
    )

    // 2. Chat completion.
    val result = Llama.complete(
        model,
        prompt = "Explain gravity to a 5-year-old.",
        systemPrompt = "You are a friendly teacher.",
        maxTokens = 256,
    )
    println(result.text)
    println("${result.tokensPerSecond} tok/s")

    // 3. Embeddings (text → vector) for search / RAG.
    val vector = Llama.embed(model, "on-device AI")

    // 4. Free native memory.
    Llama.releaseModel(model)
}
```

All heavy calls are `suspend` functions — call them from a coroutine.

## Models (not bundled — you download them)

LLM weights are **400 MB–8 GB**, far too large to bundle in an AAR. Download a
**GGUF** model once (ship it in your app or fetch on first run):

| Model | Size (Q4_K_M) | Notes | Download |
|---|---|---|---|
| Qwen2.5 0.5B | ~400 MB | ultra-fast, basic | [HF](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF) |
| Gemma 2 2B | ~1.6 GB | compact & capable | [HF](https://huggingface.co/bartowski/gemma-2-2b-it-GGUF) |
| Llama 3.2 3B | ~2.0 GB | best for chat | [HF](https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF) |
| Phi-3.5 Mini 3.8B | ~2.2 GB | strong reasoning | [HF](https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF) |

Rule of thumb: a Q4_K_M model needs roughly *(model size + ~20%)* of free RAM. On a
6 GB phone, stick to ≤ 3B models. See the [wiki](../../wiki) for a full guide.

## What's inside

| | |
|---|---|
| Engine | llama.cpp (`b9878`) + ggml, CPU/**NEON** |
| API | Chat completion, embeddings, system prompt, chat templates (auto) |
| Models | any GGUF (Q4_0, Q4_K_M, Q5_K_M, …) |
| ABI | `arm64-v8a` |
| Min SDK | API 24 (Android 7.0) · 16 KB pages (Android 15 ready) |

## Free vs Pro

| | **Free** (this) | **Pro** |
|---|:---:|:---:|
| Chat completion, embeddings | ✅ | ✅ |
| Streaming tokens (Flow) | ✗ | ✅ |
| Multiple concurrent sessions (per-session KV cache) | ✗ | ✅ |
| Vulkan GPU acceleration | ✗ (CPU/NEON) | ✅ |
| ABI | arm64-v8a | arm64-v8a + x86_64 |
| Channel | Maven Central + JitPack + Release | Gumroad |

**→ [Get llama.cpp Pro](https://www.jokobee.com)** — Vulkan GPU, streaming, multi-session.

## Works with Whisper — voice AI, fully on-device

Chain the Jokobee on-device AI stack for a private voice assistant:

**[FFmpegKit](https://github.com/ffmpegkit-maintained/ffmpeg)** (decode audio) →
**[Whisper](https://github.com/ffmpegkit-maintained/whisper)** (speech → text) →
**llama.cpp** (text → answer) → Android TTS (answer → speech). No data leaves the device.

## Building from source

```bash
git clone --recursive https://github.com/ffmpegkit-maintained/llama-android.git
cd llama-android
./gradlew :library:assembleRelease
```

Requires NDK r27c (`27.2.12479018`) and CMake 3.22.1. llama.cpp is a pinned git
submodule (`b9878`).

## License

MIT — see [LICENSE](LICENSE). llama.cpp is MIT (© the ggml authors).

---

Maintained by **[Jokobee](https://www.jokobee.com)** · contact@jokobee.com
