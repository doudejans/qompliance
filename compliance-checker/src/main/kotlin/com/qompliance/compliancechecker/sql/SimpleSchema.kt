package com.qompliance.compliancechecker.sql

import org.apache.calcite.schema.Table
import org.apache.calcite.schema.impl.AbstractSchema

class SimpleSchema constructor(val schemaName: String, private val tableMap: Map<String, Table>) :
    AbstractSchema() {

    public override fun getTableMap(): Map<String, Table> {
        return tableMap
    }

}
