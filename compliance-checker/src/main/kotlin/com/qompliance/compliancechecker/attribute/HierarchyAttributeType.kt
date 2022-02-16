package com.qompliance.compliancechecker.attribute

import com.qompliance.util.exception.AttributeValidationException
import com.qompliance.util.tree.AttributeValueTree
import org.apache.logging.log4j.kotlin.logger

abstract class HierarchyAttributeType : AttributeType {
    private val logger = logger()

    // A set containing individual lists of attribute values for all input attributes
    // E.g. for input [Advertising,AudienceResearch] will give {[Advertising,Marketing,All],[AudienceResearch,Research,All]}
    // This allows us to check for conflicts across different input attributes which might occur in different subtrees
    abstract val inputAttrBranches: MutableSet<List<String>>
    // The flattened version of inputAttrBranches for easy lookups
    abstract val allAllowableValues: MutableSet<String>

    abstract fun getTree(): AttributeValueTree<String>

    fun evaluate(attrVal: String): Boolean {
        return allAllowableValues.contains(attrVal)
    }

    fun evaluate(attrVal: Collection<String>): Boolean {
        // Note that this evaluates individual policy attribute values in an OR fashion
        for (attr in attrVal) {
            if (allAllowableValues.contains(attr)) return true
        }
        return false
    }

    /**
     * The first and second input are the collections of attribute values for a particular attribute id.
     * Like the input, these can be multiple values and not all have to be applicable to this particular input.
     * Returns 0 if one is not more specific than the other, 1 if the first is more specific, 2 if the second is more specific.
     * Returns -1 in all other cases.
     */
    fun checkConflicts(first: Collection<String>, second: Collection<String>): Int {
        var firstMoreSpecificCount = 0
        var secondMoreSpecificCount = 0
        // Compare all attribute values
        for (branch in inputAttrBranches) {
            val firstAlreadyInBranch = false
            val secondAlreadyInBranch = false

            for (i in first.indices) {
                val firstIndex = branch.indexOf(first.elementAt(i))
                // If a particular attribute value has not been found, it is not a relevant policy attribute value for this input
                if (firstIndex == -1) continue

                for (j in second.indices) {
                    val secondIndex = branch.indexOf(second.elementAt(j))
                    if (secondIndex == -1) continue

                    // At this point, we know that both i and j are in the branch
                    // However, if we have already seen a value for either first or second in this branch, a policy has multiple values in the same branch
                    // which is illegal (otherwise they can cheat the system by adding many attributes from the same branch).
                    // NOTE: Normally this should be an exception but for testing purposes this is more forgiving.
                    if (firstAlreadyInBranch) logger.warn("The following set contains multiple values that cover the same branch: $firstAlreadyInBranch")
                    if (secondAlreadyInBranch) logger.warn("The following set contains multiple values that cover the same branch: $secondAlreadyInBranch")

                    when {
                        firstIndex == secondIndex -> {
                            firstMoreSpecificCount++
                            secondMoreSpecificCount++
                        }
                        firstIndex < secondIndex -> {
                            firstMoreSpecificCount++
                        }
                        firstIndex > secondIndex -> {
                            secondMoreSpecificCount++
                        }
                    }
                }
            }
        }
        logger.debug("Conflict evaluation: $first, $second, $firstMoreSpecificCount, $secondMoreSpecificCount, ${getConflictResult(firstMoreSpecificCount, secondMoreSpecificCount)}")
        return getConflictResult(firstMoreSpecificCount, secondMoreSpecificCount)
    }

    /**
     * Evaluates the requirements of a hierarchical attribute by looking up the value in the tree, and adding it
     * and all of its children to a (flattened) set. This gives all allowable values for that particular requirement.
     * E.g., if the requirement is to keep data within the EU, it can also stay in NL and other EU 'children'.
     *
     * Note that we do not take the context into account in this method. However, you may want to write a requirement
     * where the values are only valid within the context (e.g. in allAllowableValues). In that case, override
     * this method for your particular attribute to check this.
     */
    fun evaluateRequirement(attrVals: Collection<String>): Set<String> {
        val res = getTree().searchWithChildren(attrVals)
        for (evaluatedVals in res) {
            if (evaluatedVals.isEmpty()) throw AttributeValidationException("The following set of attribute values contains a value that does not exist: $attrVals")
        }
        return res.flatten().toSet()
    }

    /**
     * Generates the final set of outcomes that should be returned by intersecting the evaluated requirements from
     * the policies. We start with the set of all tree values.
     * Example: P1 requires EU, P2 requires NL or BE. Then evaluatedRequirements will be {{EU,NL,BE},{NL,BE}}
     * and thus the final outcomes will be the intersection of all these requirements, which is {NL,BE}.
     */
    fun generateFinalOutcomes(evaluatedRequirements: Collection<Collection<String>>): Set<String> {
        var outcomes = getTree().flatten().toSet() // Start with all values
        for (evaluatedRequirement in evaluatedRequirements) {
            outcomes = outcomes.intersect(evaluatedRequirement)
        }
        return outcomes
    }

    private fun getConflictResult(firstCount: Int, secondCount: Int): Int {
        return if (firstCount == secondCount && firstCount > 0) {
            0
        } else if (firstCount == 0 && secondCount == 0) {
            -1
        } else if (firstCount > secondCount) {
            1
        } else if (secondCount > firstCount) {
            2
        } else {
            -1
        }
    }
}