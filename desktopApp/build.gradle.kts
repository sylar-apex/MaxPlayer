import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.internal.os.OperatingSystem
import javax.inject.Inject
import java.net.URI
import java.nio.file.Files

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

/**
 * libVLC ships inside the app, so playback never depends on a VLC install. The official VideoLAN builds are
 * downloaded once into the Gradle cache and their `lib` + `plugins` copied to `<resources>/<os>/vlc`, which
 * Compose bundles (and passes to `run`) as `compose.application.resources.dir`. Linux uses the distribution's
 * libvlc package instead (no portable VideoLAN build exists).
 */
val vlcVersion = "3.0.24"
val vlcResources = layout.buildDirectory.dir("vlc-resources")

abstract class PrepareVlc @Inject constructor(
    private val exec: ExecOperations,
    private val files: FileSystemOperations,
    private val archives: ArchiveOperations,
) : DefaultTask() {
    @get:Input abstract val version: Property<String>
    @get:Internal abstract val cacheDir: DirectoryProperty
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val v = version.get()
        val root = outputDir.get().asFile
        val os = OperatingSystem.current()
        when {
            os.isMacOsX -> {
                val target = File(root, "macos/vlc")
                if (File(target, "lib/libvlc.dylib").exists()) return
                val dmg = download("https://download.videolan.org/pub/videolan/vlc/$v/macosx/vlc-$v-universal.dmg", "vlc-universal.dmg")
                // Mount outside the project: hdiutil refuses mount points on some external volumes.
                val mount = Files.createTempDirectory("maxplayer-vlc").toFile()
                exec.exec { commandLine("hdiutil", "attach", "-nobrowse", "-readonly", "-mountpoint", mount.path, dmg.path) }
                try {
                    val app = File(mount, "VLC.app/Contents/MacOS")
                    target.mkdirs()
                    // ditto keeps symlinks and signatures, and --arch thins the universal build to this Mac's CPU
                    // (Compose packages one installer per architecture).
                    val arch = if (System.getProperty("os.arch") == "aarch64") "arm64" else "x86_64"
                    listOf("lib", "plugins").forEach { exec.exec { commandLine("ditto", "--arch", arch, File(app, it).path, File(target, it).path) } }
                    // Its timestamps no longer match after the copy, so libVLC would log every plugin as stale.
                    File(target, "plugins/plugins.dat").delete()
                } finally {
                    exec.exec { commandLine("hdiutil", "detach", mount.path, "-quiet") }
                    mount.delete()
                }
            }
            os.isWindows -> {
                val target = File(root, "windows/vlc")
                if (File(target, "libvlc.dll").exists()) return
                val zip = download("https://download.videolan.org/pub/videolan/vlc/$v/win64/vlc-$v-win64.zip", "vlc-win64.zip")
                files.copy {
                    from(archives.zipTree(zip)) {
                        include("vlc-$v/libvlc.dll", "vlc-$v/libvlccore.dll", "vlc-$v/plugins/**")
                        eachFile { relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray()) }
                        includeEmptyDirs = false
                    }
                    into(target)
                }
            }
            else -> logger.lifecycle("Linux: install the libvlc package (e.g. `apt install vlc`); it is not bundled.")
        }
    }

    private fun download(url: String, name: String): File = File(cacheDir.get().asFile, name).also { f ->
        if (f.exists()) return@also
        f.parentFile.mkdirs()
        logger.lifecycle("Downloading $url")
        val tmp = File(f.path + ".part")
        URI(url).toURL().openStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
        tmp.renameTo(f)
    }
}

val prepareVlc by tasks.registering(PrepareVlc::class) {
    version.set(vlcVersion)
    cacheDir.set(File(gradle.gradleUserHomeDir, "caches/maxplayer-vlc/$vlcVersion"))
    outputDir.set(vlcResources)
}

compose.desktop {
    application {
        mainClass = "org.app.maxplayerpro.MainKt"
        // `-PvlcPath=<dir with libvlc>` runs against a specific VLC build instead of the bundled one.
        providers.gradleProperty("vlcPath").orNull?.let { jvmArgs += "-Dvlc.path=$it" }
        providers.gradleProperty("vlcVerbose").orNull?.let { jvmArgs += "-Dvlc.verbose=true" }
        // `-PlibraryRoots=/a:/b` scans only those folders (testing, or a user who wants specific drives).
        providers.gradleProperty("libraryRoots").orNull?.let { jvmArgs += "-Dlibrary.roots=$it" }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Max Player"
            packageVersion = "1.0.0"
            // vlcj's callback video surface needs sun.misc.Unsafe; without it frames never allocate (audio only).
            modules("jdk.unsupported")
            macOS {
                bundleID = "org.app.maxplayerpro"
                dockName = "Max Player"
                iconFile.set(project.file("icons/icon.icns"))
            }
            windows { iconFile.set(project.file("icons/icon.ico")) }
            linux { iconFile.set(project.file("icons/icon.png")) }
            appResourcesRootDir.set(vlcResources)
        }
    }
}
tasks.matching { it.name == "prepareAppResources" }.configureEach { dependsOn(prepareVlc) }
