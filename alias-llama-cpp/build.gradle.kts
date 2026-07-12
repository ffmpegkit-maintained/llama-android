// Maven Central search alias. No code — it publishes an artifact named
// `llama-cpp-android` whose POM simply depends on the real library
// `llama-android`, so developers searching for "llama cpp" also find it.
plugins {
    `java-library`
    alias(libs.plugins.vanniktech.publish)
}

val llamaVersion = providers.gradleProperty("VERSION").get()

mavenPublishing {
    coordinates("dev.ffmpegkit-maintained", "llama-cpp-android", llamaVersion)

    // Sign only when a GPG key is configured (see library/build.gradle.kts).
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    publishToMavenCentral(automaticRelease = true)

    pom {
        name = "llama.cpp for Android"
        description = "Maven Central search alias → dev.ffmpegkit-maintained:llama-android. " +
            "Prebuilt llama.cpp AAR for Android — on-device LLM inference (chat, completion, embeddings), no NDK, no cloud, no API key. GGUF models, arm64-v8a, API 24+. jokobee.com"
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
                id = "jokobee"
                name = "Jokobee"
                url = "https://www.jokobee.com"
                email = "contact@jokobee.com"
                organization = "Jokobee"
                organizationUrl = "https://www.jokobee.com"
            }
        }

        scm {
            url = "https://github.com/ffmpegkit-maintained/llama-android"
            connection = "scm:git:git://github.com/ffmpegkit-maintained/llama-android.git"
            developerConnection = "scm:git:ssh://git@github.com/ffmpegkit-maintained/llama-android.git"
        }
    }
}

dependencies {
    api("dev.ffmpegkit-maintained:llama-android:$llamaVersion")
}
