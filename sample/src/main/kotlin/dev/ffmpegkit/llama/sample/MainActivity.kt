package dev.ffmpegkit.llama.sample

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import kotlinx.coroutines.launch
import java.io.File

/**
 * Minimal demo for llama-android: show llama.cpp system info, then load a GGUF
 * model and run one chat completion when the button is tapped.
 *
 * Push a model to the app's external files dir first, e.g.:
 *
 *   adb push model.gguf \
 *     /sdcard/Android/data/dev.ffmpegkit.llama.sample/files/models/model.gguf
 */
class MainActivity : AppCompatActivity() {

    private lateinit var output: TextView
    private lateinit var runButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        output = findViewById(R.id.output)
        runButton = findViewById(R.id.runButton)

        output.text = "llama.cpp system info:\n\n${Llama.getSystemInfo()}"
        runButton.setOnClickListener { runChat() }
    }

    private fun runChat() {
        val modelFile = File(getExternalFilesDir("models"), "model.gguf")
        if (!modelFile.exists()) {
            output.text = "No model found. Push one to:\n${modelFile.absolutePath}"
            return
        }
        runButton.isEnabled = false
        output.text = "Generating…"
        lifecycleScope.launch {
            try {
                val model = Llama.loadModel(
                    modelFile.absolutePath,
                    LlamaConfig(contextSize = 2048, threads = 4),
                )
                val result = Llama.complete(
                    model,
                    prompt = "Write a one-sentence greeting from a friendly llama.",
                    systemPrompt = "You are a concise, helpful assistant.",
                    maxTokens = 64,
                )
                Llama.releaseModel(model)
                output.text = "${result.text.trim()}\n\n" +
                    "${result.tokensGenerated} tok · %.1f tok/s".format(result.tokensPerSecond)
            } catch (e: Throwable) {
                output.text = "Error: ${e.message}"
            } finally {
                runButton.isEnabled = true
            }
        }
    }
}
