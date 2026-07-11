package com.openlauncher.app.headunit

data class LaunchTarget(
    val id: String,
    val label: String,
    val packageName: String? = null,
    val className: String? = null,
    val action: String? = null,
    val uri: String? = null,
    val iconHint: String? = null,
    val requiredProfile: HeadUnitProfile? = null,
    val notes: String? = null
)

data class LaunchTargetStatus(
    val target: LaunchTarget,
    val isAvailable: Boolean
)

internal fun LaunchTarget.supportsProfile(profile: HeadUnitProfile): Boolean =
    requiredProfile == null || requiredProfile == profile
