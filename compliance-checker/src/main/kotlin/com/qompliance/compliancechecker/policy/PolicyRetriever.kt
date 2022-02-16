package com.qompliance.compliancechecker.policy

import com.qompliance.compliancechecker.Config
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.logging.log4j.kotlin.logger
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.ParameterizedTypeReference
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

object PolicyRetriever {

    val logger = logger()
    private val config: Config = AnnotationConfigApplicationContext(Config::class.java).getBean(Config::class.java)

    data class PolicyResp(val policies: List<InputPolicy>)

    fun getPolicies(tags: Collection<String>): List<InputPolicy> {
        val baseUrl = config.dataManagerUrl + "/policies"
        val urlTemplate = UriComponentsBuilder.fromHttpUrl(baseUrl).queryParam("tags", tags.joinToString(",")).build().toUri()
        logger.info(urlTemplate)
        val responseEntity = restTemplate().exchange(
            urlTemplate, HttpMethod.GET, null,
            object : ParameterizedTypeReference<PolicyResp>() {}
        )
        val resp: PolicyResp? = responseEntity.body
        resp?.let { logger.debug(it) }

        return resp!!.policies
    }

    private fun restTemplate(): RestTemplate {
        val mapper = ObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.registerModule(Jackson2HalModule())
        mapper.registerModule(KotlinModule())
        val converter = MappingJackson2HttpMessageConverter()
        converter.supportedMediaTypes = MediaType.parseMediaTypes("application/json")
        converter.objectMapper = mapper
        return RestTemplate(listOf(converter))
    }

}