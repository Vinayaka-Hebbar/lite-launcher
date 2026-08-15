package com.hebbar.litelauncher

import com.hebbar.litelauncher.drawer.AppSearch
import com.hebbar.litelauncher.model.LaunchableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {

    private val sampleApps = listOf(
        LaunchableApp("com.google.android.youtube", "MainActivity", "YouTube"),
        LaunchableApp("com.whatsapp", "HomeActivity", "WhatsApp"),
        LaunchableApp("com.android.calculator2", "Calculator", "Calculator"),
        LaunchableApp("com.google.android.apps.maps", "MapsActivity", "Google Maps"),
        LaunchableApp("com.spotify.music", "MainActivity", "Spotify")
    )

    @Test
    fun testExactAndSubstringMatch() {
        val result = AppSearch.filter(sampleApps, "spot")
        assertEquals(1, result.size)
        assertEquals("Spotify", result[0].label)
    }

    @Test
    fun testInitialsAndPrefixMatch() {
        val ytResult = AppSearch.filter(sampleApps, "yt")
        assertTrue(ytResult.any { it.label == "YouTube" })

        val whResult = AppSearch.filter(sampleApps, "wh")
        assertTrue(whResult.any { it.label == "WhatsApp" })
    }

    @Test
    fun testPackageSubstringMatch() {
        val calcResult = AppSearch.filter(sampleApps, "calc")
        assertTrue(calcResult.any { it.label == "Calculator" })
    }

    @Test
    fun testEmptyQueryReturnsAll() {
        val result = AppSearch.filter(sampleApps, "")
        assertEquals(5, result.size)
    }
}
