package com.qompliance.datamanager

import com.qompliance.datamanager.metadata.repository.DatastoreRepository
import com.qompliance.datamanager.metadata.repository.TagRepository
import com.qompliance.datamanager.policy.repository.PolicyRepository
import org.apache.logging.log4j.kotlin.logger
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    value=["datamanager.generatedata"],
    havingValue = "true",
    matchIfMissing = false)
class PreloadGeneratedData {
    val logger = logger()

    val config: Config = AnnotationConfigApplicationContext(Config::class.java).getBean(Config::class.java)

    @Bean
    fun initDb(datastoreRepository: DatastoreRepository, tagRepository: TagRepository, policyRepository: PolicyRepository): CommandLineRunner {
        val dg = DataGenerator(config.nPoliciesPerTag, config.nSchemas, config.nContextAttrs, config.nRequirementAttrs)
        val schemas = dg.generateSchemas()
        val tags = dg.generateTags(schemas)
        val policies = dg.generatePolicies(tags)

        return CommandLineRunner {
            logger.info("Preloading generated metadata and policies, this can take some time...")
            datastoreRepository.saveAll(schemas)
            tagRepository.saveAll(tags)
            policyRepository.saveAll(policies)
            logger.info("Finished preloading")
        }
    }
}
