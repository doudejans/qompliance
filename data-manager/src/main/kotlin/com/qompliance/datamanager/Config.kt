package com.qompliance.datamanager

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:application.properties")
class Config(
    @Value("\${datamanager.generatedata.nschemas}")
    val nSchemas: Int,

    @Value("\${datamanager.generatedata.npoliciespertag}")
    val nPoliciesPerTag: Int,

    @Value("\${datamanager.generatedata.ncontextattrs}")
    val nContextAttrs: Int,

    @Value("\${datamanager.generatedata.nrequirementattrs}")
    val nRequirementAttrs: Int
)
