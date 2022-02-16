package com.qompliance.datamanager.policy.parser

import com.qompliance.datamanager.policy.parser.types.ParsedContext
import com.qompliance.datamanager.policy.parser.types.ParsedPolicy
import com.qompliance.datamanager.policy.parser.types.ParsedRequire
import com.qompliance.util.entity.policy.ContextCondition
import com.qompliance.util.entity.policy.Policy
import com.qompliance.util.entity.policy.Requirement
import com.qompliance.util.exception.PolicyValidationException
import org.apache.logging.log4j.kotlin.logger

object PolicyConverter {
    private val logger = logger()

    /**
     * Converts a ParsedPolicy (which resembles the YAML structure of a policy) to its database com.qompliance.entity counterpart.
     */
    fun convertToPolicyEntity(parsedPolicy: ParsedPolicy): Policy {
        val policy = Policy()
        policy.id = parsedPolicy.id
        policy.name = parsedPolicy.name
        policy.owner = "unknown"
        policy.decision = parsedPolicy.decision

        parsedPolicy.context.tag?.let {
            policy.contextConditions.addAll(it.map { tag -> convertToCondition(policy, "tag", tag) })
        }

        parsedPolicy.context.role?.let {
            policy.contextConditions.addAll(it.map { role -> convertToCondition(policy, "role", role) })
        }

        parsedPolicy.context.purpose?.let {
            policy.contextConditions.addAll(it.map { purpose -> convertToCondition(policy, "purpose", purpose) })
        }

        parsedPolicy.context.dataLocation?.let {
            policy.contextConditions.addAll(it.map { dataLocation -> convertToCondition(policy, "data-location", dataLocation) })
        }

        parsedPolicy.context.storageClassification?.let {
            policy.contextConditions.addAll(it.map { storageClassification -> convertToCondition(policy, "storage-classification", storageClassification) })
        }

        parsedPolicy.require?.dataLocation?.let {
            policy.requirements.addAll(it.map { dataLocation -> convertToRequirement(policy, "data-location", dataLocation) })
        }

        parsedPolicy.require?.storageClassification?.let {
            policy.requirements.addAll(it.map { storageClassification -> convertToRequirement(policy, "storage-classification", storageClassification) })
        }

        parsedPolicy.require?.without?.let {
            policy.requirements.addAll(it.map { without -> convertToRequirement(policy, "without", without) })
        }

        parsedPolicy.require?.aggregate?.let {
            policy.requirements.addAll(it.map { aggregate -> convertToRequirement(policy, "aggregate", aggregate) })
        }

        return policy
    }

    private fun convertToCondition(policy: Policy, attrId: String, attrVal: String): ContextCondition {
        val condition = ContextCondition()
        condition.policy = policy
        condition.attrId = attrId
        condition.attrVal = attrVal
        return condition
    }

    private fun convertToRequirement(policy: Policy, attrId: String, attrVal: String): Requirement {
        val requirement = Requirement()
        requirement.policy = policy
        requirement.attrId = attrId
        requirement.attrVal = attrVal
        return requirement
    }

    fun convertToParsedPolicy(policy: Policy): ParsedPolicy {
        val parsed = ParsedPolicy(
            id = policy.id,
            name = policy.name ?: "", // Handle issues here more elegantly
            decision = policy.decision ?: "nondeciding",
            context = convertToParsedContext(policy.contextConditions),
            require = convertToParsedRequire(policy.requirements)
        )
        if (parsed.require != null && (parsed.decision != null && parsed.decision.lowercase() == "deny")) {
            throw PolicyValidationException("Requirements can only be specified for allow and non-deciding policies")
        }

        return parsed
    }

    fun convertToParsedPolicies(policies: Iterable<Policy>): List<ParsedPolicy> {
        val res = mutableListOf<ParsedPolicy>()
        for (policy in policies) {
            res.add(convertToParsedPolicy(policy))
        }
        return res
    }

    private fun convertToParsedContext(contextConditions: Collection<ContextCondition>): ParsedContext {
        val attributes = mutableMapOf<String, MutableList<String>>()

        for (contextCondition in contextConditions) {
            val attr = attributes.getOrPut(contextCondition.attrId!!) { mutableListOf() }
            attr.add(contextCondition.attrVal!!)
        }

        return ParsedContext(
            tag = attributes["tag"],
            role = attributes["role"],
            purpose = attributes["purpose"],
            dataLocation = attributes["data-location"],
            storageClassification = attributes["storage-classification"]
        )
    }

    private fun convertToParsedRequire(requirements: Collection<Requirement>): ParsedRequire? {
        if (requirements.isEmpty()) return null
        val attributes = mutableMapOf<String, MutableList<String>>()

        for (requirement in requirements) {
            val attr = attributes.getOrPut(requirement.attrId!!) { mutableListOf() }
            attr.add(requirement.attrVal!!)
        }
        return ParsedRequire(
            dataLocation = attributes["data-location"],
            storageClassification = attributes["storage-classification"],
            without = attributes["without"],
            aggregate = attributes["aggregate"]
        )
    }
}
