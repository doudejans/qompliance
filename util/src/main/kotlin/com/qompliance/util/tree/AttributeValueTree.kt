package com.qompliance.util.tree

class AttributeValueTree<T>(val root: TreeNode<T>) {
    /**
     * Returns a list of the [searchValue] and its parents in this AttributeValueTree.
     * Returns an empty list if the [searchValue] could not be found in the tree.
     */
    fun search(searchValue: T): List<T> {
        val res = dfsWithParents(searchValue) ?: return listOf()
        return res.map { tn -> tn.value }
    }

    /**
     * Returns a list of the [searchValues] and their parents with the branches of the trees merged wherever applicable.
     * For example, [Advertising, Marketing, All] and [Marketing, All] will be merged into [Advertising, Marketing, All].
     * Returns an empty list if the [searchValues] could not be found in the tree.
     */
    fun search(searchValues: Collection<T>): List<List<T>> {
        val branches = mutableListOf<List<T>>()

        for (searchValue in searchValues) {
            val valueWithParents = dfsWithParents(searchValue)?.map { it.value } ?: return listOf() // Handle this differently because of multiple values

            var replacedExistingBranch = false
            val iterator = branches.listIterator()
            while (iterator.hasNext()) {
                val branch = iterator.next()
                val union = unionParentsIfOverlap(branch, valueWithParents)
                if (union != null) {
                    iterator.set(union)
                    replacedExistingBranch = true
                    break
                }
            }

            if (!replacedExistingBranch) valueWithParents.let { branches.add(it.toMutableList()) }
        }

        return branches
    }

    /**
     * Searches for the [searchValue] in the tree and returns a list of this and all children values.
     * Returns an empty list if the [searchValue] could not be found.
     */
    fun searchWithChildren(searchValue: T): List<T> {
        val node = dfs(searchValue) ?: return listOf()
        return flatten(node)
    }

    /**
     * Searches for each of the values in [searchValues] and returns a list of lists, where each list contains
     * the search value and its children.
     */
    fun searchWithChildren(searchValues: Collection<T>): List<List<T>> {
        return searchValues.map { searchWithChildren(it) }
    }

    /**
     * Returns the number of nodes in the tree.
     */
    fun size(node: TreeNode<T> = root): Int {
        if (node.children.size == 0) {
            return 1
        }
        var count = 0
        for (child in node.children) {
            count += size(child)
        }
        return count + 1
    }

    /**
     * Returns the list of attribute values which entirely contains the other list.
     * Note that only the 'head' of the list can be longer, the tail will always result in the root as the last parent.
     * Therefore it is enough to check if one list is entirely contained within the other to get a single longer branch.
     * Returns null if the lists do not overlap.
     */
    private fun unionParentsIfOverlap(first: List<T>, second: List<T>): List<T>? {
        if (first.containsAll(second)) {
            return first
        } else if (second.containsAll(first)) {
            return second
        }
        return null
    }

    /**
     * Returns a random value from the tree.
     */
    fun getRandomValue(): T {
        val limit = (1..size()).random()
        val res = traverseDfWithLimit(limit) ?: throw Exception("Traversal of tree somehow exceeded tree size")
        return res.value
    }

    /**
     * Returns all values in the tree as a list.
     */
    fun flatten(node: TreeNode<T> = root, list: List<T> = listOf()): List<T> {
        val res = list.toMutableList()
        res.add(node.value)
        if (node.children.isNullOrEmpty()) return res
        for (child in node.children) {
            res.addAll(flatten(child))
        }
        return res
    }

    /**
     * Returns a list of TreeNodes of the [searchValue] and its parents in the (sub)tree contained in [node] by
     * performing depth first search. Returns null if the [searchValue] could not be found.
     */
    private fun dfsWithParents(searchValue: T, node: TreeNode<T> = root, stack: List<TreeNode<T>> = listOf()): List<TreeNode<T>>? {
        val newStack = mutableListOf(node).apply { addAll(stack) }
        for (child in node.children) {
            val res = dfsWithParents(searchValue, child, newStack)
            if (res != null) return res
        }
        if (node.value == searchValue) {
            return newStack
        }
        return null
    }

    private fun dfs(searchValue: T, node: TreeNode<T> = root): TreeNode<T>? {
        if (node.value == searchValue) {
            return node
        }
        for (child in node.children) {
            val res = dfs(searchValue, child)
            if (res != null) return res
        }
        return null
    }

    /**
     * Traverses the tree in a DFS-like manner until the 'limit' number of steps has been reached and returns the value
     * at that node. Can for example be used to select a random node in the tree.
     */
    private fun traverseDfWithLimit(limit: Int, node: TreeNode<T> = root): TreeNode<T>? {
        if (limit <= 1) {
            return node
        }
        var newLimit = limit
        for (child in node.children) {
            if (newLimit - size(child) > 1) {
                newLimit -= size(child)
            } else {
                val res = traverseDfWithLimit(newLimit - 1, child)
                if (res != null) return res
            }
        }
        return null
    }
}