package org.onast.example.quotaservice.api

import kotlinx.serialization.Serializable

@Serializable
data class ConsumeRequest(val units: Int)

@Serializable
data class ConsumeResponse(
    val accepted: Boolean,
    val remaining: Int,
    val resetAtMs: Long
)