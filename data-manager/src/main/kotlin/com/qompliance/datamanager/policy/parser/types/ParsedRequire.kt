package com.qompliance.datamanager.policy.parser.types

import com.fasterxml.jackson.annotation.JsonProperty

data class ParsedRequire (
    @JsonProperty("data-location") val dataLocation: List<String>?,
    @JsonProperty("storage-classification") val storageClassification: List<String>?,
    @JsonProperty("without") val without: List<String>?,
    @JsonProperty("aggregate") val aggregate: List<String>?
)
