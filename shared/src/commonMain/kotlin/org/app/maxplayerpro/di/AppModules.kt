package org.app.maxplayerpro.di

import com.psbapp.di.KoinModulesInterface
import com.psbapp.utils.dispatcher.dispatcherDiModule
import org.app.maxplayerpro.appcore.di.appCoreModule
import org.app.maxplayerpro.folder.di.folderDomainModule
import org.app.maxplayerpro.files.di.filesDomainModule
import org.app.maxplayerpro.gallery.di.galleryDomainModule
import org.app.maxplayerpro.help.di.helpDomainModule
import org.app.maxplayerpro.home.di.homeDomainModule
import org.app.maxplayerpro.localnetwork.di.localNetworkDomainModule
import org.app.maxplayerpro.mediamanager.di.mediaManagerDomainModule
import org.app.maxplayerpro.music.di.musicDomainModule
import org.app.maxplayerpro.player.di.playerDomainModule
import org.app.maxplayerpro.playlists.di.playlistsDomainModule
import org.app.maxplayerpro.privatefolder.di.privateFolderDomainModule
import org.app.maxplayerpro.profile.di.profileDomainModule
import org.app.maxplayerpro.search.di.searchDomainModule
import org.app.maxplayerpro.settings.di.settingsDomainModule
import org.app.maxplayerpro.sharedlogic.di.sharedLogicModule
import org.app.maxplayerpro.theme.di.themeDomainModule
import org.koin.core.module.Module

/** Every Koin module of the app: core data, shared UI and one module per feature. */
object AppModules : KoinModulesInterface {
    override fun getModules(): List<Module> = listOf(
        dispatcherDiModule(),
        sharedLogicModule(),
        appCoreModule(),
        homeDomainModule(),
        folderDomainModule(),
        searchDomainModule(),
        musicDomainModule(),
        playerDomainModule(),
        settingsDomainModule(),
        profileDomainModule(),
        themeDomainModule(),
        playlistsDomainModule(),
        privateFolderDomainModule(),
        mediaManagerDomainModule(),
        helpDomainModule(),
        localNetworkDomainModule(),
        galleryDomainModule(),
        filesDomainModule(),
    )
}
