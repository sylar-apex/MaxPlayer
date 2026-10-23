import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val features = listOf(
    "home", "folder", "search", "music", "player", "settings", "profile",
    "theme", "playlists", "privatefolder", "mediamanager", "help", "localnetwork", "gallery", "files",
)

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Swift implements the player's PlaybackBackend (VLCKit) and registers it with IosPlayback.
            export(projects.kmediaplayer.core)
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "org.app.maxplayerpro.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(libs.versions.javaVersion.get())
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.appcore)
            api(projects.kmediaplayer.core)
            api(projects.kmpcore.di)
            features.forEach { f ->
                api(project(":appfeatures:$f:domain"))
                api(project(":appfeatures:$f:presentation"))
            }
            implementation(libs.kotlin.serialization)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
