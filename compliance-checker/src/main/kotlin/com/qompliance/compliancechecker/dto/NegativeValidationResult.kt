package com.qompliance.compliancechecker.dto

import com.qompliance.compliancechecker.policy.DecisionResult

data class NegativeValidationResult(override val result: DecisionResult, val validatedSql: String) :
    ValidationResult()
