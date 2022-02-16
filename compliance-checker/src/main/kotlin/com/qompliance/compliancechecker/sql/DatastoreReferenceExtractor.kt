package com.qompliance.compliancechecker.sql

import org.apache.calcite.sql.*
import org.apache.calcite.sql.parser.SqlParser
import org.apache.logging.log4j.kotlin.logger

class DatastoreReferenceExtractor {
    private val logger = logger()

    fun getDatastoreReferences(sql: String): Set<String> {
        val parser = SqlParser.create(sql)
        val node = parser.parseStmt()
        return extractReferences(node)
    }

    private fun extractReferences(input: SqlNode): Set<String> {
        val res = mutableSetOf<String>()

        val node = if (input.kind.equals(SqlKind.ORDER_BY)) {
            ((input as SqlOrderBy).query as SqlSelect).from!!
        } else {
            (input as SqlSelect).from!!
        }

        if (node.kind == SqlKind.IDENTIFIER) {
            val id = node as SqlIdentifier
            res.add(id.toString())
        }

        if (node.kind == SqlKind.AS) {
            val basicCall = node as SqlBasicCall
            res.add(basicCall.operand<SqlNode>(0).toString())
        }

        if (node.kind == SqlKind.JOIN) {
            val join = node as SqlJoin

            if (join.left.kind == SqlKind.AS) {
                val basicCall = join.left as SqlBasicCall
                res.add(basicCall.operand<SqlNode>(0).toString())
            } else if (join.left.kind == SqlKind.IDENTIFIER) {
                val id = join.left as SqlIdentifier
                res.add(id.toString())
            } else {
                var left = join.left as SqlJoin

                while (left.left.kind != SqlKind.AS || left.left.kind != SqlKind.IDENTIFIER) {
                    if (left.left.kind == SqlKind.AS) {
                        val basicCall = left.right as SqlBasicCall
                        res.add(basicCall.operand<SqlNode>(0).toString())
                    }
                    if (left.left.kind == SqlKind.IDENTIFIER) {
                        val id = left.right as SqlIdentifier
                        res.add(id.toString())
                    }
                    left = left.left as SqlJoin
                }

                if (left.left.kind == SqlKind.IDENTIFIER) {
                    val id = left.left as SqlIdentifier
                    res.add(id.toString())
                } else if (left.left.kind == SqlKind.AS) {
                    val basicCall = left.left as SqlBasicCall
                    res.add(basicCall.operand<SqlNode>(0).toString())
                }

                if (left.right.kind == SqlKind.IDENTIFIER) {
                    val id = left.right as SqlIdentifier
                    res.add(id.toString())
                } else if (left.right.kind == SqlKind.AS) {
                    val basicCall = left.right as SqlBasicCall
                    res.add(basicCall.operand<SqlNode>(0).toString())
                }
            }

            if (join.right.kind == SqlKind.IDENTIFIER) {
                val id = join.right as SqlIdentifier
                res.add(id.toString())
            } else if (join.right.kind == SqlKind.AS) {
                val basicCall = join.right as SqlBasicCall
                res.add(basicCall.operand<SqlNode>(0).toString())
            }
        }

        return res
    }

}