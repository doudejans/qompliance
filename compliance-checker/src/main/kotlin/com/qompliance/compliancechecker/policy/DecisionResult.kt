package com.qompliance.compliancechecker.policy

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class DecisionResult(val decision: Decision,
                          val acceptablePolicies: Collection<EvaluatedPolicy>,
                          val declinedPolicies: Collection<EvaluatedPolicy>,
                          val reason: String? = null,
                          val outcomes: Map<String, Set<String>>? = null) {

    val validatedAt = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)

}