package com.qompliance.compliancechecker.policy

import com.qompliance.compliancechecker.attribute.RequirementAttributeFactory
import com.qompliance.util.exception.DecisionException

object Decider {

    /**
     * Decides about whether the set of policies allows or denies a data movement/transformation. This assumes all
     * processing has been done, meaning that all policies have their correct respective decisions. The decision is made
     * based on the default decision: if the default decision is allow, a single deny will deny the whole request. If
     * the default decision is deny, a single allow will allow the whole request. Indeterminate policies will always
     * result in a denied request and nondeciding policies will not influence the final decision.
     */
    fun decide(policies: Collection<EvaluatedPolicy>, outcomes: Map<String, Set<String>>, defaultDecision: String): DecisionResult {
        val default = Decision.fromString(defaultDecision)
        if (default != Decision.ALLOW && default != Decision.DENY) throw DecisionException("Default decision should be either allow or deny")

        val allowed = mutableListOf<EvaluatedPolicy>()
        val denied = mutableListOf<EvaluatedPolicy>()
        val nondeciders = mutableListOf<EvaluatedPolicy>()
        val indeterminates = mutableListOf<EvaluatedPolicy>()

        for (policy in policies) {
            // If the evaluated decision has not already been set, we can simply set it to the policy's decision
            if (policy.evaluatedDecision == null) {
                policy.evaluatedDecision = policy.decision
            }

            when (policy.evaluatedDecision) {
                Decision.ALLOW -> {
                    policy.reason = policy.reason ?: "Policy is applicable and can be enforced and allows the request"
                    allowed.add(policy)
                }
                Decision.NONDECIDING -> {
                    policy.reason = policy.reason ?: "Policy is applicable and can be enforced but does not influence final decision"
                    nondeciders.add(policy)
                }
                Decision.DENY -> {
                    policy.reason = policy.reason ?: "Policy is applicable and denies the request"
                    denied.add(policy)
                }
                Decision.INDETERMINATE -> {
                    policy.reason = policy.reason ?: "Policy is applicable but cannot be evaluated"
                    indeterminates.add(policy)
                }
                else -> throw DecisionException("Invalid decision enum value")
            }
        }

        // First, handle the case of an indeterminate policy
        if (indeterminates.isNotEmpty()) {
            return DecisionResult(Decision.INDETERMINATE, allowed.union(nondeciders), denied.union(indeterminates), "One or more policies cannot be evaluated")
        }

        val safeOutcomes = outcomes.filter { (k, v) -> !(RequirementAttributeFactory.blockingAttrIds.contains(k) && v.isEmpty()) }

        if (safeOutcomes.filterValues { it.isEmpty() }.isNotEmpty()) {
            return DecisionResult(Decision.INDETERMINATE, allowed.union(nondeciders), denied.union(indeterminates), "For one or more requirements there is not an outcome solution possible", safeOutcomes)
        }

        // Second, check if there are any blocking outcomes (such as the need to rewrite the query), also return indeterminate because no decision can be made yet
        val blocking = RequirementAttributeFactory.blockingAttrIds.intersect(safeOutcomes.keys)
        if (blocking.isNotEmpty()) {
            return DecisionResult(Decision.INDETERMINATE, allowed.union(nondeciders), denied.union(indeterminates), "One or more policy outcomes require changes before a decision can be made. Blocking attributes: $blocking", safeOutcomes)
        }

        // Then, check the case of no deciding policies
        if (allowed.isEmpty() && denied.isEmpty() && indeterminates.isEmpty()) {
            if (default == Decision.ALLOW) {
                return DecisionResult(default, nondeciders, listOf(), "No deciding policies, default decision allow was used", safeOutcomes)
            }
            return DecisionResult(default, nondeciders, listOf(), "No deciding policies, default decision deny was used")
        }

        if (default == Decision.ALLOW) {
            // Any deny will deny the request
            return if (denied.isNotEmpty()) {
                DecisionResult(Decision.DENY, allowed.union(nondeciders), denied.union(indeterminates), "One or more policies deny this request")
            } else {
                DecisionResult(Decision.ALLOW, allowed.union(nondeciders), denied.union(indeterminates), "All policies allow this request", safeOutcomes)
            }
        } else if (default == Decision.DENY) {
            // Any allow will allow the request
            return if (allowed.isNotEmpty()) {
                DecisionResult(Decision.ALLOW, allowed.union(nondeciders), denied.union(indeterminates), "One or more policies allow this request", safeOutcomes)
            } else {
                DecisionResult(Decision.DENY, allowed.union(nondeciders), denied.union(indeterminates), "All policies deny this request")
            }
        }

        throw DecisionException("A decision wasn't made which shouldn't be able to happen")
    }

}