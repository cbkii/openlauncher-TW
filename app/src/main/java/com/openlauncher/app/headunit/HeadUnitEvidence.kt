package com.openlauncher.app.headunit

data class HeadUnitEvidence(
    val id: String,
    val isPresent: Boolean,
    val details: String? = null
)
