package com.abrarshakhi.bricks.core.domain.search

import com.abrarshakhi.bricks.core.domain.model.App
import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppUsage
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchEngineTest {

    private val engine = AppSearchEngine()

    private fun app(label: String, pkg: String = "com.example.${label.lowercase().replace(" ", "")}", launches: Int = 0) = App(
        key = AppKey(pkg, "$pkg.Main", 0L),
        label = label,
        systemLabel = label,
        profile = ProfileType.Personal,
        installedAt = 0L,
        isSystem = false,
        isHidden = false,
        isSuspended = false,
        usage = AppUsage(launchCount = launches, lastLaunchedAt = 0L),
    )

    private fun search(query: String, vararg apps: App) = engine.search(apps.toList(), query, now = 0L).map { it.label }

    @Test
    fun `exact and prefix matches rank first`() {
        val result = search("cal", app("Calendar"), app("Calculator"), app("Local Radio"), app("Cal"))
        assertEquals("Cal", result.first())
        assertTrue(result.indexOf("Calendar") < result.indexOf("Local Radio"))
    }

    @Test
    fun `word prefixes beat substrings`() {
        val result = search("maps", app("Google Maps"), app("Bitmapster"))
        assertEquals(listOf("Google Maps", "Bitmapster"), result)
    }

    @Test
    fun `initials match multi word labels`() {
        assertEquals(listOf("Google Maps"), search("gm", app("Google Maps"), app("Gallery")))
    }

    @Test
    fun `search ignores case and accents`() {
        assertEquals(listOf("Café Menu"), search("CAFE", app("Café Menu"), app("Tea")))
    }

    @Test
    fun `fuzzy subsequences still match`() {
        assertEquals(listOf("Spotify"), search("sptfy", app("Spotify"), app("Settings")))
    }

    @Test
    fun `usage breaks ties`() {
        val result = search("note", app("Notes", pkg = "a.notes"), app("Notebook", pkg = "b.notebook", launches = 50))
        assertEquals("Notes", result.first())
        val tie = engine.search(
            listOf(app("Note A", pkg = "x.a"), app("Note B", pkg = "x.b", launches = 9)),
            "note",
            now = 0L,
        ).map { it.label }
        assertEquals("Note B", tie.first())
    }

    @Test
    fun `blank queries return nothing`() {
        assertTrue(search("   ", app("Anything")).isEmpty())
    }

    @Test
    fun `package names are searchable`() {
        assertEquals(listOf("Browser"), search("chrome", app("Browser", pkg = "com.android.chrome")))
    }
}
