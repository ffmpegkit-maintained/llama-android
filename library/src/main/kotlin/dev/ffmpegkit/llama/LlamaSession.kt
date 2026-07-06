package dev.ffmpegkit.llama

/**
 * A single multi-turn chat session over a loaded [LlamaModel].
 *
 * Free tier: **one session at a time**. Conversation history is replayed into the
 * prompt each turn. For concurrent sessions with a persistent per-session KV cache
 * (much faster multi-turn), see `LlamaSessionManager` in the **Pro** build.
 *
 * ```
 * val session = LlamaSession(model, systemPrompt = "You are a helpful assistant.")
 * val reply = session.send("Hello!")
 * println(reply.text)
 * ```
 */
class LlamaSession(
    private val model: LlamaModel,
    val systemPrompt: String = "",
) {
    private val turns = ArrayList<Pair<String, String>>()  // (user, assistant)

    /** The conversation so far, as (user, assistant) pairs. */
    val history: List<Pair<String, String>> get() = turns.toList()

    /** Send a user message and get the assistant's reply, keeping conversation context. */
    suspend fun send(userMessage: String, maxTokens: Int = 512): LlamaResult {
        val prompt = buildString {
            for ((u, a) in turns) {
                append("User: ").append(u).append('\n')
                append("Assistant: ").append(a).append('\n')
            }
            append(userMessage)
        }
        val result = Llama.complete(model, prompt, systemPrompt, maxTokens)
        turns += userMessage to result.text
        return result
    }

    /** Clear the conversation history (fresh session, same model). */
    fun reset() = turns.clear()
}
