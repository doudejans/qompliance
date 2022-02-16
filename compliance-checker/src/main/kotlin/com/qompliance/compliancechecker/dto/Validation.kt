package com.qompliance.compliancechecker.dto

data class Validation(val sql: String, val attributes: Map<String, List<String>>)
