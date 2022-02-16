package com.qompliance.compliancechecker.dto

import com.qompliance.compliancechecker.policy.DecisionResult

data class RewriteValidationResult(override val result: DecisionResult, val rewrittenSql: String) :
    ValidationResult()