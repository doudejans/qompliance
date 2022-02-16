package com.qompliance.compliancechecker.attribute

import com.qompliance.compliancechecker.dto.Validation
import com.qompliance.compliancechecker.metadata.Tag
import com.qompliance.compliancechecker.policy.Decision
import com.qompliance.compliancechecker.policy.EvaluatedPolicy
import com.qompliance.compliancechecker.policy.Policy
import com.qompliance.compliancechecker.sql.SqlProcessor
import com.qompliance.util.exception.AttributeValidationException
import com.qompliance.util.exception.PolicyValidationException
import org.apache.logging.log4j.kotlin.logger

/**
 * Class for processing input and policy attributes.
 * Constructed using a Validation object for the current job, along with the default decision used by the system.
 */
class AttributeProcessor(validation: Validation, val defaultDecision: Decision) {

    val logger = logger()

    /**
     * During initialization of an AttributeProcessor, we initialize ContextAttributes for all the attributes in the
     * input specified by the user, or enriched by the system from metadata.
     */
    val inputAttributes: Map<String, ContextAttribute> = validation.attributes
        .filterValues { attrVals -> attrVals.isNotEmpty() }
        .mapValues { (attrId, attrVals) -> ContextAttributeFactory.getAttributeFromId(attrId, attrVals) }

    /**
     * Function for matching policies with the input (with which the AttributeProcessor has been constructed
     * in [inputAttributes]). The function returns a set of policies which are applicable on the input and for which
     * any conflicts have been resolved.
     *
     * Note that this function assumes that the set of policies passed as parameter [policies] will already be filtered
     * based on the tags that apply on the input SQL since the policies have been retrieved from the database using
     * these tags. However, tags are still considered for conflict resolution.
     */
    fun matchPolicies(policies: Collection<Policy>): Set<Policy> {
        var applicablePolicies = policies.toList()

        // Remember that all context attributes that are included in a policy should be satisfied
        // Attributes that are left out of a policy are not considered
        // Therefore, filter the applicable policies to just the policies for which we have all the attributes in the request
        // Note that if policies are matched solely on tags (i.e. they do not specify additional attributes), they always apply
        applicablePolicies = applicablePolicies.filter { p ->
            var attrMap = p.contextConditions.groupBy({ it.attrId }, { it.attrVal })
            attrMap = attrMap.minus("tag") // Tags do not need to be evaluated here
            inputAttributes.keys.containsAll(attrMap.keys) &&
                    attrMap.all { (attrId, attrVals) ->
                        // The attribute types are responsible for dealing with multiple values and for determining
                        // what constitutes a match
                        inputAttributes[attrId]!!.evaluate(attrVals)
                    }
        }

        return checkForConflicts(applicablePolicies.toSet())
    }

    /**
     * Function for evaluating the requirements. Evaluating the requirements means two things:
     * 1. It checks whether the requirements can be fulfilled and are valid under the policy's context. If this is not
     * the case, the policy's decision will be set to 'indeterminate' and the policy will need to be checked.
     * 2. For each policy, it generates a set of outcomes that should be enforced based on the requirements.
     */
    fun evaluateRequirements(policies: Collection<Policy>, tags: Collection<Tag>, sqlProcessor: SqlProcessor): Set<EvaluatedPolicy> {
        val res = mutableSetOf<EvaluatedPolicy>()
        // For all applicable policies
        for (policy in policies) {
            // Just add the policy as an EvaluatedPolicy if there are no requirements to evaluate
            if (policy.requirements.isNullOrEmpty()) {
                res.add(EvaluatedPolicy(policy, null, null, null))
                continue
            } else if (!policy.requirements.isNullOrEmpty() &&
                policy.decision == Decision.DENY || policy.decision == Decision.INDETERMINATE) {
                // A deny policy cannot have requirements
                throw PolicyValidationException("Policies with decision 'deny' or 'indeterminate' cannot specify requirements.")
            } else {
                // Evaluate all of the requirements and add the EvaluatedPolicy along with its outcomes to the set
                val requirementMap = policy.requirements.groupBy({ it.attrId }, { it.attrVal })
                val evaluatedReqs = mutableMapOf<String, Set<String>>()

                var setIndeterminate = false
                var reason: String? = null

                // For all requirement attributes in the system
                for ((attrId, attrVals) in requirementMap) {
                    val attr = RequirementAttributeFactory.getAttributeFromId(attrId, tags)
                    try {
                        if (attr is SqlProcessingAttribute) {
                            val evaluated = attr.evaluateRequirement(attrVals, sqlProcessor)
                            if (evaluated.isNotEmpty()) evaluatedReqs[attrId] = evaluated
                        } else {
                            val evaluated = attr.evaluateRequirement(attrVals)
                            if (evaluated.isNotEmpty()) evaluatedReqs[attrId] = evaluated
                        }
                    } catch (e: AttributeValidationException) {
                        // In this case, the requirement could not be enforced under the current context
                        // This means that the policy decision should be set to indeterminate
                        reason = e.message
                        setIndeterminate = true
                    }
                }
                val evalPolicy = EvaluatedPolicy(policy, evaluatedReqs, if (setIndeterminate) Decision.INDETERMINATE else null, reason)
                res.add(evalPolicy)
            }
        }
        return res
    }

