package com.qompliance.compliancechecker.dto

import com.qompliance.compliancechecker.policy.DecisionResult

data class PositiveValidationResult(override val result: DecisionResult, val validatedSql: String) :
    ValidationResult()
