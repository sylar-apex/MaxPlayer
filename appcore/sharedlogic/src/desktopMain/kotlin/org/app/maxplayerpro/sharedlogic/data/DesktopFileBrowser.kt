package org.app.maxplayerpro.sharedlogic.data

import java.io.File

/** Finder-style places: everyday folders in the home folder, then home, the startup disk and every other drive. */
class DesktopFileBrowser : JvmFileBrowser() {
    override val hasAccess = true
    override fun requestAccess() {}

    override fun roots(): List<FileRoot> {
        val home = File(System.getProperty("user.home"))
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val favourites = listOf(
            "Desktop" to PlaceKind.Desktop, "Documents" to PlaceKind.Documents, "Downloads" to PlaceKind.Downloads,
            "Movies" to PlaceKind.Movies, "Videos" to PlaceKind.Movies, "Music" to PlaceKind.Music, "Pictures" to PlaceKind.Pictures,
        ).map { (name, kind) -> File(home, name) to kind }.filter { (f, _) -> f.isDirectory }
            .map { (f, kind) -> FileRoot(f.name, f.path, kind, PlaceSection.Favourites) }
            .let { list -> if ("mac" in os && File("/Applications").isDirectory) list + FileRoot("Applications", "/Applications", PlaceKind.Applications, PlaceSection.Favourites) else list }
        val homePlace = FileRoot(home.name, home.path, PlaceKind.Home)
        val drives = when {
            "mac" in os -> listOf(FileRoot(startupDiskName(), "/", PlaceKind.Disk)) +
                File("/Volumes").listFiles().orEmpty()
                    .filter { it.isDirectory && runCatching { it.canonicalPath != "/" }.getOrDefault(false) }
                    .sortedBy { it.name.lowercase() }.map { FileRoot(it.name, it.path, PlaceKind.Removable) }
            "win" in os -> File.listRoots().map { FileRoot(it.path.trimEnd('\\'), it.path, PlaceKind.Disk) }
            else -> listOf(FileRoot("Computer", "/", PlaceKind.Disk)) +
                listOf("/media/${home.name}", "/media", "/mnt", "/run/media/${home.name}").flatMap { File(it).listFiles().orEmpty().toList() }
                    .filter { it.isDirectory }.distinctBy { it.path }.map { FileRoot(it.name, it.path, PlaceKind.Removable) }
        }
        return (favourites + homePlace + drives).distinctBy { it.path }
    }

    /** The startup disk's name as Finder shows it ("Macintosh HD"). */
    private fun startupDiskName(): String = File("/Volumes").listFiles().orEmpty()
        .firstOrNull { runCatching { it.canonicalPath == "/" }.getOrDefault(false) }?.name ?: "Macintosh HD"
}
