package org.app.maxplayerpro.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute

/**
 * Browser-style back / forward for the title bar: back pops a page and remembers it, forward opens it again.
 * Opening any other page drops the forward pages, as in a browser.
 */
@Stable
internal class NavHistory(private val nav: NavController) {
    private val forwardPages = mutableStateListOf<Any>()
    private var stepping = false

    val canGoForward: Boolean get() = forwardPages.isNotEmpty()

    fun back() {
        val current = nav.currentBackStackEntry ?: return
        if (nav.previousBackStackEntry == null) return
        current.toAppRoute()?.let { forwardPages += it }
        stepping = true
        nav.popBackStack()
    }

    fun forward() {
        val route = forwardPages.removeLastOrNull() ?: return
        stepping = true
        nav.navigate(route)
    }

    /** Called on every page change; a change not made by [back] / [forward] clears the forward pages. */
    fun onPageChanged() {
        if (!stepping) forwardPages.clear()
        stepping = false
    }
}

/** The route object an entry was opened with, so forward can open the same page again. */
private fun NavBackStackEntry.toAppRoute(): Any? = destination.let { d ->
    when {
        d.hasRoute<Routes.Home>() -> Routes.Home
        d.hasRoute<Routes.Folder>() -> toRoute<Routes.Folder>()
        d.hasRoute<Routes.Search>() -> Routes.Search
        d.hasRoute<Routes.Music>() -> Routes.Music
        d.hasRoute<Routes.Me>() -> Routes.Me
        d.hasRoute<Routes.Theme>() -> Routes.Theme
        d.hasRoute<Routes.Settings>() -> Routes.Settings
        d.hasRoute<Routes.SettingsPage>() -> toRoute<Routes.SettingsPage>()
        d.hasRoute<Routes.Tuner>() -> toRoute<Routes.Tuner>()
        d.hasRoute<Routes.Playlists>() -> Routes.Playlists
        d.hasRoute<Routes.PlaylistDetail>() -> toRoute<Routes.PlaylistDetail>()
        d.hasRoute<Routes.PrivateFolder>() -> Routes.PrivateFolder
        d.hasRoute<Routes.MediaManager>() -> Routes.MediaManager
        d.hasRoute<Routes.MediaList>() -> toRoute<Routes.MediaList>()
        d.hasRoute<Routes.Help>() -> Routes.Help
        d.hasRoute<Routes.LocalNetwork>() -> Routes.LocalNetwork
        d.hasRoute<Routes.Unavailable>() -> toRoute<Routes.Unavailable>()
        else -> null
    }
}
