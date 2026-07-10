package com.openlauncher.app.headunit

data class HeadUnitEvidence(
    val id: String,
    val isPresent: Boolean,
    val details: String? = null
)

enum class DetectionConfidence { NONE, LOW, MEDIUM, HIGH }

data class HeadUnitDetection(
    val profile: HeadUnitProfile,
    val confidence: DetectionConfidence,
    val evidence: List<HeadUnitEvidence>
)
