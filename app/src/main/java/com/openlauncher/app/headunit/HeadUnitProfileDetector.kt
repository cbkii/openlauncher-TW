package com.openlauncher.app.headunit

import android.content.Context
import android.content.pm.PackageManager

class HeadUnitProfileDetector(private val context: Context) {

    fun detect(): Pair<HeadUnitProfile, List<HeadUnitEvidence>> {
        val pm = context.packageManager
        val evidence = mutableListOf<HeadUnitEvidence>()

        var ts18Score = 0
        var szchoicewayScore = 0

        val ts18Packages = listOf(
            "com.dofun.variety",
            "com.dofun.carsetting",
            "com.tw.service",
            "com.tw.core",
            "com.tw.coreservice",
            "com.tw.carinfoservice",
            "com.tw.bt",
            "com.tw.eq",
            "com.tw.auxin",
            "com.tw.twfileexplore"
        )

        for (pkg in ts18Packages) {
            val present = isPackageInstalled(pm, pkg)
            evidence.add(HeadUnitEvidence(pkg, present))
            if (present) ts18Score++
        }

        val szPackages = listOf("com.szchoiceway.radio", "com.szchoiceway.eventcenter")
        for (pkg in szPackages) {
            val present = isPackageInstalled(pm, pkg)
            evidence.add(HeadUnitEvidence(pkg, present))
            if (present) szchoicewayScore++
        }

        val profile = when {
            ts18Score > 2 -> HeadUnitProfile.TopwayTs18Dofun
            szchoicewayScore > 0 -> HeadUnitProfile.Szchoiceway
            else -> HeadUnitProfile.StandardAndroid
        }

        return Pair(profile, evidence)
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
