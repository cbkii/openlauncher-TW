package com.openlauncher.app.headunit.topway

import com.openlauncher.app.headunit.LaunchTarget
import com.openlauncher.app.headunit.HeadUnitProfile

object TopwayTs18LaunchTargets {

    val preset = listOf(
        LaunchTarget(
            id = "navradio",
            label = "Radio",
            packageName = "com.navimods.radio",
            iconHint = "RADIO",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "Preferred radio target. Excludes com.tw.radio."
        ),
        LaunchTarget(
            id = "music",
            label = "Music",
            // No default package for music to avoid com.tw.music
            iconHint = "MUSIC",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "Generic music target. User selected. Excludes com.tw.music."
        ),
        LaunchTarget(
            id = "bt",
            label = "Bluetooth",
            packageName = "com.tw.bt",
            iconHint = "BLUETOOTH",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "Stock BT/Phone UI."
        ),
        LaunchTarget(
            id = "eq",
            label = "EQ",
            packageName = "com.tw.eq",
            className = "com.tw.eq.EQActivity",
            iconHint = "EQUALIZER",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "Stock EQ controls."
        ),
        LaunchTarget(
            id = "dsp",
            label = "DSP",
            packageName = "com.tw.eq",
            className = "com.tw.eq.DSPActivity",
            iconHint = "EQUALIZER",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "Stock DSP controls."
        ),
        LaunchTarget(
            id = "aux",
            label = "AUX",
            packageName = "com.tw.auxin",
            className = "com.tw.auxin.AuxInActivity",
            iconHint = "VIDEOCAM",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "AUX video/audio input UI."
        ),
        LaunchTarget(
            id = "fileexplore",
            label = "Files",
            packageName = "com.tw.twfileexplore",
            iconHint = "GLOBE",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "Stock file manager."
        ),
        LaunchTarget(
            id = "carsetting",
            label = "Car Settings",
            packageName = "com.dofun.carsetting",
            className = "com.dofun.carsetting.ui.MainActivity",
            iconHint = "SETTINGS",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "DoFun car settings."
        ),
        LaunchTarget(
            id = "android_settings",
            label = "Android Settings",
            packageName = "com.android.settings",
            className = "com.android.settings.Settings",
            iconHint = "SETTINGS",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "Public Android settings fallback."
        ),
        LaunchTarget(
            id = "keypad_settings",
            label = "Steering Wheel Keys",
            packageName = "com.tw.keypad",
            className = "com.tw.keypad.SteeringWheelActivity",
            iconHint = "SETTINGS",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "Requires device validation; unavailable components remain visible in diagnostics."
        ),
        LaunchTarget(
            id = "dofun_setting",
            label = "DoFun Setting",
            uri = "launcher://variety/setting",
            iconHint = "SETTINGS",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "DoFun theme/settings URI."
        ),
        LaunchTarget(
            id = "dofun_theme_one",
            label = "DoFun Theme Category One",
            uri = "launcher://variety/theme/category/one",
            iconHint = "SETTINGS",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "Requires device validation; resolved before display/use."
        ),
        LaunchTarget(
            id = "dofun_theme_two",
            label = "DoFun Theme Category Two",
            uri = "launcher://variety/theme/category/two",
            iconHint = "SETTINGS",
            requiredProfile = HeadUnitProfile.TopwayTs18Dofun,
            notes = "Requires device validation; resolved before display/use."
        )
    )
}
