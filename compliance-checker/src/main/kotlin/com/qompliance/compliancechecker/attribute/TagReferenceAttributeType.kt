package com.qompliance.compliancechecker.attribute

import com.qompliance.util.exception.AttributeValidationException

abstract class TagReferenceAttributeType : AttributeType {

    // The set of allowable values for a tag reference is just the set of tags that apply on the input query
    // We can't and shouldn't take the tag hierarchy into account here because 'children' or 'parents' may reference
    // other data in the query which will result in unexpected rewrites.
    // For example, if a policy applies on all data tagged PII, we may want to have a policy which leaves out email addresses.
    // Therefore it is also not necessary to check whether the tags are applicable under the context per se.
    abstract val allowableValues: Set<String>
    abstract val allValues: Set<String>

    /**
     * For tag reference type attributes, we just have to evaluate whether the tag value exists in our system.
     *
     * Note that we do not take the context into account in this method. However, you may want to write a requirement
     * where the values are only valid within the context (e.g. in allAllowableValues). In that case, override
     * this method for your particular attribute to check this.
     */
    fun evaluateRequirement(attrVals: Collection<String>): Set<String> {
        for (attrVal in attrVals) {
            if (!allValues.map { it.lowercase() }.contains(attrVal.lowercase())) {
                throw AttributeValidationException("Tag reference attribute value is not a valid option: $attrVal")
            }
        }
        return attrVals.toSet()
    }

}
