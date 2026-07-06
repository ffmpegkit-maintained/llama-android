package dev.ffmpegkit.llama

/**
 * A loaded GGUF model + its inference context. Obtain one from [Llama.loadModel] and
 * free it with [Llama.releaseModel]. Not thread-safe: serialise calls, or use the Pro
 * session API for concurrent chats.
 */
class LlamaModel internal constructor(
    internal var handle: Long,
    val config: LlamaConfig,
) {
    /** True until [Llama.releaseModel] is called. */
    val isLoaded: Boolean get() = handle != 0L

    internal fun requireHandle(): Long {
        if (handle == 0L) throw LlamaException.ModelReleased()
        return handle
    }
}
