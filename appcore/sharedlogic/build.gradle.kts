import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "org.app.maxplayerpro.sharedlogic"
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
            baseName = "sharedlogic"
            isStatic = true
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    // Keep the standard ios/web source-set tree; the JVM-only set below is added on top of it.
    applyDefaultHierarchyTemplate()

    sourceSets {
        // Code shared by the two JVM targets (Android and desktop), such as the file-system media walker.
        val jvmSharedMain by creating { dependsOn(commonMain.get()) }
        androidMain.get().dependsOn(jvmSharedMain)
        val desktopMain by getting { dependsOn(jvmSharedMain) }
        commonMain.dependencies {
            api(projects.kmediaplayer.core)
            api(projects.kmpcore.di)
            api(projects.kmpcore.utils)
            api(projects.kmpcore.mvi)
            api(libs.koin.core.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val desktopTest by getting {
            dependencies { implementation(libs.kotlin.test) }
        }
    }
}
