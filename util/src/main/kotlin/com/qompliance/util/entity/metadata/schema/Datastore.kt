package com.qompliance.util.entity.metadata.schema

import com.qompliance.util.entity.metadata.classification.StorageClassification
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonManagedReference
import org.hibernate.annotations.NaturalId
import javax.persistence.*
import javax.persistence.Column

@Entity
@Table(name = "schema_datastore")
open class Datastore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    @NaturalId
    @Column(name = "name", nullable = false)
    open var name: String? = null

    @Column(name = "location", nullable = true)
    open var location: String? = null

    @Column(name = "type", nullable = true)
    open var type: String? = null

    @OneToMany(mappedBy = "datastore", cascade = [CascadeType.ALL])
    @JsonManagedReference
    open var datasets: MutableSet<Dataset> = mutableSetOf()

    @ManyToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name = "storage_classification_link",
        joinColumns = [JoinColumn(name = "datastore_id")],
        inverseJoinColumns = [JoinColumn(name = "storage_classification_id")]
    )
    @JsonIgnoreProperties("datastores")
    open var storageClassifications: MutableSet<StorageClassification> = mutableSetOf()

    override fun toString(): String {
        return "Datastore(id=$id, name=$name, location=$location, type=$type, datasets=$datasets)"
    }
}
