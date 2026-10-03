package fr.nico7an.spotlight.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    private fun app(label: String, pkg: String = "com.example.${label.lowercase().replace(" ", "")}") =
        AppEntry(label, pkg, "$pkg.Main")

    private val apps = listOf(
        app("Chrome", "com.android.chrome"),
        app("Calculatrice"),
        app("Google Maps", "com.google.android.apps.maps"),
        app("Gmail", "com.google.android.gm"),
        app("Paramètres", "com.android.settings"),
        app("YouTube Music", "com.google.android.apps.youtube.music"),
        app("YouTube"),
        app("WhatsApp"),
    )

    private fun top(query: String) = SearchEngine.search(apps, query).firstOrNull()?.label

    @Test
    fun `prefix match comes first`() = assertEquals("Chrome", top("chr"))

    @Test
    fun `accents are ignored`() = assertEquals("Paramètres", top("parametres"))

    @Test
    fun `initials match multi word names`() = assertEquals("YouTube Music", top("ym"))

    @Test
    fun `word start match`() = assertEquals("Google Maps", top("maps"))

    @Test
    fun `camel case words are searchable`() = assertEquals("WhatsApp", top("app"))

    @Test
    fun `shortest prefix match wins`() = assertEquals("YouTube", top("youtube"))

    @Test
    fun `fuzzy subsequence match`() = assertEquals("YouTube Music", top("ytmsc"))

    @Test
    fun `package name is a last resort`() = assertEquals("Paramètres", top("settings"))

    @Test
    fun `usage boost breaks ties`() {
        val results = SearchEngine.search(apps, "g") { if (it.label == "Gmail") 100 else 0 }
        assertEquals("Gmail", results.first().label)
    }

    @Test
    fun `blank query returns nothing`() = assertTrue(SearchEngine.search(apps, "  ").isEmpty())

    @Test
    fun `no match returns nothing`() = assertTrue(SearchEngine.search(apps, "zzzz").isEmpty())
}
