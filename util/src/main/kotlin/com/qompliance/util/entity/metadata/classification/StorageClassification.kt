package com.qompliance.util.entity.metadata.classification

import com.qompliance.util.entity.metadata.schema.Datastore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.NaturalId
import javax.persistence.*

@Entity
@Table(name = "\"storage_classification\"")
open class StorageClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    @NaturalId
    @Column(name = "name", nullable = false)
    open var name: String? = null

    @ManyToMany(mappedBy = "storageClassifications")
    @JsonIgnoreProperties("storageClassifications")
    open var datastores: MutableSet<Datastore> = mutableSetOf()
}
