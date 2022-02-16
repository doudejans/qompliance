package com.qompliance.util.tree.defaults

import com.qompliance.util.tree.AttributeValueTree
import com.qompliance.util.tree.TreeNode

object DefaultTagTree {
    private val root = TreeNode("All")

    private val classifications = TreeNode("AllClassifications")
    private val pii = TreeNode("PII")
    private val sensitive = TreeNode("Sensitive")

    private val types = TreeNode("AllTypes")
    private val email = TreeNode("Email")

    private val address = TreeNode("Address")
    private val country = TreeNode("Country")

    private val qars = TreeNode("Qars")
    private val cardata = TreeNode("Car Data")
    private val vin = TreeNode("VIN")

    init {
        root.add(listOf(classifications, types))
        classifications.add(listOf(pii, sensitive))
        types.add(listOf(email, address))
        address.add(country)

        root.add(qars)
        qars.add(vin)
        qars.add(cardata)
    }

    fun get(): AttributeValueTree<String> {
        return AttributeValueTree(root)
    }
}