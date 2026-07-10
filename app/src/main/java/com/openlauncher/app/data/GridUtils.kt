package com.openlauncher.app.data

object GridUtils {
    const val MAX_CELLS = 64

    init {
        require(GRID_COLS * GRID_ROWS <= MAX_CELLS) {
            "Grid size exceeds 64 cells, cannot be represented by a Long bitmask."
        }
    }

    /**
     * Builds a Long bitmask representing the occupied cells of the given widgets.
     */
    fun buildOccupiedMask(widgets: List<WidgetConfig>): Long {
        var mask = 0L
        for (w in widgets) {
            if (!w.enabled) continue
            mask = mask or getWidgetMask(w)
        }
        return mask
    }

    /**
     * Returns a Long bitmask for a single widget, bounded to the grid.
     */
    fun getWidgetMask(w: WidgetConfig): Long {
        var mask = 0L
        for (dy in 0 until w.spanY) {
            val y = w.gridY + dy
            if (y !in 0 until GRID_ROWS) continue
            for (dx in 0 until w.spanX) {
                val x = w.gridX + dx
                if (x !in 0 until GRID_COLS) continue
                mask = mask or (1L shl (y * GRID_COLS + x))
            }
        }
        return mask
    }

    fun getSpanMask(col: Int, row: Int, spanX: Int, spanY: Int): Long {
        var mask = 0L
        for (dy in 0 until spanY) {
            val y = row + dy
            if (y !in 0 until GRID_ROWS) continue
            for (dx in 0 until spanX) {
                val x = col + dx
                if (x !in 0 until GRID_COLS) continue
                mask = mask or (1L shl (y * GRID_COLS + x))
            }
        }
        return mask
    }

    /**
     * Returns the first free grid position (col to row) that can fit a widget with the given spans.
     */
    fun firstFreeGridPos(spanX: Int, spanY: Int, occupiedMask: Long): Pair<Int, Int>? {
        for (row in 0..GRID_ROWS - spanY) {
            for (col in 0..GRID_COLS - spanX) {
                val spanMask = getSpanMask(col, row, spanX, spanY)
                if ((occupiedMask and spanMask) == 0L) {
                    return col to row
                }
            }
        }
        return null
    }

    /**
     * Returns a full grid mask.
     */
    fun getFullGridMask(): Long {
        val cells = GRID_COLS * GRID_ROWS
        if (cells >= 64) {
            return -1L
        }
        return (1L shl cells) - 1L
    }
}
