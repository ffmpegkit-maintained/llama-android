package dev.ffmpegkit.llama

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * On-device LLM inference with llama.cpp — chat completion, embeddings, GGUF models.
 * No cloud, no API key, no Python.
 *
 * ```
 * val model = Llama.loadModel("/path/to/model.gguf")
 * val result = Llama.complete(model, "Explain gravity in one sentence.")
 * println(result.text)
 * Llama.releaseModel(model)
 * ```
 *
 * A single [LlamaModel] is **not** thread-safe. All heavy calls are `suspend`
 * functions running on [Dispatchers.Default].
 */
object Llama {

    /** Load a GGUF model from disk. @throws LlamaException.ModelLoadFailed on failure. */
    suspend fun loadModel(
        modelPath: String,
        config: LlamaConfig = LlamaConfig(),
    ): LlamaModel = withContext(Dispatchers.Default) {
        if (!File(modelPath).exists()) throw LlamaException.ModelLoadFailed(modelPath)
        val handle = LlamaJNI.nativeLoadModel(
            modelPath, config.contextSize, config.threads, config.gpuLayers,
        )
        if (handle == 0L) throw LlamaException.ModelLoadFailed(modelPath)
        LlamaModel(handle, config)
    }

    /** Chat completion: `prompt` → full response, formatted with the model's chat template. */
    suspend fun complete(
        model: LlamaModel,
        prompt: String,
        systemPrompt: String = "",
        maxTokens: Int = 512,
    ): LlamaResult = withContext(Dispatchers.Default) {
        val c = model.config
        val json = LlamaJNI.nativeComplete(
            model.requireHandle(), prompt, systemPrompt, maxTokens,
            c.temperature, c.topP, c.topK, c.seed,
        )
        parseResult(json)
    }

    /** Embed `text` into a vector (for search / RAG / clustering). */
    suspend fun embed(model: LlamaModel, text: String): FloatArray =
        withContext(Dispatchers.Default) {
            LlamaJNI.nativeEmbed(model.requireHandle(), text)
        }

    /** Free the model's native memory. Safe to call more than once. */
    fun releaseModel(model: LlamaModel) {
        val h = model.handle
        if (h != 0L) {
            LlamaJNI.nativeReleaseModel(h)
            model.handle = 0L
        }
    }

    /** llama.cpp system info (CPU features / NEON / backends / threads). */
    fun getSystemInfo(): String = LlamaJNI.nativeGetSystemInfo()

    private fun parseResult(json: String): LlamaResult {
        val o = JSONObject(json)
        if (o.has("error")) throw LlamaException.InferenceFailed(o.optString("error"))
        return LlamaResult(
            text = o.optString("text"),
            tokensGenerated = o.optInt("tokensGenerated"),
            tokensPerSecond = o.optDouble("tokensPerSecond", 0.0).toFloat(),
            promptEvalTimeMs = o.optLong("promptEvalTimeMs"),
            generateTimeMs = o.optLong("generateTimeMs"),
        )
    }
}
