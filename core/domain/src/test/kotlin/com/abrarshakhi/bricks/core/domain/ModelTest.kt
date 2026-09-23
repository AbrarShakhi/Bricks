package com.abrarshakhi.bricks.core.domain

import com.abrarshakhi.bricks.core.domain.model.AppKey
import com.abrarshakhi.bricks.core.domain.model.AppUsage
import com.abrarshakhi.bricks.core.domain.model.GridSize
import com.abrarshakhi.bricks.core.domain.model.GridSpan
import com.abrarshakhi.bricks.core.domain.model.ProfileType
import com.abrarshakhi.bricks.core.domain.model.WidgetProvider
import com.abrarshakhi.bricks.core.domain.model.WidgetProviderKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {

    @Test
    fun `app keys round trip through their flattened form`() {
        val key = AppKey("com.example.app", "com.example.app.ui.MainActivity", 10L)
        assertEquals(key, AppKey.parse(key.flattened))
    }

    @Test
    fun `malformed app keys are rejected`() {
        assertNull(AppKey.parse("nothing"))
        assertNull(AppKey.parse("pkg/cls#"))
        assertNull(AppKey.parse("pkg/cls#abc"))
        assertNull(AppKey.parse("/cls#0"))
    }

    @Test
    fun `recent launches outweigh stale ones`() {
        val now = 1_000_000_000L
        val recent = AppUsage(launchCount = 5, lastLaunchedAt = now)
        val stale = AppUsage(launchCount = 5, lastLaunchedAt = now - 30L * 24 * 3_600_000)
        assertTrue(recent.score(now) > stale.score(now))
        assertEquals(0.0, AppUsage().score(now), 0.0)
    }

    @Test
    fun `recommended grids scale with the device`() {
        assertEquals(GridSize(4, 4), GridSize.recommendedFor(360, 640))
        assertEquals(GridSize(4, 6), GridSize.recommendedFor(360, 800))
        assertEquals(GridSize(5, 6), GridSize.recommendedFor(412, 915))
        assertEquals(GridSize(6, 5), GridSize.recommendedFor(800, 1280))
        assertEquals(GridSize(8, 5), GridSize.recommendedFor(900, 1400))
    }

    @Test
    fun `grid sizes are clamped`() {
        assertEquals(GridSize(GridSize.MIN_COLUMNS, GridSize.MAX_ROWS), GridSize.of(1, 99))
    }

    private fun provider(
        minWidth: Int,
        minHeight: Int,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        resizable: Boolean = true,
        maxWidth: Int? = null,
    ) = WidgetProvider(
        key = WidgetProviderKey("p", "c", 0L),
        label = "Widget",
        appLabel = "App",
        description = null,
        profile = ProfileType.Personal,
        minWidthDp = minWidth,
        minHeightDp = minHeight,
        minResizeWidthDp = 40,
        minResizeHeightDp = 40,
        maxResizeWidthDp = maxWidth,
        maxResizeHeightDp = null,
        targetCellWidth = targetWidth,
        targetCellHeight = targetHeight,
        resizeHorizontal = resizable,
        resizeVertical = resizable,
        isConfigurable = false,
    )

    @Test
    fun `widget spans follow min size and target cells`() {
        val grid = GridSize(5, 6)
        assertEquals(GridSpan(3, 2), provider(180, 110).defaultSpan(80f, 100f, grid))
        assertEquals(GridSpan(4, 1), provider(180, 110, targetWidth = 4, targetHeight = 1).defaultSpan(80f, 100f, grid))
        assertEquals(GridSpan(5, 6), provider(2000, 2000).defaultSpan(80f, 100f, grid))
    }

    @Test
    fun `widget resize limits respect the provider`() {
        val grid = GridSize(5, 6)
        val fixed = provider(180, 110, resizable = false)
        assertEquals(fixed.defaultSpan(80f, 100f, grid), fixed.minimumSpan(80f, 100f, grid))
        assertEquals(fixed.defaultSpan(80f, 100f, grid), fixed.maximumSpan(80f, 100f, grid))
        val resizable = provider(180, 110, maxWidth = 250)
        assertEquals(GridSpan(1, 1), resizable.minimumSpan(80f, 100f, grid))
        assertEquals(3, resizable.maximumSpan(80f, 100f, grid).width)
    }
}
