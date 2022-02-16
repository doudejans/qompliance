package com.qompliance.compliancechecker.attribute

import com.qompliance.util.enum.defaults.DefaultStorageClassifications

class StorageClassificationAttribute : ContextAttribute, EnumAttributeType() {
    override val name = "storage-classification"

    override val allowableValues: Set<String> = DefaultStorageClassifications.valuesAsSetOfStrings()
    override val allValues: Set<String> = DefaultStorageClassifications.valuesAsSetOfStrings()
}
