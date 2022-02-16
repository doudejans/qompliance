package com.qompliance.compliancechecker.attribute

import com.qompliance.compliancechecker.sql.SqlProcessor

sealed interface SqlProcessingAttribute : Attribute {
    // Interface because the attribute implementations themselves should determine how to use the SqlProcessor.
    // The implementation can then for example call TagReferenceAttributeType.evaluateRequirement with the attrVals
    // to evaluate the tags.
    fun evaluateRequirement(attrVals: Collection<String>, sqlProcessor: SqlProcessor): Set<String>
}