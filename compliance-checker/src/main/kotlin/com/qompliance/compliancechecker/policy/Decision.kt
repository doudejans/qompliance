package com.qompliance.compliancechecker.policy

import com.qompliance.util.exception.DecisionException
import com.fasterxml.jackson.annotation.JsonProperty

enum class Decision {
    @JsonProperty("allow") ALLOW,
    @JsonProperty("deny") DENY,
    @JsonProperty("nondeciding") NONDECIDING,
    @JsonProperty("indeterminate") INDETERMINATE;

    fun isOppositeDecision(other: Decision): Boolean {
        if (this == ALLOW && other == DENY) {
            return true
        }
        if (this == DENY && other == ALLOW) {
            return true
        }
        return false
    }

    fun getOppositeDecision(): Decision {
        return when (this) {
            ALLOW -> DENY
            DENY -> ALLOW
            NONDECIDING -> NONDECIDING
            INDETERMINATE -> INDETERMINATE
        }
    }

    companion object {
        fun fromString(str: String): Decision {
            return when (str.lowercase()) {
                "allow" -> ALLOW
                "deny" -> DENY
                "nondeciding" -> NONDECIDING
                "indeterminate" -> INDETERMINATE
                else -> throw DecisionException("Invalid option '$str' for decision")
            }
        }
    }
}
