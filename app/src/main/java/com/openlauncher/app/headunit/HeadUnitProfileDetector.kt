package com.openlauncher.app.headunit

import android.content.Context
import android.content.pm.PackageManager

class HeadUnitProfileDetector(private val context: Context) {

    fun detect(): HeadUnitDetection {
        val pm = context.packageManager
        val allPackages = TS18_PACKAGES + SZCHOICEWAY_PACKAGES
        val presentPackages = allPackages.filterTo(mutableSetOf()) { isPackageInstalled(pm, it) }
        val classification = classifyHeadUnitProfile(presentPackages)
        val evidence = allPackages.map { packageName ->
            val present = packageName in presentPackages
            HeadUnitEvidence(
                id = packageName,
                isPresent = present,
                details = if (present) "Observed via PackageManager" else "Not installed or not visible"
            )
        }

        return HeadUnitDetection(classification.first, classification.second, evidence)
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        internal val TS18_PACKAGES = setOf(
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
        internal val SZCHOICEWAY_PACKAGES = setOf(
            "com.szchoiceway.radio",
            "com.szchoiceway.eventcenter"
        )
    }
}

internal fun classifyHeadUnitProfile(
    presentPackages: Set<String>
): Pair<HeadUnitProfile, DetectionConfidence> {
    val dofunCount = presentPackages.count { it == "com.dofun.variety" || it == "com.dofun.carsetting" }
    val coreCount = presentPackages.count {
        it == "com.tw.service" || it == "com.tw.core" ||
            it == "com.tw.coreservice" || it == "com.tw.carinfoservice"
    }
    val ts18Count = presentPackages.count { it in HeadUnitProfileDetector.TS18_PACKAGES }
    val szchoicewayCount = presentPackages.count { it in HeadUnitProfileDetector.SZCHOICEWAY_PACKAGES }

    return when {
        dofunCount > 0 && coreCount >= 2 && ts18Count >= 5 ->
            HeadUnitProfile.TopwayTs18Dofun to DetectionConfidence.HIGH
        dofunCount > 0 && ts18Count >= 3 ->
            HeadUnitProfile.TopwayTs18Dofun to DetectionConfidence.MEDIUM
        szchoicewayCount == 2 -> HeadUnitProfile.Szchoiceway to DetectionConfidence.HIGH
        szchoicewayCount == 1 -> HeadUnitProfile.Szchoiceway to DetectionConfidence.MEDIUM
        ts18Count > 0 -> HeadUnitProfile.UnknownHeadUnit to DetectionConfidence.LOW
        else -> HeadUnitProfile.StandardAndroid to DetectionConfidence.NONE
    }
}
