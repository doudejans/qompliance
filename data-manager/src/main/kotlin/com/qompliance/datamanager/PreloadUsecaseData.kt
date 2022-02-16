package com.qompliance.datamanager

import com.qompliance.datamanager.metadata.repository.DatastoreRepository
import com.qompliance.datamanager.metadata.repository.TagRepository
import com.qompliance.datamanager.policy.parser.PolicyConverter
import com.qompliance.datamanager.policy.parser.PolicyParser
import com.qompliance.datamanager.policy.parser.types.ParsedPolicy
import com.qompliance.datamanager.policy.repository.PolicyRepository
import com.qompliance.util.entity.metadata.schema.Column
import com.qompliance.util.entity.metadata.schema.Dataset
import com.qompliance.util.entity.metadata.schema.Datastore
import com.qompliance.util.entity.metadata.tag.DataRef
import com.qompliance.util.entity.metadata.tag.Tag
import org.apache.logging.log4j.kotlin.logger
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

@Configuration
@ConditionalOnProperty(
    value=["datamanager.usecasedata"],
    havingValue = "true",
    matchIfMissing = false)
class PreloadUsecaseData {
    val logger = logger()

    @Bean
    fun initPoliciesFromFile(policyRepository: PolicyRepository): CommandLineRunner {
        val resolver = PathMatchingResourcePatternResolver()
        val resources = resolver.getResources("classpath:use-case-policies/*.yaml")

        val parsedPolicies = mutableListOf<ParsedPolicy>()

        if (resources.isNotEmpty()) {
            for (resource in resources) {
                val policy = PolicyParser.parse(resource.inputStream)
                parsedPolicies.add(policy)
            }
        } else {
            logger.warn("Failed to read policy directory")
        }

        val policyEntities = parsedPolicies.map { p -> PolicyConverter.convertToPolicyEntity(p) }

        return CommandLineRunner {
            policyRepository.saveAll(policyEntities)
            logger.info("Preloaded use case policies")
        }
    }

    @Bean
    fun initMetadata(datastoreRepository: DatastoreRepository, tagRepository: TagRepository): CommandLineRunner {
        return CommandLineRunner {
            datastoreRepository.saveAll(generateDatastores())
            logger.info("Preloaded use case schema data")
            tagRepository.saveAll(generateTags())
            logger.info("Preloaded use case tag data")
            logger.info("Finished preloading")
        }
    }

    private fun generateDatastores(): Set<Datastore> {
        val nl = Datastore()
        nl.name = "qars-nl"
        nl.location = "Netherlands"

        val us = Datastore()
        us.name = "qars-us"
        us.location = "United States"

        // Cars table
        val carCols = listOf("vin", "year", "model", "configuration")
        nl.datasets.add(generateDataset("cars", nl, carCols))
        us.datasets.add(generateDataset("cars", us, carCols))

        // Customers table
        val custCols = listOf("id", "name", "email", "car_vin")
        nl.datasets.add(generateDataset("customers", nl, custCols))
        us.datasets.add(generateDataset("customers", us, custCols))

        // Car vitals table
        val vitalsCols = listOf("id", "vin", "datetime", "sensor", "value")
        nl.datasets.add(generateDataset("car_vitals", nl, vitalsCols))
        us.datasets.add(generateDataset("car_vitals", us, vitalsCols))

        // Car events table
        val eventCols = listOf("id", "vin", "datetime", "message", "part")
        nl.datasets.add(generateDataset("car_events", nl, eventCols))
        us.datasets.add(generateDataset("car_events", us, eventCols))

        return setOf(nl, us)
    }

    private fun generateDataset(name: String, datastore: Datastore, columnNames: List<String>): Dataset {
        val ds = Dataset()
        ds.name = name
        ds.columns = generateColumns(columnNames, ds)
        ds.datastore = datastore
        return ds
    }

    private fun generateColumns(columnNames: Collection<String>, dataset: Dataset): MutableSet<Column> {
        val res = mutableSetOf<Column>()
        for (name in columnNames) {
            val c = Column()
            c.name = name
            c.dataset = dataset
            c.type = "VARCHAR"
            res.add(c)
        }
        return res
    }

    private fun generateTags(): Set<Tag> {
        return setOf(
            generateTag("PII", setOf("qars-nl.customers", "qars-us.customers")),
            generateTag("VIN", setOf(
                "qars-nl.cars.vin", "qars-us.cars.vin",
                "qars-nl.car_vitals.vin", "qars-us.car_vitals.vin",
                "qars-nl.car_events.vin", "qars-us.car_events.vin")),
            generateTag("Car Data", setOf(
                "qars-nl.cars", "qars-us.cars",
                "qars-nl.car_vitals", "qars-us.car_vitals",
                "qars-nl.car_events", "qars-us.car_events"))
        )
    }

    private fun generateTag(name: String, refs: Collection<String>): Tag {
        val t = Tag()
        t.name = name
        t.dataRefs = mutableSetOf()

        for (ref in refs) {
            val r = DataRef()
            r.refId = ref
            r.tags = mutableSetOf(t)
            t.dataRefs.add(r)
        }

        return t
    }
}