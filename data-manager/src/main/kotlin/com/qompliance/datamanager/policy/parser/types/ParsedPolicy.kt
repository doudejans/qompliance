package com.qompliance.datamanager.policy.parser.types

data class ParsedPolicy(
    val id: Long?,
    val name: String,
    val context: ParsedContext,
    val decision: String?,
    val require: ParsedRequire?
)
