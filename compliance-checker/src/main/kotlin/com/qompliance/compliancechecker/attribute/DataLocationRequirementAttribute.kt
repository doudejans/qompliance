package com.qompliance.compliancechecker.attribute

import com.qompliance.util.tree.AttributeValueTree
import com.qompliance.util.tree.defaults.DefaultLocationTree

class DataLocationRequirementAttribute : RequirementAttribute, HierarchyAttributeType() {
    override val name = "data-location"

    override val inputAttrBranches = mutableSetOf<List<String>>()
    override val allAllowableValues = mutableSetOf<String>()

    init {
        // Just add all children because we don't restrict the allowable values for the data location requirement
        inputAttrBranches.addAll(listOf(getTree().searchWithChildren(getTree().root.value)))
        allAllowableValues.addAll(inputAttrBranches.flatten())
    }

    override fun getTree(): AttributeValueTree<String> {
        return DefaultLocationTree.get()
    }
}