package rs.ruffle

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREF_NAME = "ruffle_prefs"
    private const val KEY_GAME_FOLDER = "game_folder_uri"
    private const val KEY_GLOBAL_MAPPING = "key_mapping_global"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setGameFolderUri(context: Context, uriString: String) {
        getPrefs(context).edit().putString(KEY_GAME_FOLDER, uriString).apply()
    }

    fun getGameFolderUri(context: Context): String? {
        return getPrefs(context).getString(KEY_GAME_FOLDER, null)
    }

    // 手动解析，安全可靠
    private fun mapToString(mapping: Map<Int, Int>): String {
        return mapping.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    private fun stringToMap(data: String?): MutableMap<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        if (data.isNullOrEmpty()) return result
        try {
            data.split(",").forEach {
                val p = it.split(":")
                if (p.size == 2) result[p[0].toInt()] = p[1].toInt()
            }
        } catch (e: Exception) { }
        return result
    }

    fun saveGlobalMapping(context: Context, mapping: Map<Int, Int>) {
        getPrefs(context).edit().putString(KEY_GLOBAL_MAPPING, mapToString(mapping)).apply()
    }

    fun getGlobalMapping(context: Context): MutableMap<Int, Int> {
        return stringToMap(getPrefs(context).getString(KEY_GLOBAL_MAPPING, null))
    }

    fun saveGameMapping(context: Context, gameUri: String, mapping: Map<Int, Int>) {
        getPrefs(context).edit().putString("local_$gameUri", mapToString(mapping)).apply()
    }

    fun getGameMapping(context: Context, gameUri: String): MutableMap<Int, Int> {
        return stringToMap(getPrefs(context).getString("local_$gameUri", null))
    }
}