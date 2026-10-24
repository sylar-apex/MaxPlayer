import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "org.app.maxplayerpro.appcore"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(libs.versions.javaVersion.get())
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "appcore"
            isStatic = true
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.appcore.appres)
            api(projects.appcore.sharedlogic)
            api(projects.kmediaplayer.ui)
            api(projects.kmpcore.mvi)
            api(projects.kmpcore.navigation)
            api(projects.kmpcore.utils)
            api(libs.compose.material3)
            api(libs.compose.materialIconsExtended)
            api(libs.compose.uiBackhandler)
            // Photos: local files and, from other devices, over HTTP.
            api(libs.coil.compose)
            api(libs.coil.network.ktor3)
            implementation(libs.ktor.client.core)
            api(libs.compose.uiToolingPreview)
            api(libs.androidx.lifecycle.runtimeCompose)
            api(libs.androidx.lifecycle.viewmodelCompose)
            api(libs.koin.compose.viewmodel)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
