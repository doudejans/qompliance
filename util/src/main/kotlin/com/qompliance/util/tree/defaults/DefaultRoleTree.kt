package com.qompliance.util.tree.defaults

import com.qompliance.util.tree.AttributeValueTree
import com.qompliance.util.tree.TreeNode

object DefaultRoleTree {
    private val root = TreeNode("All")

    private val marketing = TreeNode("MarketingDept")
    private val research = TreeNode("ResearchDept")

    private val qars = TreeNode("Qars")
    private val data = TreeNode("Data Scientist")
    private val engineer = TreeNode("Engineer")
    private val investigator = TreeNode("Investigator")

    init {
        root.add(marketing)
        root.add(research)

        root.add(qars)
        qars.add(data)
        qars.add(engineer)
        qars.add(investigator)
    }

    fun get(): AttributeValueTree<String> {
        return AttributeValueTree(root)
    }
}