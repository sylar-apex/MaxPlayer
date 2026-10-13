package org.app.nxplayer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.app.nxplayer.core.KeyValueStore
import org.app.nxplayer.core.Setting
import org.app.nxplayer.core.Settings
import org.app.nxplayer.core.UserData

class PrefsStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
    override fun keys(): Set<String> = prefs.all.keys
}

/** Settings wrapper that bumps [version] on every write so Compose screens can recompose. */
class ObservableSettings(store: KeyValueStore) {
    val raw = Settings(store)
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    operator fun <T> get(s: Setting<T>): T = raw[s]
    operator fun <T> set(s: Setting<T>, value: T) { raw[s] = value; _version.value++ }
    fun reset() { raw.reset(); _version.value++ }
    fun import(text: String): Map<String, String> = raw.import(text).also { _version.value++ }
}

class UserDataStore(private val prefs: SharedPreferences) {
    private val _data = MutableStateFlow(UserData.decode(prefs.getString(KEY, null)))
    val data: StateFlow<UserData> = _data.asStateFlow()
    val value: UserData get() = _data.value

    @Synchronized
    fun update(transform: (UserData) -> UserData) {
        val next = transform(_data.value)
        _data.value = next
        prefs.edit().putString(KEY, next.encode()).apply()
    }

    private companion object { const val KEY = "user_data" }
}

object AppGraph {
    lateinit var app: Context; private set
    lateinit var settings: ObservableSettings; private set
    lateinit var userData: UserDataStore; private set
    lateinit var media: MediaRepository; private set
    lateinit var privateFolder: PrivateFolder; private set

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        settings = ObservableSettings(PrefsStore(app.getSharedPreferences("settings", Context.MODE_PRIVATE)))
        userData = UserDataStore(app.getSharedPreferences("user", Context.MODE_PRIVATE))
        media = MediaRepository(app)
        privateFolder = PrivateFolder(app)
    }
}
