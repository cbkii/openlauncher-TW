package com.openlauncher.app.headunit

import com.openlauncher.app.headunit.topway.TopwayTs18LaunchTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchTargetResolverTest {
    @Test
    fun ts18Preset_excludesStockTwRadioAndMusic() {
        val packages = TopwayTs18LaunchTargets.preset.mapNotNull { it.packageName }

        assertFalse("com.tw.radio" in packages)
        assertFalse("com.tw.music" in packages)
    }

    @Test
    fun genericMusicPreset_doesNotPretendAnUnassignedPackageExists() {
        val music = TopwayTs18LaunchTargets.preset.single { it.id == "music" }

        assertNull(music.packageName)
    }

    @Test
    fun effectiveProfile_prefersManualOverride() {
        assertEquals(
            HeadUnitProfile.StandardAndroid,
            effectiveHeadUnitProfile(
                detected = HeadUnitProfile.TopwayTs18Dofun,
                override = HeadUnitProfile.StandardAndroid
            )
        )
    }

    @Test
    fun requiredProfile_blocksTs18TargetOnStandardAndroid() {
        val target = TopwayTs18LaunchTargets.preset.first()

        assertFalse(target.supportsProfile(HeadUnitProfile.StandardAndroid))
        assertTrue(target.supportsProfile(HeadUnitProfile.TopwayTs18Dofun))
    }
}
