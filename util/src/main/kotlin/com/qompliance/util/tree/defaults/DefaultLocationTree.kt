package com.qompliance.util.tree.defaults

import com.qompliance.util.tree.AttributeValueTree
import com.qompliance.util.tree.TreeNode

object DefaultLocationTree {
    private val root = TreeNode("All")

    private val europe = TreeNode("EU")
    private val nl = TreeNode("Netherlands")
    private val de = TreeNode("Germany")
    private val be = TreeNode("Belgium")

    private val northAmerica = TreeNode("North America")
    private val us = TreeNode("United States")
    private val ca = TreeNode("Canada")

    init {
        root.add(europe)
        root.add(northAmerica)

        europe.add(listOf(nl, de, be))
        northAmerica.add(listOf(us, ca))
    }

    fun get(): AttributeValueTree<String> {
        return AttributeValueTree(root)
    }
}