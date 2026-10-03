package fr.nico7an.spotlight.data

import java.text.Normalizer
import java.util.Locale

object SearchEngine {

    private val DIACRITICS = Regex("\\p{Mn}+")

    /** Minuscules, sans accents : « Paramètres » → « parametres ». */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase(Locale.ROOT)
            .trim()

    /**
     * Trie les apps qui correspondent à [query] de la plus pertinente à la moins pertinente.
     * [boost] permet de favoriser les apps souvent utilisées.
     */
    fun search(apps: List<AppEntry>, query: String, boost: (AppEntry) -> Int = { 0 }): List<AppEntry> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        return apps.asSequence()
            .map { it to score(it, q) }
            .filter { it.second > 0 }
            .map { (app, score) -> app to score + boost(app) }
            .sortedWith(compareByDescending<Pair<AppEntry, Int>> { it.second }.thenBy { it.first.normLabel })
            .map { it.first }
            .toList()
    }

    internal fun score(app: AppEntry, q: String): Int {
        val name = app.normLabel
        return when {
            name == q -> 1000
            name.startsWith(q) -> 900 - minOf(name.length - q.length, 50)
            app.words.any { it.startsWith(q) } -> 760
            q.length >= 2 && app.initials.startsWith(q) -> 700
            name.contains(q) -> 600 - minOf(name.indexOf(q), 50)
            q.contains(' ') && name.replace(" ", "").contains(q.replace(" ", "")) -> 550
            else -> {
                val fuzzy = fuzzy(name, q)
                when {
                    fuzzy > 0 -> 200 + fuzzy
                    q.length >= 3 && app.normPackage.contains(q) -> 120
                    else -> 0
                }
            }
        }
    }

    /** Correspondance par sous-séquence (« ytm » → « youtube music »), entre 0 et 200. */
    internal fun fuzzy(text: String, q: String): Int {
        if (q.length < 2) return 0
        var from = 0
        var gaps = 0
        var first = -1
        var last = -1
        for (c in q) {
            if (c == ' ') continue
            val idx = text.indexOf(c, from)
            if (idx < 0) return 0
            if (first < 0) first = idx
            if (last >= 0) gaps += idx - last - 1
            last = idx
            from = idx + 1
        }
        return (200 - gaps * 8 - first * 4).coerceIn(1, 200)
    }
}
