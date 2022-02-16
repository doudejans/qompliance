package com.qompliance.compliancechecker.attribute

import com.qompliance.compliancechecker.metadata.Tag
import com.qompliance.compliancechecker.sql.DataIdentifier
import com.qompliance.compliancechecker.sql.SqlProcessor
import com.qompliance.util.tree.defaults.DefaultTagTree
import org.apache.calcite.sql.*

class AggregateRequirementAttribute(val tags: Collection<Tag>) : RequirementAttribute, SqlProcessingAttribute, TagReferenceAttributeType() {
    override val name = "aggregate"

    override val allowableValues = tags.map { it.name }.toSet()
    override val allValues = DefaultTagTree.get().flatten().toSet()

    override fun evaluateRequirement(attrVals: Collection<String>, sqlProcessor: SqlProcessor): Set<String> {
        val evaluatedTagRefs = evaluateRequirement(attrVals)
        val idsToAggregate = mutableSetOf<String>()

        for (tagRef in evaluatedTagRefs) {
            val tag = tags.find { it.name.lowercase() == tagRef.lowercase() }
            if (tag != null) {
                idsToAggregate.addAll(tag.dataRefs.map { it.refId })
            }
        }

        return filterApplicableIds(sqlProcessor, idsToAggregate)
    }

    private fun filterApplicableIds(sqlProcessor: SqlProcessor, ids: Collection<String>): Set<String> {
        val idsToAggregate = DataIdentifier.fromString(ids)
        val allIdsInQuery = sqlProcessor.dataIds
        val aggregateIdsInQuery = extractAggregateIds(sqlProcessor.validatedSql)
        val resolvedAggregateIdsInQuery = sqlProcessor.matchDataIdentifiers(aggregateIdsInQuery, sqlProcessor.fromAliases)

        val idsInQueryToAggregate = allIdsInQuery.intersect(idsToAggregate)
        return idsInQueryToAggregate.minus(resolvedAggregateIdsInQuery).map { it.toString() }.toSet()
    }

    private fun extractAggregateIds(node: SqlNode): Set<String> {
        val res = mutableSetOf<String>()

        val select = if (node.kind.equals(SqlKind.ORDER_BY)) {
            ((node as SqlOrderBy).query as SqlSelect).selectList!!
        } else {
            (node as SqlSelect).selectList!!
        }

        for (item in select) {
            if (SqlKind.AGGREGATE.contains(item.kind)) {
                val basicCall = item as SqlBasicCall
                val identifier = basicCall.operand<SqlNode>(0).toString()
                res.add(identifier)
            }
        }

        return res
    }

    override fun generateFinalOutcomes(evaluatedRequirements: Collection<Collection<String>>): Set<String> {
        return evaluatedRequirements.flatten().toSet()
    }
}
