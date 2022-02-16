package com.qompliance.datamanager

import com.qompliance.util.entity.metadata.classification.StorageClassification
import com.qompliance.util.entity.metadata.schema.Column
import com.qompliance.util.entity.metadata.schema.Dataset
import com.qompliance.util.entity.metadata.schema.Datastore
import com.qompliance.util.entity.metadata.tag.DataRef
import com.qompliance.util.entity.metadata.tag.Tag
import com.qompliance.util.entity.policy.ContextCondition
import com.qompliance.util.entity.policy.Policy
import com.qompliance.util.entity.policy.Requirement
import com.qompliance.util.enum.defaults.DefaultStorageClassifications
import com.qompliance.util.tree.defaults.DefaultLocationTree
import com.qompliance.util.tree.defaults.DefaultPurposeTree
import com.qompliance.util.tree.defaults.DefaultRoleTree
import com.qompliance.util.tree.defaults.DefaultTagTree
import org.apache.logging.log4j.kotlin.logger

// nSchemas min value: 5
class DataGenerator(val nPoliciesPerTag: Int, var nSchemas: Int, val nContextAttrs: Int, val nRequirementAttrs: Int) {
    private val logger = logger()

    private val DATA_TYPES = listOf("VARCHAR")

    private val TABLE_NAME_PREFIXES = listOf("customer", "organization", "school", "employee", "company", "department")
    private val TABLE_NAME_SUFFIXES = listOf("info", "data", "address", "finance", "definition", "contract", "groups")
    private val TABLE_NAMES = TABLE_NAME_PREFIXES.flatMap { pref -> TABLE_NAME_SUFFIXES.map { suf -> pref + "_" + suf } }

    private val COLUMN_NAMES = listOf("name", "address", "city", "state", "country", "ref", "money", "amount", "reason", "location", "price")
    private val DATASTORE_TYPES = listOf("managed", "external")

    // Fixed variables
    private val N_TABLES = 20
    private val N_COLUMNS = 20

    init {
        if (nSchemas < 5) {
            nSchemas = 5
            logger.warn("Property nSchemas was < 5 but value 5 will be used instead because at least 5 schemas are required for experiments.")
        }
    }

    // Calculated variables
    private val nTags = DefaultTagTree.get().size()
    private val nSchemasPerTag = if (nSchemas < nTags) 1 else nSchemas.floorDiv(nTags)

    fun generateSchemas(): MutableSet<Datastore> {
        val schemas = mutableSetOf<Datastore>()
        val datastoreClassifications = DefaultStorageClassifications.valuesAsSetOfStrings().map { n ->
            val dc = StorageClassification()
            dc.name = n
            dc
        }

        for (iS in 0 until nSchemas) {
            val schemaName = "DB$iS"
            val store = Datastore()
            store.name = schemaName
            store.type = DATASTORE_TYPES.random()
            // maybe remove this randomness
            store.location = DefaultLocationTree.get().getRandomValue()

            store.datasets = generateDatasets(store)
            // maybe remove this randomness
            store.storageClassifications = datastoreClassifications.shuffled().take((1..2).random()).toMutableSet()

            schemas.add(store)
        }

        return schemas
    }

    fun generateDatasets(store: Datastore): MutableSet<Dataset> {
        val datasets = mutableSetOf<Dataset>()

        for (iT in 0 until N_TABLES) {
            val tableName = if (iT < TABLE_NAMES.size) {
                TABLE_NAMES.elementAt(iT)
            } else {
                // If we run out of 'normal' table names, just add some random ones
                TABLE_NAMES.random() + "_$iT"
            }

            val dataset = Dataset()
            dataset.name = tableName
            dataset.datastore = store

            dataset.columns = generateColumns(dataset)

            datasets.add(dataset)
        }

        return datasets
    }

    fun generateColumns(dataset: Dataset): MutableSet<Column> {
        val columns = mutableSetOf<Column>()
        for (iC in 0 until N_COLUMNS) {
            val columnName = if (iC < COLUMN_NAMES.size) {
                COLUMN_NAMES.elementAt(iC)
            } else {
                COLUMN_NAMES.random() + "_$iC"
            }

            val column = Column()
            column.name = columnName
            column.type = DATA_TYPES.random()
            column.dataset = dataset
            columns.add(column)
        }

        return columns
    }

    fun generateTags(schemas: Collection<Datastore>): MutableSet<Tag> {
        val tags = mutableSetOf<Tag>()
        val tagValues = DefaultTagTree.get().flatten().reversed()

        val finalNTags = if (nSchemas < nTags) nSchemas else nTags
        for (i in 0 until finalNTags) {
            val tag = Tag()
            tag.name = tagValues[i]

            for (j in 0 until nSchemasPerTag) {
                val dataRef = DataRef()
                val schema = schemas.elementAt(j + i * nSchemasPerTag)
                // For consistency we only generate tags on data stores
                dataRef.refId = schema.name
                dataRef.tags.add(tag)
                tag.dataRefs.add(dataRef)
            }

            tags.add(tag)
        }

        return tags
    }

    fun generatePolicies(tags: Collection<Tag>): MutableSet<Policy> {
        val policies = mutableSetOf<Policy>()

        for (t in tags) {
            for (i in 0 until nPoliciesPerTag) {
                val policy = Policy()
                policy.decision = listOf("allow", "deny", "nondeciding").random()
                policy.name = getRandomString(10)
                policy.owner = getRandomString(10)

                val ccs = mutableSetOf<ContextCondition>()
                val tagcc = ContextCondition()
                tagcc.attrId = "tag"
                tagcc.attrVal = t.name
                tagcc.policy = policy
                ccs.add(tagcc)
                // Should we guarantee a purpose and role so that the number of matches is also more consistent?

                for (nc in 0 until nContextAttrs) {
                    ccs.add(generateContextCondition(policy))
                }
                policy.contextConditions = ccs

                if (policy.decision == "allow" || policy.decision == "nondeciding") {
                    val reqs = mutableSetOf<Requirement>()
                    for (nr in 0 until nRequirementAttrs) {
                        reqs.add(generateRequirement(policy))
                    }
                    policy.requirements = reqs
                }

                policies.add(policy)
            }
        }

        return policies
    }

    fun generateContextCondition(policy: Policy): ContextCondition {
        val cc = ContextCondition()
        cc.attrId = listOf("purpose", "role", "data-location", "storage-classification").random()
        cc.attrVal = when (cc.attrId) {
            "purpose" -> DefaultPurposeTree.get().getRandomValue()
            "role" -> DefaultRoleTree.get().getRandomValue()
            "data-location" -> DefaultLocationTree.get().getRandomValue()
            "storage-classification" -> DefaultStorageClassifications.getRandomValue()
            else -> throw Exception("Somehow not a valid attrId")
        }
        cc.policy = policy
        return cc
    }

    fun generateRequirement(policy: Policy): Requirement {
        val r = Requirement()
        r.attrId = listOf("data-location", "storage-classification", "without", "aggregate").random()
        r.attrVal = when (r.attrId) {
            "data-location" -> DefaultLocationTree.get().getRandomValue()
            "storage-classification" -> DefaultStorageClassifications.getRandomValue()
            "without" -> DefaultTagTree.get().getRandomValue()
            "aggregate" -> DefaultTagTree.get().getRandomValue()
            else -> throw Exception("Somehow not a valid attrId")
        }
        r.policy = policy
        return r
    }

    private fun getRandomString(length: Int) : String {
        val allowedChars = ('a'..'z')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

}