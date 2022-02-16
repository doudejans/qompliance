package com.qompliance.compliancechecker.policy

class EvaluatedRequirement(id: Long, attrId: String, attrVal: String) : Requirement(id, attrId, attrVal) {
    constructor(r: Requirement) : this(r.id, r.attrId, r.attrVal)
}
