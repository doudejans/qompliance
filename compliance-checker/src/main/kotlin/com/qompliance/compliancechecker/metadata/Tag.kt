package com.qompliance.compliancechecker.metadata

data class Tag(val id: Long, val name: String, val dataRefs: Set<DataRef>)
