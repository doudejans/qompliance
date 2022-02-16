package com.qompliance.util.tree

class TreeNode<T>(val value: T) {
    val children: MutableList<TreeNode<T>> = mutableListOf()

    init {
        if (value == null) throw NullPointerException("The value of a tree node cannot be null")
    }

    fun add(child: TreeNode<T>) {
        children.add(child)
    }

    fun add(child: List<TreeNode<T>>) {
        children.addAll(child)
    }

    override fun toString(): String {
        return value.toString()
    }
}