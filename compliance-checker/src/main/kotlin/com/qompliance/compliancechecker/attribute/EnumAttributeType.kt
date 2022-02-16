package com.qompliance.compliancechecker.attribute

import com.qompliance.util.exception.AttributeValidationException
import org.apache.logging.log4j.kotlin.logger

abstract class EnumAttributeType : AttributeType {
    abstract val allowableValues: Set<String>
    abstract val allValues: Set<String>

    fun evaluate(attrVal: String): Boolean {
        return allowableValues.contains(attrVal.lowercase())
    }

    fun evaluate(attrVal: Collection<String>): Boolean {
        // Note that this evaluates individual policy attribute values in an OR fashion
        for (attr in attrVal) {
            if (allowableValues.contains(attr.lowercase())) return true
        }
        return false
    }

    fun checkConflicts(first: Collection<String>, second: Collection<String>): Int {
        val firstMatches = allowableValues.intersect(first.map { it.lowercase() })
        val secondMatches = allowableValues.intersect(second.map { it.lowercase() })

        val firstMatchCount = firstMatches.size
        val secondMatchCount = secondMatches.size

        val res = when {
            firstMatchCount == secondMatchCount && firstMatchCount > 0 -> 0
            firstMatchCount == 0 && secondMatchCount == 0 -> -1
            firstMatchCount > secondMatchCount -> 1
            secondMatchCount > firstMatchCount -> 2
            else -> -1
        }

        logger().debug("Conflict evaluation: $first, $second, $firstMatchCount, $secondMatchCount, $res")
        return res
    }

    /**
     * For enum type attributes, we just have to evaluate whether the requirement value exists in our system.
     *
     * Note that we do not take the context into account in this method. However, you may want to write a requirement
     * where the values are only valid within the context (e.g. in allAllowableValues). In that case, override
     * this method for your particular attribute to check this.
     */
    fun evaluateRequirement(attrVals: Collection<String>): Set<String> {
        for (attrVal in attrVals) {
            if (!allValues.contains(attrVal.lowercase())) {
                throw AttributeValidationException("Attribute value is not a valid option: $attrVal")
            }
        }
        return attrVals.toSet()
    }

    /**
     * For generating the final outcomes for an enum type attribute, we union all values.
     */
    fun generateFinalOutcomes(evaluatedRequirements: Collection<Collection<String>>): Set<String> {
        return evaluatedRequirements.flatten().toSet()
    }

}
