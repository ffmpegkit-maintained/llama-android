package dev.ffmpegkit.llama

/** Base type for all llama errors. */
sealed class LlamaException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /** The GGUF model file could not be loaded (missing, corrupt, or out of memory). */
    class ModelLoadFailed(path: String) :
        LlamaException(
            "Failed to load model '$path'. Check the path, that it's a valid GGUF file, " +
                "and that the device has enough RAM (try a smaller / more-quantized model).",
        )

    /** A call was made with a model that was already released. */
    class ModelReleased :
        LlamaException("This LlamaModel has been released. Load the model again.")

    /** Inference failed at the native layer. */
    class InferenceFailed(message: String) : LlamaException(message)
}
