plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.vanniktech.publish)
}

android {
    namespace = "dev.ffmpegkit.llama"
    compileSdk = 35
    ndkVersion = "27.2.12479018" // NDK r27c

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("proguard-rules.pro")

        ndk { abiFilters += "arm64-v8a" }   // Free = arm64-v8a only

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DGGML_OPENMP=OFF",   // no libomp.so runtime dependency
                    "-DGGML_VULKAN=OFF",   // Free = CPU/NEON only (Pro enables Vulkan)
                    "-DGGML_LLAMAFILE=OFF",
                    "-DLLAMA_CURL=OFF",    // no libcurl dependency
                )
                cppFlags += "-O3"
                // Build only our JNI lib (+ its deps libllama/libggml). Skips
                // llama.cpp's many CLI tools/examples, which we don't ship.
                targets += "llama_jni"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    // Keep .so 16 KB page aligned (Android 15 ready).
    packaging { jniLibs { useLegacyPackaging = false } }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
}

mavenPublishing {
    coordinates("dev.ffmpegkit-maintained", "llama-android", providers.gradleProperty("VERSION").get())

    // Sign only when a GPG key is configured (Maven Central); JitPack/local skip it.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    publishToMavenCentral(automaticRelease = true)

    pom {
        name = "llama-android"
        description = "Llama.cpp for Android — prebuilt AAR for on-device LLM inference: chat, text completion, embeddings. GGUF models, no NDK, no cloud, no API key. arm64-v8a, API 24+. jokobee.com"
        inceptionYear = "2026"
        url = "https://github.com/ffmpegkit-maintained/llama-android"
        licenses {
            license {
                name = "MIT License"
                url = "https://github.com/ffmpegkit-maintained/llama-android/blob/main/LICENSE"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "jokobee"; name = "Jokobee"; url = "https://www.jokobee.com"
                email = "contact@jokobee.com"; organization = "Jokobee"; organizationUrl = "https://www.jokobee.com"
            }
        }
        scm {
            url = "https://github.com/ffmpegkit-maintained/llama-android"
            connection = "scm:git:git://github.com/ffmpegkit-maintained/llama-android.git"
            developerConnection = "scm:git:ssh://git@github.com/ffmpegkit-maintained/llama-android.git"
        }
    }
}

// --- THIRD-PARTY-NOTICES : bundle automatique dans les assets de l'AAR ---
tasks.register<Copy>("copyThirdPartyNotices") {
    from(rootProject.file("THIRD-PARTY-NOTICES.txt"))
    into(layout.projectDirectory.dir("src/main/assets"))
}
tasks.named("preBuild") { dependsOn("copyThirdPartyNotices") }
