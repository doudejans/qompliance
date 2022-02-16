package com.qompliance.compliancechecker.metadata

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

object SchemaRequest {

    val logger = logger()
    private val config: Config = AnnotationConfigApplicationContext(Config::class.java).getBean(Config::class.java)

    data class DatastoresResp(val datastores: Set<Datastore>)

    fun getDatastores(fromRefs: Collection<String>): Set<Datastore> {
        val baseUrl = "${config.dataManagerUrl}/datastores"
        val urlTemplate = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParam("fromRefs", fromRefs.joinToString(separator = ","))
            .encode()
            .toUriString()
        logger.info(urlTemplate)
        val responseEntity = restTemplate().exchange(
            urlTemplate, HttpMethod.GET, null,
            object : ParameterizedTypeReference<DatastoresResp>() {}
        )
        val resp: DatastoresResp? = responseEntity.body

        return resp!!.datastores
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
