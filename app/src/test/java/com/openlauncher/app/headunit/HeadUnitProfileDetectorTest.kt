package com.openlauncher.app.headunit

import org.junit.Test
import org.junit.Assert.assertEquals

class HeadUnitProfileDetectorTest {
    @Test
    fun noEvidence_isStandardAndroidWithNoConfidence() {
        assertEquals(
            HeadUnitProfile.StandardAndroid to DetectionConfidence.NONE,
            classifyHeadUnitProfile(emptySet())
        )
    }

    @Test
    fun genericTwEvidence_isUnknownRatherThanAFalsePositive() {
        assertEquals(
            HeadUnitProfile.UnknownHeadUnit to DetectionConfidence.LOW,
            classifyHeadUnitProfile(setOf("com.tw.bt", "com.tw.eq"))
        )
    }

    @Test
    fun dofunAndCoreEvidence_isHighConfidenceTs18() {
        val present = setOf(
            "com.dofun.variety",
            "com.tw.service",
            "com.tw.core",
            "com.tw.coreservice",
            "com.tw.bt",
            "com.tw.eq"
        )

        assertEquals(
            HeadUnitProfile.TopwayTs18Dofun to DetectionConfidence.HIGH,
            classifyHeadUnitProfile(present)
        )
    }

    @Test
    fun szchoicewayEvidence_isKeptSeparateFromTs18() {
        assertEquals(
            HeadUnitProfile.Szchoiceway to DetectionConfidence.HIGH,
            classifyHeadUnitProfile(setOf("com.szchoiceway.radio", "com.szchoiceway.eventcenter"))
        )
    }
}
