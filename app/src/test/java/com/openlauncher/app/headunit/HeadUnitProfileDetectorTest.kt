package com.openlauncher.app.headunit

import org.junit.Test
import org.junit.Assert.assertEquals

class HeadUnitProfileDetectorTest {
    @Test
    fun testEnumValuesExist() {
        assertEquals("TopwayTs18Dofun", HeadUnitProfile.TopwayTs18Dofun.name)
        assertEquals("StandardAndroid", HeadUnitProfile.StandardAndroid.name)
        assertEquals("Szchoiceway", HeadUnitProfile.Szchoiceway.name)
    }
}
