package com.qompliance.compliancechecker.attribute

sealed interface ContextAttribute : Attribute {
    fun evaluate(attrVal: String): Boolean
    fun evaluate(attrVal: Collection<String>): Boolean

    /**
     * Returns an integer representing the outcome of checking for conflicts.
     * 0: If [first] and [second] are just as specific.
     * 1: If [first] is more specific than [second].
     * 2: If [second] is more specific than [first].
     * -1: In all other cases, also if there is no conflict.
     */
    fun checkConflicts(first: Collection<String>, second: Collection<String>): Int
}
