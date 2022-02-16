package com.qompliance.compliancechecker.attribute

import com.qompliance.util.exception.AttributeValidationException
import com.qompliance.util.tree.AttributeValueTree
import com.qompliance.util.tree.defaults.DefaultLocationTree

class DataLocationAttribute(inputAttrVals: Collection<String>) : ContextAttribute, HierarchyAttributeType() {
    override val name = "data-location"

    override val inputAttrBranches = mutableSetOf<List<String>>()
    override val allAllowableValues = mutableSetOf<String>()

    init {
        val searchRes = getTree().search(inputAttrVals)
        if (searchRes.isEmpty()) {
            throw AttributeValidationException("'$inputAttrVals' contains an invalid attribute in tree with root '${getTree().root.value}'")
        }
        inputAttrBranches.addAll(searchRes)
        allAllowableValues.addAll(inputAttrBranches.flatten())
    }

    override fun getTree(): AttributeValueTree<String> {
        return DefaultLocationTree.get()
    }
}