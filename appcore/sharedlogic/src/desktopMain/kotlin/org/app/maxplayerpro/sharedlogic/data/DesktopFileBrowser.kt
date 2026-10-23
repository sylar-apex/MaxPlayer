package org.app.maxplayerpro.sharedlogic.data

import org.app.maxplayerpro.sharedlogic.scan.StorageRoots

/** The home folder, then every drive the library knows. */
class DesktopFileBrowser : JvmFileBrowser() {
    override val hasAccess = true
    override fun requestAccess() {}
    override fun roots(): List<FileRoot> {
        val home = System.getProperty("user.home")
        return (listOf(FileRoot("Home", home)) + StorageRoots.current().map { FileRoot(it.label, it.path) }).distinctBy { it.path }
    }
}
