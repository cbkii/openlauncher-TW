package com.openlauncher.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GridUtilsTest {
    @Test
    fun fullGridMask_coversEveryConfiguredCell() {
        val cells = GRID_COLS * GRID_ROWS\n        val expected = if (cells == Long.SIZE_BITS) -1L else (1L shl cells) - 1L\n\n        assertEquals(expected, GridUtils.getFullGridMask())
    }

    @Test
    fun widgetMask_ignoresOutOfBoundsCellsWithoutAliasing() {
        val partlyOutside = WidgetConfig("legacy", gridX = -1, gridY = 0, spanX = 2, spanY = 1)

        assertEquals(1L, GridUtils.getWidgetMask(partlyOutside))
    }

    @Test
    fun firstFreeGridPos_preservesRowMajorFirstFitForMultiCellSpan() {
        val occupied = GridUtils.getWidgetMask(
            WidgetConfig("occupied", gridX = 0, gridY = 0, spanX = 1, spanY = 1)
        )

        assertEquals(1 to 0, GridUtils.firstFreeGridPos(spanX = 2, spanY = 1, occupied))
    }

    @Test
    fun firstFreeGridPos_rejectsInvalidOrOversizedSpans() {
        assertNull(GridUtils.firstFreeGridPos(spanX = 0, spanY = 1, occupiedMask = 0L))
        assertNull(GridUtils.firstFreeGridPos(spanX = GRID_COLS + 1, spanY = 1, occupiedMask = 0L))
        assertNull(GridUtils.firstFreeGridPos(spanX = 1, spanY = GRID_ROWS + 1, occupiedMask = 0L))
    }

    @Test
    fun firstFreeGridPos_returnsNullWhenGridIsFull() {
        assertNull(
            GridUtils.firstFreeGridPos(
                spanX = 1,
                spanY = 1,
                occupiedMask = GridUtils.getFullGridMask()
            )
        )
    }
}
