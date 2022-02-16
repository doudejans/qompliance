package com.qompliance.compliancechecker

import com.qompliance.compliancechecker.attribute.AttributeProcessor
import com.qompliance.compliancechecker.dto.NegativeValidationResult
import com.qompliance.compliancechecker.dto.PositiveValidationResult
import com.qompliance.compliancechecker.dto.Validation
import com.qompliance.compliancechecker.dto.ValidationResult
import com.qompliance.compliancechecker.metadata.Datastore
import com.qompliance.compliancechecker.metadata.SchemaRequest
import com.qompliance.compliancechecker.metadata.TagRetriever
import com.qompliance.compliancechecker.policy.Decider
import com.qompliance.compliancechecker.policy.Decision
import com.qompliance.compliancechecker.policy.PolicyRetriever
import com.qompliance.compliancechecker.sql.DataIdentifier
import com.qompliance.compliancechecker.sql.DatastoreReferenceExtractor
import com.qompliance.compliancechecker.sql.SqlProcessor
import com.qompliance.util.exception.DecisionException
import org.apache.logging.log4j.kotlin.logger
import org.springframework.util.StopWatch

/**
 * Class for orchestrating the entire validation pipeline.
 */
class ComplianceValidationJob(val validation: Validation, val defaultDecision: String) {

    private val logger = logger()
    private val stopwatch = StopWatch()

    fun runJob(): ValidationResult {
        // Retrieve the managed datastores metadata and initialize them in the SqlProcessor
        // Ideally for performance reasons we shouldn't retrieve all schemas at once
        stopwatch.start("get datastores from db")
        val datastoreReferences = DatastoreReferenceExtractor().getDatastoreReferences(validation.sql)
        val managedDatastores = SchemaRequest.getDatastores(datastoreReferences)
        stopwatch.stop()

        // Parse the SQL from the request
        stopwatch.start("validate and parse sql")
        val sqlProcessor = SqlProcessor(validation.sql, managedDatastores)
        val parsedSqlNode = sqlProcessor.parsedSql
        logger.debug("Parsed SQL: ${sqlProcessor.sqlNodeToString(parsedSqlNode)}")

        // Validate the SQL using schema data
        val validatedSqlString = sqlProcessor.sqlNodeToString()
        logger.debug("Validated SQL: $validatedSqlString")
        stopwatch.stop()

        // Extract data ids from validated SQL
        stopwatch.start("extract data ids")
        val dataIds = sqlProcessor.dataIds
        logger.debug("Extracted data ids: $dataIds")
        stopwatch.stop()

        // Query dataIds with database to match tags
        stopwatch.start("get tags from db")
        val tags = TagRetriever.getTags(dataIds.map { it.toString() })
        stopwatch.stop()
        logger.debug("Applicable tags: $tags")

        // Query database with tags for policies
        stopwatch.start("get policies from db")
        val policies = PolicyRetriever.getPolicies(tags.map { it.name })
        logger.debug("Policies matched by tag: $policies")
        logger.info("Number of applicable policies based on tags: ${policies.size}")
        stopwatch.stop()

        // Retrieve datastore locations
        stopwatch.start("get additional metadata")
        val locations = getDatastoreLocationsFromSchema(managedDatastores, dataIds)
        val classifications = getDatastoreClassificationsFromSchema(managedDatastores, dataIds)
        stopwatch.stop()

        val derivedAttributes = mapOf(
            "tag" to tags.map { it.name },
            "data-location" to locations.toList(),
            "storage-classification" to classifications.toList()
        )

        // Create the final object for validation including the evaluated tags and datastore locations
        stopwatch.start("init attribute processor")
        val validationIncludingTags = Validation(validation.sql, validation.attributes + derivedAttributes)
        val attributeProcessor = AttributeProcessor(validationIncludingTags, Decision.fromString(defaultDecision))
        stopwatch.stop()

        // Match policies based on additional application defined attributes
        stopwatch.start("policy matching and conflict resolution")
        val applicablePolicies = attributeProcessor.matchPolicies(policies)
        stopwatch.stop()
        logger.debug("Applicable policies: $applicablePolicies")
        logger.info("Number of applicable policies after matching: ${applicablePolicies.size}")

        // Evaluate policy requirements and generate the final outcomes
        stopwatch.start("eval requirements")
        val evaluatedPolicies = attributeProcessor.evaluateRequirements(applicablePolicies, tags, sqlProcessor)
        val outcomes = attributeProcessor.generateFinalOutcomes(evaluatedPolicies)
        stopwatch.stop()

        stopwatch.start("final decision")
        // Make second/final decision
        val finalDecision = Decider.decide(evaluatedPolicies, outcomes, defaultDecision)
        logger.debug("Final decision: ${finalDecision.decision}, ${finalDecision.reason}")
        stopwatch.stop()
        logger.info(stopwatch.prettyPrint())

        // Return the validation result
        if (finalDecision.decision == Decision.ALLOW) {
            return PositiveValidationResult(finalDecision, validatedSqlString)
        } else if (finalDecision.decision == Decision.DENY || finalDecision.decision == Decision.INDETERMINATE) {
            return NegativeValidationResult(finalDecision, validatedSqlString)
        }

        throw DecisionException("Something went wrong with evaluating the final decision. Is the default decision set correctly?")
    }

    private fun getDatastoreLocationsFromSchema(schemas: Collection<Datastore>, dataIds: Collection<DataIdentifier>): Set<String> {
        val res = mutableSetOf<String>()
        for (dataId in dataIds) {
            val schemaMatch = schemas.find { s -> s.name == dataId.schema }
            if (schemaMatch != null) {
                schemaMatch.location?.let { res.add(it) }
            }
        }
        return res
    }

    private fun getDatastoreClassificationsFromSchema(schemas: Collection<Datastore>, dataIds: Collection<DataIdentifier>): Set<String> {
        val res = mutableSetOf<String>()
        for (dataId in dataIds) {
            val schemaMatch = schemas.find { s -> s.name == dataId.schema }
            if (schemaMatch != null) {
                val classifications = schemaMatch.storageClassifications?.map { it.name }
                if (classifications != null) res.addAll(classifications)
            }
        }
        return res
    }

}
