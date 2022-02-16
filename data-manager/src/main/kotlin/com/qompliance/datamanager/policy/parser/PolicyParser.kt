package com.qompliance.datamanager.policy.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.qompliance.datamanager.policy.parser.types.ParsedPolicy
import java.io.InputStream

object PolicyParser {
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun parse(stream: InputStream): ParsedPolicy {
        return mapper.readValue(stream)
    }

    fun parse(string: String): ParsedPolicy {
        return mapper.readValue(string)
    }

    fun toYamlString(parsedPolicy: ParsedPolicy): String {
        return mapper.writeValueAsString(parsedPolicy)
    }

}