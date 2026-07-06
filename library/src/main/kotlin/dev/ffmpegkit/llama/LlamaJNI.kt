package dev.ffmpegkit.llama

/**
 * Internal bridge to the native libraries. Not part of the public API — use [Llama].
 * Loads ggml (+ split backends) → llama → llama_jni. Missing split libs are ignored
 * (some llama.cpp builds ship a single libggml.so, others split it).
 */
internal object LlamaJNI {

    init {
        for (lib in listOf("ggml-base", "ggml-cpu", "ggml", "llama")) {
            runCatching { System.loadLibrary(lib) }
        }
        System.loadLibrary("llama_jni")
    }

    /** @return native handle, or 0 on failure. */
    external fun nativeLoadModel(path: String, contextSize: Int, threads: Int, gpuLayers: Int): Long

    /** @return JSON: `{"text":..,"tokensGenerated":..,"tokensPerSecond":..,"promptEvalTimeMs":..,"generateTimeMs":..}`. */
    external fun nativeComplete(
        handle: Long, prompt: String, systemPrompt: String, maxTokens: Int,
        temp: Float, topP: Float, topK: Int, seed: Int,
    ): String

    external fun nativeEmbed(handle: Long, text: String): FloatArray

    external fun nativeReleaseModel(handle: Long)

    external fun nativeGetSystemInfo(): String
}
