package org.app.maxplayerpro.sharedlogic.data

/**
 * Lets the user add a folder to the library on platforms where the app cannot scan storage itself
 * (the browser). Native apps scan every drive on their own and use [None].
 */
interface FolderImport {
    val available: Boolean

    /** Opens the platform's folder picker; picked media lands in the [MediaRepository]. */
    fun pickFolder()

    object None : FolderImport {
        override val available = false
        override fun pickFolder() {}
    }
}
