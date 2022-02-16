package com.qompliance.compliancechecker.policy

class InputPolicy(
    id: Long,
    name: String,
    owner: String,
    decision: Decision?,
    contextConditions: List<InputContextCondition>,
    requirements: List<InputRequirement>
) : Policy(id, name, owner, decision ?: Decision.NONDECIDING, contextConditions, requirements)
