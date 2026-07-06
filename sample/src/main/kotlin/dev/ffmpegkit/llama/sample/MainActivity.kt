package dev.ffmpegkit.llama.sample

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import kotlinx.coroutines.launch
import java.io.File

/**
 * Minimal demo: show llama.cpp system info, then load a GGUF model and run one chat
 * completion. Push a model first, e.g.:
 *
 *   adb push model.gguf /sdcard/Android/data/dev.ffmpegkit.llama.sample/files/models/model.gguf
 *
 * The on-device smoke test logs under tag `LlamaSelfTest`.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        output = findViewById(R.id.output)

        output.text = "llama.cpp system info:\n\n${Llama.getSystemInfo()}"
        Log.i("LlamaSelfTest", "sysinfo=${Llama.getSystemInfo()}")

        findViewById<Button>(R.id.runButton).setOnClickListener { runChat() }

        // Auto-run the smoke test on launch (read via `adb logcat -s LlamaSelfTest`).
        runChat()
    }

    private fun runChat() {
        val modelFile = File(getExternalFilesDir("models"), "model.gguf")
        Log.i("LlamaSelfTest", "runChat start: path=${modelFile.absolutePath} " +
            "exists=${modelFile.exists()} size=${modelFile.length()}")
        if (!modelFile.exists()) {
            output.text = "No model. Push one to:\n${modelFile.absolutePath}"
            return
        }
        lifecycleScope.launch {
            try {
                Log.i("LlamaSelfTest", "loading model…")
                val model = Llama.loadModel(
                    modelFile.absolutePath,
                    LlamaConfig(contextSize = 512, threads = 4),
                )
                Log.i("LlamaSelfTest", "model loaded; generating…")
                val result = Llama.complete(
                    model,
                    prompt = "Reply with exactly: Hello from llama.",
                    systemPrompt = "You are concise.",
                    maxTokens = 16,
                )
                Llama.releaseModel(model)
                Log.i("LlamaSelfTest",
                    "text='${result.text.trim()}' tokens=${result.tokensGenerated} " +
                        "tps=${result.tokensPerSecond} PASS=${result.text.isNotBlank()}")
                output.text = "${result.text.trim()}\n\n${result.tokensGenerated} tok · " +
                    "%.1f tok/s".format(result.tokensPerSecond)
            } catch (e: Throwable) {
                Log.e("LlamaSelfTest", "chat failed", e)
                output.text = "Error: ${e.message}"
            }
        }
    }
}
