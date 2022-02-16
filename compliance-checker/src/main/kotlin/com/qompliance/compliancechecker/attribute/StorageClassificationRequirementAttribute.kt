package com.qompliance.compliancechecker.attribute

import com.qompliance.util.enum.defaults.DefaultStorageClassifications

class StorageClassificationRequirementAttribute : RequirementAttribute, EnumAttributeType() {
    override val name = "storage-classification"

    override val allowableValues: Set<String> = DefaultStorageClassifications.values().map { it.toString().lowercase() }.toSet()
    override val allValues: Set<String> = DefaultStorageClassifications.valuesAsSetOfStrings()
}
