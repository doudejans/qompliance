package com.qompliance.compliancechecker.sql

import org.apache.calcite.rel.type.*
import org.apache.calcite.schema.impl.AbstractTable
import org.apache.calcite.sql.type.SqlTypeName

class SimpleTable constructor(
    private val tableName: String,
    private val fieldNames: List<String>,
    private val fieldTypes: List<SqlTypeName>
) : AbstractTable() {

    private var rowType: RelDataType? = null

    override fun getRowType(typeFactory: RelDataTypeFactory): RelDataType {
        if (rowType == null) {
            val fields: MutableList<RelDataTypeField> = ArrayList(
                fieldNames.size
            )
            for (i in fieldNames.indices) {
                val fieldType = typeFactory.createSqlType(fieldTypes[i])
                val field: RelDataTypeField = RelDataTypeFieldImpl(fieldNames[i], i, fieldType)
                fields.add(field)
            }
            rowType = RelRecordType(StructKind.PEEK_FIELDS, fields, false)
        }
        return rowType as RelDataType
    }

}
