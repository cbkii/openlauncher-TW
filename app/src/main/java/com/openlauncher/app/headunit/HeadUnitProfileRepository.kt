package com.openlauncher.app.headunit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HeadUnitProfileRepository(private val context: android.content.Context) {
    private val _detectedProfile = MutableStateFlow(HeadUnitProfile.StandardAndroid)
    val detectedProfile: StateFlow<HeadUnitProfile> = _detectedProfile.asStateFlow()

    private val _evidence = MutableStateFlow<List<HeadUnitEvidence>>(emptyList())
    val evidence: StateFlow<List<HeadUnitEvidence>> = _evidence.asStateFlow()

    private val _profileOverride = MutableStateFlow<HeadUnitProfile?>(null)
    val profileOverride: StateFlow<HeadUnitProfile?> = _profileOverride.asStateFlow()

    fun updateDetectedProfile(profile: HeadUnitProfile, evidenceList: List<HeadUnitEvidence>) {
        _detectedProfile.value = profile
        _evidence.value = evidenceList
    }

    fun setOverride(profile: HeadUnitProfile?) {
        _profileOverride.value = profile
    }

    fun getEffectiveProfile(): HeadUnitProfile {
        return _profileOverride.value ?: _detectedProfile.value
    }
}
