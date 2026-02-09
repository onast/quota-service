package org.onast.example.quotaservice.quota

data class QuotaPolicy(
    val limitUnits: Int,
    val windowMs: Long
)