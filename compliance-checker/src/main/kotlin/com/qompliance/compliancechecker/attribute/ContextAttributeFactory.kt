package com.qompliance.compliancechecker.attribute

import com.qompliance.compliancechecker.metadata.Tag
import com.qompliance.util.exception.AttributeValidationException

object ContextAttributeFactory : AttributeFactory {
    override fun getAttributeFromId(attrId: String, attrValues: List<String>): ContextAttribute {
        return when (attrId) {
            "purpose" -> PurposeAttribute(attrValues)
            "role" -> RoleAttribute(attrValues)
            "tag" -> TagAttribute(attrValues)
            "data-location" -> DataLocationAttribute(attrValues)
            "storage-classification" -> StorageClassificationAttribute()
            else -> throw AttributeValidationException("'$attrId' is not a valid attribute identifier")
        }
    }

    override fun getAttributeFromId(
        attrId: String,
        tags: Collection<Tag>
    ): Attribute {
        throw Exception("Method not applicable on context attributes")
    }
}
