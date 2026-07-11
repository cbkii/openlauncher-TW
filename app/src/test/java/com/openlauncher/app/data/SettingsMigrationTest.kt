package com.openlauncher.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SettingsMigrationTest {
    @Test
    fun migratedLaunchTargetIds_areStableAndUniqueBySlot() {
        val shortcuts = listOf(
            ShortcutConfig(packageName = "example.player", label = "Player"),
            ShortcutConfig(packageName = "example.player", label = "Player duplicate")
        )

        val first = migrateShortcutsToLaunchTargets(shortcuts)
        val second = migrateShortcutsToLaunchTargets(shortcuts)

        assertEquals(first, second)
        assertNotEquals(first[0].id, first[1].id)
    }

    @Test
    fun unassignedShortcut_migratesWithoutAnEmptyPackageTarget() {
        val migrated = migrateShortcutsToLaunchTargets(
            listOf(ShortcutConfig(label = "Music", packageName = ""))
        ).single()

        assertNull(migrated.packageName)
    }
}
