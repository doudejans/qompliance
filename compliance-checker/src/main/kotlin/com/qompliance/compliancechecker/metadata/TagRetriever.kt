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

object TagRetriever {

    val logger = logger()
    private val config: Config = AnnotationConfigApplicationContext(Config::class.java).getBean(Config::class.java)

    data class TagsResp(val tags: Set<Tag>)

    fun getTags(dataRefs: List<String>): Set<Tag> {
        val baseUrl = "${config.dataManagerUrl}/tags"
        val urlTemplate = UriComponentsBuilder.fromHttpUrl(baseUrl).queryParam("dataRefs", dataRefs.joinToString(separator = ",")).encode().toUriString()
        logger.info(urlTemplate)
        val responseEntity = restTemplate().exchange(
            urlTemplate, HttpMethod.GET, null,
            object : ParameterizedTypeReference<TagsResp>() {}
        )
        val resp: TagsResp? = responseEntity.body

        return resp!!.tags
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
