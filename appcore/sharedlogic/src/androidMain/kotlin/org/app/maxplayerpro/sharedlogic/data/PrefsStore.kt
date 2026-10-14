package org.app.maxplayerpro.sharedlogic.data

import android.content.SharedPreferences
import org.app.kmediaplayer.settings.KeyValueStore

class PrefsStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
    override fun keys(): Set<String> = prefs.all.keys
}
