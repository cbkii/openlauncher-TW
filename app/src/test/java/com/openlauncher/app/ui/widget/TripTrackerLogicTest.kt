package com.openlauncher.app.ui.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class TripTrackerLogicTest {
    @Test
    fun testSimulationSpeedCalculation() {
        val speed = TripTrackerLogic.calculateSimulationSpeed(2.0f)
        // 2.0 * 2.0 * 2.8 + 2.0 * 8.0 = 4.0 * 2.8 + 16.0 = 11.2 + 16.0 = 27.2
        assertEquals(27.2f, speed, 0.001f)
    }
}
