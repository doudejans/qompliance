package com.qompliance.compliancechecker.sql

import com.qompliance.compliancechecker.metadata.Datastore
import org.apache.calcite.avatica.util.Casing
import org.apache.calcite.config.CalciteConnectionConfigImpl
import org.apache.calcite.config.CalciteConnectionProperty
import org.apache.calcite.schema.SchemaPlus
import org.apache.calcite.sql.*
import org.apache.calcite.sql.parser.SqlParser
import org.apache.calcite.sql.pretty.SqlPrettyWriter
import org.apache.calcite.sql.type.SqlTypeName
import org.apache.calcite.sql.validate.SqlValidator
import org.apache.calcite.tools.FrameworkConfig
import org.apache.calcite.tools.Frameworks
import org.apache.calcite.tools.Planner
import org.apache.logging.log4j.kotlin.logger
import java.util.*

/**
 * Class for managing the processing of a SQL query.
 * On initialization, the SQL immediately gets parsed. Furthermore, this class contains functions to validate and
 * modify the input SQL.
 */
class SqlProcessor(val rawSql: String, val datastores: Collection<Datastore>) {

    private val logger = logger()

    private val configProperties = Properties()
    init {
        configProperties[CalciteConnectionProperty.CASE_SENSITIVE.camelName()] = "true"
        configProperties[CalciteConnectionProperty.UNQUOTED_CASING.camelName()] = Casing.UNCHANGED.toString()
        configProperties[CalciteConnectionProperty.QUOTED_CASING.camelName()] = Casing.UNCHANGED.toString()
    }

    private val config = CalciteConnectionConfigImpl(configProperties)
    private val parserConfig = SqlParser.config()
    private val validatorConfig = SqlValidator.Config.DEFAULT
        .withLenientOperatorLookup(config.lenientOperatorLookup())
        .withSqlConformance(config.conformance())
        .withDefaultNullCollation(config.defaultNullCollation())
        .withIdentifierExpansion(true)

    private val schemas = buildSchemas(datastores)
    val frameworkConfig: FrameworkConfig = Frameworks.newConfigBuilder()
        .parserConfig(parserConfig)
        .sqlValidatorConfig(validatorConfig)
        .defaultSchema(schemas)
        .build()
    
    val planner: Planner = Frameworks.getPlanner(frameworkConfig)

    val parsedSql: SqlNode = planner.parse(rawSql)

    val validatedSql: SqlNode = planner.validate(parsedSql)
    
//    val relRoot: RelRoot = planner.rel(validatedSql)

    val fromAliases = getFromAliasMap(validatedSql)

    // The entire expanded set of all data identifiers referenced in the query
    // E.g. DB0.customers.email, but also DB0.customers and DB0
    val dataIds: Set<DataIdentifier> = getDataIds(validatedSql)

    // Alias maps which map alias to identifier
    val tableAliasMap = getFromAliasMap(validatedSql)
    val columnAliasMap = getSelectAliasMap(validatedSql)

    fun sqlNodeToString(sqlNode: SqlNode = validatedSql): String {
        val writer: SqlWriter = SqlPrettyWriter()
        sqlNode.unparse(writer, 0, 0)
        return writer.toSqlString().sql
    }

    private fun getDataIds(sqlNode: SqlNode = validatedSql): Set<DataIdentifier> {
        val selectList = getSelectList(sqlNode)
        return matchDataIdentifiers(selectList, fromAliases)
    }

    private fun buildSchemas(datastores: Collection<Datastore>): SchemaPlus {
        val root = Frameworks.createRootSchema(false)

        for (datastore in datastores) {
            val tables = datastore.datasets.associate { ds ->
                ds.name to SimpleTable(
                    ds.name,
                    ds.columns.map { it.name },
                    ds.columns.map { SqlTypeName.get(it.type.uppercase()) ?: throw Exception("Unknown type ${it.type}") })
            }
            val schema = SimpleSchema(datastore.name, tables)
            root.add(datastore.name, schema)
        }

        return root
    }

    /**
     * Builds a list of the identifiers in the SELECT clause of the SQL. This assumes that the SqlNode already has been
     * validated so that we can get the fully qualified identifiers.
     */
    private fun getSelectList(input: SqlNode?): List<String> {
        val list = mutableListOf<String>()
        var node = input ?: return list

        node = if (node.kind.equals(SqlKind.ORDER_BY)) {
            ((node as SqlOrderBy).query as SqlSelect).selectList!!
        } else {
            (node as SqlSelect).selectList!!
        }

        for (item in node) {
            if (item.kind == SqlKind.AS || SqlKind.AGGREGATE.contains(item.kind)) {
                val basicCall = item as SqlBasicCall
                val identifier = basicCall.operand<SqlNode>(0).toString()
                list.add(identifier)
            } else if (item is SqlIdentifier) {
                list.add(item.toString())
            }
        }

        return list
    }

