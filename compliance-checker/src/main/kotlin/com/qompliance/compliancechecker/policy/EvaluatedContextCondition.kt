package com.qompliance.compliancechecker.policy

class EvaluatedContextCondition(id: Long, attrId: String, attrVal: String) : ContextCondition(id, attrId, attrVal) {
    constructor(cc: ContextCondition) : this(cc.id, cc.attrId, cc.attrVal)
}
