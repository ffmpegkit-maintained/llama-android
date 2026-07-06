package dev.ffmpegkit.llama

/**
 * Model + inference configuration.
 *
 * @property contextSize context window in tokens.
 * @property threads CPU threads for inference.
 * @property gpuLayers layers to offload to the GPU. **0 = CPU only** (Free tier).
 *   Vulkan GPU offload (`gpuLayers > 0`) requires the **Pro** build.
 * @property temperature sampling temperature; `<= 0` = greedy.
 * @property topP nucleus sampling.
 * @property topK top-k sampling.
 * @property seed RNG seed; `-1` = random.
 */
data class LlamaConfig(
    val contextSize: Int = 2048,
    val threads: Int = 4,
    val gpuLayers: Int = 0,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val seed: Int = -1,
)
