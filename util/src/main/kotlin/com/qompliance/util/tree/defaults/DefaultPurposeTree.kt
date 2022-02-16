package com.qompliance.util.tree.defaults

import com.qompliance.util.tree.AttributeValueTree
import com.qompliance.util.tree.TreeNode

object DefaultPurposeTree {
    private val root = TreeNode("All")

    private val marketing = TreeNode("Marketing")
    private val research = TreeNode("Research")
    private val productimprovement = TreeNode("Product Improvement")

    private val advertising = TreeNode("Advertising")
    private val audience = TreeNode("AudienceResearch")


    private val qars = TreeNode("Qars")
    private val accident = TreeNode("Accident Investigation")

    init {
        root.add(marketing)
        root.add(research)

        marketing.add(advertising)
        research.add(audience)
        research.add(productimprovement)

        root.add(qars)
        qars.add(accident)
    }

    fun get(): AttributeValueTree<String> {
        return AttributeValueTree(root)
    }
}