    private fun getSelectAliasMap(input: SqlNode?): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var node = input ?: return map

        node = if (node.kind.equals(SqlKind.ORDER_BY)) {
            ((node as SqlOrderBy).query as SqlSelect).selectList!!
        } else {
            (node as SqlSelect).selectList!!
        }

        for (item in node) {
            if (item.kind == SqlKind.AS) {
                val basicCall = item as SqlBasicCall
                map[basicCall.operand<SqlNode>(0).toString()] = basicCall.operand<SqlNode>(1).toString()
            }
        }

        return map
    }

    /**
     * Builds a map based on the FROM part of the SQL query by mapping the aliases assigned to the datasets to the
     * names of the datasets. If the input SqlNode has been validated, the value of the mapping should be the fully
     * qualified name of the table. This map can then be used to match column names with the fully qualified names
     * from the FROM clause of the query.
     *
     * E.g. Calcite validated SQL may look like:
     * `SELECT "customers"."email", "customers"."phone" FROM "mainDB"."customers" AS "customers"`.
     * The map resulting from this method will then be `{customers=mainDB.customers}`, which can be used to resolve the
     * aliases in the SELECT.
     */
    private fun getFromAliasMap(input: SqlNode?): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var node = input ?: return map

        node = if (node.kind.equals(SqlKind.ORDER_BY)) {
            ((node as SqlOrderBy).query as SqlSelect).from!!
        } else {
            (node as SqlSelect).from!!
        }

        // If only 1 dataset is in the query
        if (node.kind == SqlKind.AS) {
            val basicCall = node as SqlBasicCall
            map[basicCall.operand<SqlNode>(0).toString()] = basicCall.operand<SqlNode>(1).toString()
            return map
        }

        if (node.kind == SqlKind.JOIN) {
            val from = node as SqlJoin

            // If there are 2 datasets in the query
            if (from.left.kind == SqlKind.AS) {
                val basicCall = from.left as SqlBasicCall
                map[basicCall.operand<SqlNode>(0).toString()] = basicCall.operand<SqlNode>(1).toString()
            } else {
                // If there are more than 2 datasets in the query
                var left = from.left as SqlJoin

                // Traverse until we get an AS
                while (left.left.kind != SqlKind.AS) {
                    val basicCall = left.right as SqlBasicCall
                    map[basicCall.operand<SqlNode>(0).toString()] = basicCall.operand<SqlNode>(1).toString()
                    left = left.left as SqlJoin
                }

                val leftBasicCall = left.left as SqlBasicCall
                val rightBasicCall = left.right as SqlBasicCall

                map[leftBasicCall.operand<SqlNode>(0).toString()] = leftBasicCall.operand<SqlNode>(1).toString()
                map[rightBasicCall.operand<SqlNode>(0).toString()] = rightBasicCall.operand<SqlNode>(1).toString()
            }
            map[(from.right as SqlBasicCall).operand<SqlNode>(0).toString()] = (from.right as SqlBasicCall).operand<SqlNode>(1).toString()

            return map
        }

        return map
    }

    /**
     * Builds a list of DataIdentifiers which join together the schema, table and column for all columns/data addressed
     * in the query. This is done by resolving the aliases (introduced by the Calcite validation) in the FROM clause
     * stored in a map with the identifiers from the SELECT clause.
     * E.g. match `customers.email` with `customers -> mainDB.customers`
     */
    fun matchDataIdentifiers(selectItems: Collection<String>, fromAliases: Map<String, String>): Set<DataIdentifier> {
        // We'll need the reversed table alias map, since the input here is id -> alias, but we need to lookup the
        // aliases to get the full identifier. This should be safe because alias-id pairs should be unique.
        val reversedTableAliasMap = mutableMapOf<String, String>()
        for ((key, value) in fromAliases.entries) {
            reversedTableAliasMap[value] = key
        }

        val res = mutableSetOf<DataIdentifier>()
        for (item in selectItems) {
            val selectSplit = item.split('.')
            val from = reversedTableAliasMap[selectSplit[0]]
            val fromSplit = from!!.split('.') // We will assume that the query has already been validated and thus that we can resolve all of the aliases
            res.add(DataIdentifier(fromSplit[0], null, null))
            res.add(DataIdentifier(fromSplit[0], fromSplit[1], null))
            res.add(DataIdentifier(fromSplit[0], fromSplit[1], selectSplit[1]))
        }

        return res
    }

}
