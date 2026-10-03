package fr.nico7an.spotlight.data

/** Une activité lançable, avec ses formes normalisées précalculées pour la recherche. */
data class AppEntry(
    val label: String,
    val packageName: String,
    val className: String,
) {
    val key: String get() = "$packageName/$className"

    internal val normLabel: String = SearchEngine.normalize(label)

    /** Mots du nom, y compris les morceaux en camelCase (« YouTube » → you, tube). */
    internal val words: List<String> = label
        .split(SEPARATORS)
        .flatMap { it.split(CAMEL_CASE) }
        .map(SearchEngine::normalize)
        .filter { it.isNotEmpty() }

    /** Initiales des mots séparés par des espaces (« Google Maps » → gm). */
    internal val initials: String = label
        .split(SEPARATORS)
        .filter { it.isNotEmpty() }
        .joinToString("") { SearchEngine.normalize(it.take(1)) }

    internal val normPackage: String = packageName.lowercase()

    private companion object {
        val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
        val CAMEL_CASE = Regex("(?<=\\p{Ll})(?=\\p{Lu})")
    }
}
