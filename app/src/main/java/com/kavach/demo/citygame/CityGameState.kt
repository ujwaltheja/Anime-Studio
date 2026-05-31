package com.kavach.demo.citygame

import android.content.Context
import org.json.JSONObject

data class CityGameState(
    var budget: Int     = 50_000,
    var population: Int = 0
) {
    fun save(context: Context, grid: CityGrid) {
        val payload = JSONObject()
            .put("budget",     budget)
            .put("population", population)
            .put("gridSerial", grid.serialize())
            .toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, payload).apply()
    }

    companion object {
        private const val PREFS = "kavach_city"
        private const val KEY   = "save_v1"

        fun load(context: Context, grid: CityGrid): CityGameState? {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null) ?: return null
            return try {
                val json  = JSONObject(raw)
                val state = CityGameState(
                    budget     = json.optInt("budget",     50_000),
                    population = json.optInt("population", 0)
                )
                json.optString("gridSerial").takeIf { it.isNotEmpty() }
                    ?.let { grid.deserialize(it) }
                state
            } catch (e: Exception) { null }
        }

        fun delete(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
        }
    }
}
