package com.qompliance.compliancechecker

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:application.properties")
class Config {
    @Value("\${decision.default}")
    lateinit var defaultDecision: String

    @Value("\${datamanager.url}")
    lateinit var dataManagerUrl: String
}
