package com.qompliance.compliancechecker.metadata

data class Datastore(val name: String, val location: String?, val type: String?, val datasets: Set<Dataset>, val storageClassifications: Set<StorageClassification>?)
