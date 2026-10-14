package org.app.maxplayerpro.settings.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.launch
import org.app.kmediaplayer.KMediaPlayer
import org.app.kmediaplayer.settings.ObservableSettings
import org.app.maxplayerpro.sharedlogic.data.ThumbnailCache
import org.app.maxplayerpro.sharedlogic.data.UserDataRepository
import org.app.maxplayerpro.sharedlogic.model.UserData
import org.app.maxplayerpro.sharedlogic.platform.PlatformApp
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

class SettingsViewModel(
    private val settings: ObservableSettings,
    private val userData: UserDataRepository,
    private val thumbnails: ThumbnailCache,
    private val operations: MediaOperations,
) : MVIBaseViewModel<SettingsContract.Event, SettingsContract.State, SettingsContract.Effect>() {

    override fun createInitialState() = SettingsContract.State()
    override fun handleEffect(effect: SettingsContract.Effect) {}

    init {
        viewModelScope.launch { settings.version.collect { v -> setState { copy(version = v) } } }
    }

    override fun handleEvent(event: SettingsContract.Event) {
        when (event) {
            SettingsContract.Event.SettingChanged -> KMediaPlayer.engine.applySettings()
            SettingsContract.Event.ClearHistory -> userData.update { it.clearHistory() }
            SettingsContract.Event.ClearThumbnailCache -> { thumbnails.clear(); operations.toast("Thumbnail cache cleared") }
            SettingsContract.Event.ResetSettings -> { settings.reset(); KMediaPlayer.engine.applySettings() }
            SettingsContract.Event.Export -> viewModelScope.launch {
                val text = settings.raw.export(mapOf("userdata" to userData.value.encode()))
                when (PlatformApp.saveText("maxplayerpro-settings.txt", text)) {
                    true -> operations.toast("Settings exported")
                    false -> operations.toast("Export failed")
                    null -> {}
                }
            }
            SettingsContract.Event.Import -> viewModelScope.launch {
                val text = PlatformApp.openText(listOf("text/*", "application/octet-stream")) ?: return@launch
                runCatching {
                    val user = settings.import(text)
                    user["userdata"]?.let { raw -> userData.update { UserData.decode(raw) } }
                }.onSuccess { operations.toast("Settings imported") }.onFailure { operations.toast("Import failed") }
            }
            SettingsContract.Event.OpenLanguageSettings -> PlatformApp.openLanguageSettings()
        }
    }
}
