package com.openlauncher.app.headunit

import org.junit.Test
import org.junit.Assert.assertNotNull

class LaunchTargetResolverTest {
    @Test
    fun testLaunchTargetCreation() {
        val target = LaunchTarget(
            id = "test",
            label = "Test",
            packageName = "com.test.pkg"
        )
        assertNotNull(target)
    }
}
