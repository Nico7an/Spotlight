package fr.nico7an.spotlight.data

import android.content.Context

/** Fréquence et récence d'ouverture des apps, pour les suggestions et le classement. */
class UsageStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("usage", Context.MODE_PRIVATE)

    fun record(key: String) {
        prefs.edit()
            .putInt(countKey(key), count(key) + 1)
            .putLong(timeKey(key), System.currentTimeMillis())
            .apply()
    }

    fun boost(key: String): Int {
        val count = count(key)
        if (count == 0) return 0
        val ageHours = (System.currentTimeMillis() - prefs.getLong(timeKey(key), 0L)) / 3_600_000L
        val recency = when {
            ageHours < 1 -> 40
            ageHours < 24 -> 25
            ageHours < 24 * 7 -> 10
            else -> 0
        }
        return minOf(count * 3, 60) + recency
    }

    fun suggestions(apps: List<AppEntry>, limit: Int = 8): List<AppEntry> =
        apps.asSequence()
            .map { it to boost(it.key) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()

    private fun count(key: String) = prefs.getInt(countKey(key), 0)
    private fun countKey(key: String) = "c|$key"
    private fun timeKey(key: String) = "t|$key"

    companion object {
        @Volatile
        private var instance: UsageStore? = null

        fun get(context: Context): UsageStore =
            instance ?: synchronized(this) {
                instance ?: UsageStore(context.applicationContext).also { instance = it }
            }
    }
}
