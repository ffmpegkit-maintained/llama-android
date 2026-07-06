package dev.ffmpegkit.llama

/**
 * The result of a [Llama.complete] call.
 *
 * @property text the generated text.
 * @property tokensGenerated number of tokens produced.
 * @property tokensPerSecond generation throughput.
 * @property promptEvalTimeMs time spent ingesting the prompt.
 * @property generateTimeMs time spent generating the response.
 */
data class LlamaResult(
    val text: String,
    val tokensGenerated: Int,
    val tokensPerSecond: Float,
    val promptEvalTimeMs: Long,
    val generateTimeMs: Long,
)
