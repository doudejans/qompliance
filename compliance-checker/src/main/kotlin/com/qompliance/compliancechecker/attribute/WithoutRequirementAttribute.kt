package com.qompliance.compliancechecker.attribute

import com.qompliance.compliancechecker.metadata.Tag
import com.qompliance.compliancechecker.sql.DataIdentifier
import com.qompliance.compliancechecker.sql.SqlProcessor
import com.qompliance.util.tree.defaults.DefaultTagTree


class WithoutRequirementAttribute(val tags: Collection<Tag>) : RequirementAttribute, SqlProcessingAttribute, TagReferenceAttributeType() {
    override val name = "without"

    override val allowableValues = tags.map { it.name }.toSet()
    override val allValues = DefaultTagTree.get().flatten().toSet()

    override fun evaluateRequirement(attrVals: Collection<String>, sqlProcessor: SqlProcessor): Set<String> {
        val evaluatedTagRefs = evaluateRequirement(attrVals)
        val idsToExclude = mutableSetOf<String>()

        for (tagRef in evaluatedTagRefs) {
            val tag = tags.find { it.name.lowercase() == tagRef.lowercase() }
            if (tag != null) {
                idsToExclude.addAll(tag.dataRefs.map { it.refId })
            }
//            else {
//                throw AttributeValidationException("Set of tags applicable on the input SQL does not contain tag '$tagRef': $tags")
//            }
        }

        // Now that we have the entire set of data ids that need to be excluded, we have to look at what data ids
        // are actually applicable on the SQL
        return filterApplicableIds(sqlProcessor, idsToExclude)
    }

    private fun filterApplicableIds(sqlProcessor: SqlProcessor, ids: Collection<String>): Set<String> {
        // Parse the data ids that we have to remove
        val idsToRemove = DataIdentifier.fromString(ids)
        return sqlProcessor.dataIds.intersect(idsToRemove).map { it.toString() }.toSet()
    }

    /**
     * For generating the final outcomes for a without requirement, we union all values.
     * Note that the evaluated requirements are lists of data identifiers that have to be excluded from the SQL.
     * Thus, all values that need to be excluded is just the union of that.
     */
    override fun generateFinalOutcomes(evaluatedRequirements: Collection<Collection<String>>): Set<String> {
        return evaluatedRequirements.flatten().toSet()
    }
}
