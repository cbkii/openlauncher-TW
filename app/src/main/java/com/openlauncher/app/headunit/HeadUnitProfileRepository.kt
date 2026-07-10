package com.openlauncher.app.headunit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class HeadUnitProfileRepository {
    private val _detectedProfile = MutableStateFlow(HeadUnitProfile.StandardAndroid)
    val detectedProfile: StateFlow<HeadUnitProfile> = _detectedProfile.asStateFlow()

    private val _evidence = MutableStateFlow<List<HeadUnitEvidence>>(emptyList())
    val evidence: StateFlow<List<HeadUnitEvidence>> = _evidence.asStateFlow()

    private val _confidence = MutableStateFlow(DetectionConfidence.NONE)
    val confidence: StateFlow<DetectionConfidence> = _confidence.asStateFlow()

    private val _profileOverride = MutableStateFlow<HeadUnitProfile?>(null)
    val profileOverride: StateFlow<HeadUnitProfile?> = _profileOverride.asStateFlow()

    val effectiveProfile: Flow<HeadUnitProfile> = combine(
        detectedProfile,
        profileOverride
    ) { detected, override -> effectiveHeadUnitProfile(detected, override) }
        .distinctUntilChanged()

    fun updateDetection(detection: HeadUnitDetection) {
        _detectedProfile.value = detection.profile
        _confidence.value = detection.confidence
        _evidence.value = detection.evidence
    }

    fun setOverride(profile: HeadUnitProfile?) {
        _profileOverride.value = profile
    }

    fun getEffectiveProfile(): HeadUnitProfile {
        return effectiveHeadUnitProfile(_detectedProfile.value, _profileOverride.value)
    }
}

internal fun effectiveHeadUnitProfile(
    detected: HeadUnitProfile,
    override: HeadUnitProfile?
): HeadUnitProfile = override ?: detected
