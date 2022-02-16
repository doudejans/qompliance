package com.qompliance.compliancechecker.attribute

import com.qompliance.util.exception.AttributeValidationException
import com.qompliance.util.tree.AttributeValueTree
import com.qompliance.util.tree.defaults.DefaultTagTree

class TagAttribute(attrValues: Collection<String>) : ContextAttribute, HierarchyAttributeType() {
    override val name = "tag"

    override val inputAttrBranches = mutableSetOf<List<String>>()
    override val allAllowableValues = mutableSetOf<String>()

    init {
        val searchRes = getTree().search(attrValues)
        if (searchRes.isEmpty()) {
            throw AttributeValidationException("'$attrValues' contains an invalid attribute in tree with root '${getTree().root.value}'")
        }
        inputAttrBranches.addAll(searchRes)
        allAllowableValues.addAll(inputAttrBranches.flatten())
    }

    override fun getTree(): AttributeValueTree<String> {
        return DefaultTagTree.get()
    }
}