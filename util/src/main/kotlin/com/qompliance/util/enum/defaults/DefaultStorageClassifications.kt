package com.qompliance.util.enum.defaults

import com.qompliance.util.enum.EnumAttribute

enum class DefaultStorageClassifications : EnumAttribute {
    HIPAA, GDPR, GOV, FINANCIAL, SENSITIVE, DISKENCRYPTED;

    override fun toString(): String {
        return super.toString().lowercase()
    }

    companion object {
        fun valuesAsSetOfStrings(): Set<String> {
            return values().map { it.toString() }.toSet()
        }

        fun getRandomValue(): String {
            return values().random().toString()
        }
    }
}