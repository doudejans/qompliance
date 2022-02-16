package com.qompliance.compliancechecker.policy

class EvaluatedPolicy(
    id: Long,
    name: String,
    owner: String,
    decision: Decision,
    contextConditions: List<EvaluatedContextCondition>,
    requirements: List<EvaluatedRequirement>,
    var outcomes: Map<String, Set<String>>?,
    var evaluatedDecision: Decision?,
    var reason: String?
) : Policy(id, name, owner, decision, contextConditions, requirements) {
    constructor(policy: Policy, outcomes: Map<String, Set<String>>) : this(
        policy.id,
        policy.name,
        policy.owner,
        policy.decision,
        policy.contextConditions.map { EvaluatedContextCondition(it) },
        policy.requirements.map { EvaluatedRequirement(it) },
        outcomes,
        null,
        null
    )
    constructor(policy: Policy, outcomes: Map<String, Set<String>>?, evaluatedDecision: Decision?, reason: String?) : this(
        policy.id,
        policy.name,
        policy.owner,
        policy.decision,
        policy.contextConditions.map { EvaluatedContextCondition(it) },
        policy.requirements.map { EvaluatedRequirement(it) },
        outcomes,
        evaluatedDecision,
        reason
    )
}
