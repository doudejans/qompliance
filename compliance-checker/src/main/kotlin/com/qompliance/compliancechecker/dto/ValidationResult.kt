package com.qompliance.compliancechecker.dto

import com.qompliance.compliancechecker.Signature
import com.qompliance.compliancechecker.policy.DecisionResult

abstract class ValidationResult {
    abstract val result: DecisionResult
    val signature by lazy { Signature.generate(result) }
}
