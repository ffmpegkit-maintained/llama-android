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
    }

    private fun runChat() {
        val modelFile = File(getExternalFilesDir("models"), "model.gguf")
        if (!modelFile.exists()) {
            output.text = "No model. Push one to:\n${modelFile.absolutePath}"
            Log.i("LlamaSelfTest", "no model at ${modelFile.absolutePath}")
            return
        }
        lifecycleScope.launch {
            try {
                output.text = "Loading model…"
                val model = Llama.loadModel(
                    modelFile.absolutePath,
                    LlamaConfig(contextSize = 2048, threads = 4),
                )
                output.text = "Generating…"
                val result = Llama.complete(
                    model,
                    prompt = "In one short sentence, what is a large language model?",
                    systemPrompt = "You are a concise, helpful assistant.",
                    maxTokens = 128,
                )
                Llama.releaseModel(model)
                output.text = buildString {
                    append(result.text.trim()).append("\n\n")
                    append("${result.tokensGenerated} tokens · ")
                    append("%.1f tok/s".format(result.tokensPerSecond))
                }
                Log.i("LlamaSelfTest",
                    "text='${result.text.trim()}' tokens=${result.tokensGenerated} " +
                        "tps=${result.tokensPerSecond} PASS=${result.text.isNotBlank()}")
            } catch (e: Exception) {
                output.text = "Error: ${e.message}"
                Log.e("LlamaSelfTest", "chat failed", e)
            }
        }
    }
}
