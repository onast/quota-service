package org.onast.example.quotaservice.quota

sealed class QuotaDecision {
    data class Accepted(val remaining: Int, val resetAtMs: Long) : QuotaDecision()
    data class Rejected(val remaining: Int, val resetAtMs: Long) : QuotaDecision()
}