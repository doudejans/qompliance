package com.qompliance.compliancechecker.policy

abstract class Policy(
    val id: Long,
    val name: String,
    val owner: String,
    val decision: Decision,
    val contextConditions: List<ContextCondition>,
    val requirements: List<Requirement>
)
