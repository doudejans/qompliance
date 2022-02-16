package com.qompliance.datamanager.policy.parser.types

import com.fasterxml.jackson.annotation.JsonProperty

data class ParsedContext(
    val tag: List<String>?,
    val role: List<String>?,
    val purpose: List<String>?,
    @JsonProperty("data-location") val dataLocation: List<String>?,
    @JsonProperty("storage-classification") val storageClassification: List<String>?
)
