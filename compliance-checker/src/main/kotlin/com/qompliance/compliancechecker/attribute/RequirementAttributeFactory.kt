package com.qompliance.compliancechecker.attribute

import com.qompliance.compliancechecker.metadata.Tag
import com.qompliance.util.exception.AttributeValidationException

object RequirementAttributeFactory : AttributeFactory {
    val blockingAttrIds = setOf("without")

    override fun getAttributeFromId(attrId: String, attrValues: List<String>): Attribute {
        throw Exception("Method not applicable on requirements")
    }

    override fun getAttributeFromId(attrId: String, tags: Collection<Tag>): RequirementAttribute {
        return when (attrId) {
            "data-location" -> DataLocationRequirementAttribute()
            "storage-classification" -> StorageClassificationRequirementAttribute()
            "without" -> WithoutRequirementAttribute(tags)
            "aggregate" -> AggregateRequirementAttribute(tags)
            else -> throw AttributeValidationException("'$attrId' is not a valid attribute identifier")
        }
    }
}