    /**
     * Based on the set of individually evaluated policies from [evaluateRequirements], this function compares the
     * final outcomes for the individual policies and combines them to generate the final set of outcomes as a map.
     * The final map maps attribute names to the set of allowable values.
     *
     * Depending on the attribute type, conflicts between the attribute values of all policies are also resolved.
     * The semantics of the outcomes and conflict resolution strategies differ per attribute type.
     *
     * Note that the value in the map can be empty if there are no solutions. You may want to set the final decision
     * to indeterminate if this is the case.
     */
    fun generateFinalOutcomes(policies: Collection<EvaluatedPolicy>): Map<String, Set<String>> {
        // Merge the sets of evaluated requirements for all policies to a map that maps the requirement ids to a
        // list of sets of evaluated requirements (thus the sets are from individual policies).
        val outcomeMap = policies
            .mapNotNull { it.outcomes }
            .flatMap { it.asSequence() }
            .groupBy({ it.key }, { it.value })

        val finalOutcomes = mutableMapOf<String, Set<String>>()

        for ((attrId, evaluatedRequirements) in outcomeMap) {
            val attr = RequirementAttributeFactory.getAttributeFromId(attrId, setOf())
            val final = attr.generateFinalOutcomes(evaluatedRequirements)
            finalOutcomes[attrId] = final
        }

        return finalOutcomes
    }

    /**
     * Checks for conflicts in the policy contexts by comparing all policy pairs in [policies].
     * This method caches the result of comparing a policy with the input
     * Conflicts occur when two policies make opposite decisions when both policies are applicable on the input.
     * This function assumes the [policies] all apply on the input.
     *
     * For each policy pair, for each attribute in the input, check for conflicts between the values of these two
     * policies. Attributes that do not occur in the input are not considered to be able to conflict. Attribute types
     * implement their own conflict resolution strategies, which are called per attribute. The result of this evaluation
     * is then added to a tally, which counts the amount of times either policy A or policy B takes precedence. The
     * policy with the highest count takes final precedence. If this count is equal, the policy which conforms to the
     * default decision is chosen as a last resort.
     */
    private fun checkForConflicts(policies: Collection<Policy>): Set<Policy> {
        val resolvedPolicies = policies.toMutableSet()
        val cacheMap = mutableMapOf<String, MutableMap<String, MutableList<String>>>()

        for (i in policies.indices) {
            val policyA = policies.elementAt(i)
            if (policyA.decision != Decision.ALLOW && policyA.decision != Decision.DENY) continue

            for (j in i + 1 until policies.size) {
                val policyB = policies.elementAt(j)

                // Only check for conflicts for policies that have a conflicting decision
                if (!policyA.decision.isOppositeDecision(policyB.decision)) {
                    continue
                }

                var policyAScore = 0
                var policyBScore = 0

                for ((inputAttrId, inputAttrVal) in inputAttributes) {
                    val cachedPoliciesForAttribute = cacheMap.getOrPut(inputAttrId) { mutableMapOf() }

                    // If there is a conflict, the tree values of the conflicting tree branch must be in the same list of results
                    val cachedAttrValsA = cachedPoliciesForAttribute.getOrPut(policyA.name) { mutableListOf() }
                    var attrValsA: List<String> = cachedAttrValsA
                    if (cachedAttrValsA.isEmpty()) {
                        attrValsA = policyA.contextConditions.filter { cc -> cc.attrId == inputAttrId }.map { cc -> cc.attrVal }
                        cacheMap[inputAttrId]!![policyA.name]!!.addAll(attrValsA)
                    }

                    val cachedAttrValsB = cachedPoliciesForAttribute.getOrPut(policyB.name) { mutableListOf() }
                    var attrValsB: List<String> = cachedAttrValsB
                    if (cachedAttrValsB.isEmpty()) {
                        attrValsB = policyB.contextConditions.filter { cc -> cc.attrId == inputAttrId }.map { cc -> cc.attrVal }
                        cacheMap[inputAttrId]!![policyB.name]!!.addAll(attrValsA)
                    }

                    if (attrValsA.isNotEmpty() && attrValsB.isEmpty()) {
                        policyAScore++
                    } else if (attrValsA.isEmpty() && attrValsB.isNotEmpty()) {
                        policyBScore++
                    } else if (attrValsA.isEmpty() && attrValsB.isEmpty()) {
                        continue
                    }

                    when (inputAttrVal.checkConflicts(attrValsA.toSet(), attrValsB.toSet())) {
                        0 -> logger.debug("Policies ${policyA.name} and ${policyB.name} have attribute values for '$inputAttrId' at the same hierarchy level which cannot be resolved: A: $attrValsA, B: $attrValsB")
                        1 -> policyAScore++
                        2 -> policyBScore++
                        else -> continue
                    }
                }

                when {
                    policyAScore == policyBScore -> {
                        var policyName = ""
                        if (policyA.decision == defaultDecision) {
                            resolvedPolicies.remove(policyB)
                            policyName = policyA.name
                        } else if (policyB.decision == defaultDecision) {
                            resolvedPolicies.remove(policyA)
                            policyName = policyB.name
                        }
                        logger.debug("Policies ${policyA.name} and ${policyB.name} have the same precedence scores [$policyAScore, $policyBScore] after conflict resolution, so final precedence can not be established. " +
                                "Picking policy '$policyName' because it conforms to default decision '$defaultDecision'.")
                    }
                    policyAScore > policyBScore -> resolvedPolicies.remove(policyB)
                    policyAScore < policyBScore -> resolvedPolicies.remove(policyA)
                }
            }
        }
        return resolvedPolicies
    }

}
