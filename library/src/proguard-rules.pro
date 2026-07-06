# JNI bridge — native methods resolved by name.
-keep class dev.ffmpegkit.llama.LlamaJNI { *; }
-keepclasseswithmembernames class * { native <methods>; }
# Public API
-keep public class dev.ffmpegkit.llama.** { public *; }
