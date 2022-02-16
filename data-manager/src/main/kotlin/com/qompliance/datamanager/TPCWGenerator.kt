package com.qompliance.datamanager

import com.qompliance.datamanager.metadata.repository.DatastoreRepository
import com.qompliance.datamanager.metadata.repository.TagRepository
import com.qompliance.datamanager.policy.repository.PolicyRepository
import com.qompliance.util.entity.metadata.classification.StorageClassification
import com.qompliance.util.entity.metadata.schema.Dataset
import com.qompliance.util.entity.metadata.schema.Datastore
import com.qompliance.util.enum.defaults.DefaultStorageClassifications
import com.qompliance.util.tree.defaults.DefaultLocationTree
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.logger
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource

@Configuration
@ConditionalOnProperty(
    value=["datamanager.tpcwdata"],
    havingValue = "true",
    matchIfMissing = false)
class TPCWGenerator {
    val logger = logger()

    val config: Config = AnnotationConfigApplicationContext(Config::class.java).getBean(Config::class.java)

    @Bean
    fun initTPCWTablesFromFile(datastoreRepository: DatastoreRepository, tagRepository: TagRepository, policyRepository: PolicyRepository): CommandLineRunner {
        logger.info("Preloading policies on TPC-W schemas")
        val resource = ClassPathResource("tpcw.json").inputStream
        val bytes = resource.readAllBytes()

        val datastoreClassifications = DefaultStorageClassifications.valuesAsSetOfStrings().map { n ->
            val dc = StorageClassification()
            dc.name = n
            dc
        }

        val datastores = mutableListOf<Datastore>()

        for (i in 0 until config.nSchemas) {
            val datastore = Datastore()
            datastore.name = "TPCW${i}"
            datastore.location = DefaultLocationTree.get().getRandomValue()
            datastore.storageClassifications = datastoreClassifications.shuffled().take((1..2).random()).toMutableSet()
            val mapper = ObjectMapper()
            val datasets: List<Dataset> = mapper.readValue(bytes, object : TypeReference<List<Dataset>>() {})
            datastore.datasets.addAll(datasets.map{
                it.datastore = datastore
                it
            })
            datastores.add(datastore)
        }

        // nSchemas is ignored
        val dg = DataGenerator(config.nPoliciesPerTag, config.nSchemas, config.nContextAttrs, config.nRequirementAttrs)

        val tags = dg.generateTags(datastores)
        val policies = dg.generatePolicies(tags)

        return CommandLineRunner {
            datastoreRepository.saveAll(datastores)
            tagRepository.saveAll(tags)
            policyRepository.saveAll(policies)
            logger.info("Finished preloading")
        }
    }
}