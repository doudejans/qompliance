package com.qompliance.compliancechecker.attribute

sealed interface RequirementAttribute : Attribute {
    fun evaluateRequirement(attrVals: Collection<String>): Set<String>
    fun generateFinalOutcomes(evaluatedRequirements: Collection<Collection<String>>): Set<String>
}
