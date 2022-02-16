package com.qompliance.compliancechecker.sql

/**
 * Class for representing 'data identifiers'. A data identifier can consist of a schema/datastore name, a table name
 * and a column name. A fully qualified data identifier has all three. An example of a string representation looks like:
 * "DB2.customers.email". This class contains various methods for creating and converting data identifiers.
 * Note that because this is a data class, it automatically includes methods like equals() and hashCode() so that data
 * identifiers can be compared.
 */
data class DataIdentifier(var schema: String?, var table: String?, var column: String?) {

    fun isFullyQualified(): Boolean {
        return !schema.isNullOrBlank() && !table.isNullOrBlank() && !column.isNullOrBlank()
    }

    fun isDataStoreScope(): Boolean {
        return !schema.isNullOrBlank() && table.isNullOrBlank() && column.isNullOrBlank()
    }

    fun isTableScope(): Boolean {
        return !schema.isNullOrBlank() && !table.isNullOrBlank() && column.isNullOrBlank()
    }

    fun isColumnScope(): Boolean {
        return isFullyQualified()
    }

    fun asDataStoreScope(): DataIdentifier {
        if (!isDataStoreScope() && !isTableScope() && !isColumnScope()) {
            throw IllegalArgumentException("Cannot convert ${toString()} to datastore scope")
        }
        return DataIdentifier(schema, null, null)
    }

    fun asTableScope(): DataIdentifier {
        if (!isTableScope() && !isColumnScope()) {
            throw IllegalArgumentException("Cannot convert ${toString()} to table scope")
        }
        return DataIdentifier(schema, table, null)
    }

    override fun toString(): String {
        if (column == null) {
            if (table == null) {
                return "$schema"
            }
            return "$schema.$table"
        }
        return "$schema.$table.$column"
    }

    companion object {
        fun fromString(str: String): DataIdentifier {
            val split = str.split('.')
            return when (split.size) {
                1 -> DataIdentifier(split[0], null, null)
                2 -> DataIdentifier(split[0], split[1], null)
                3 -> DataIdentifier(split[0], split[1], split[2])
                else -> throw IllegalArgumentException("'$str' cannot be empty and can only contain 3 levels")
            }
        }

        fun fromString(str: Collection<String>): List<DataIdentifier> {
            return str.map { fromString(it) }
        }
    }

}
