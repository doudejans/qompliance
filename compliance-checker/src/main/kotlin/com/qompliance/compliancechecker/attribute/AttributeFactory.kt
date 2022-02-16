package com.qompliance.compliancechecker.attribute

import com.qompliance.compliancechecker.metadata.Tag

interface AttributeFactory {
    fun getAttributeFromId(attrId: String, attrValues: List<String>): Attribute
    fun getAttributeFromId(attrId: String, tags: Collection<Tag>): Attribute
}
