package com.qompliance.util.entity.metadata.schema

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonManagedReference
import javax.persistence.*
import javax.persistence.Column

@Entity
@Table(name = "schema_dataset")
open class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    @Column(name = "name", nullable = false)
    open var name: String? = null

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "datastore_id", nullable = false)
    @JsonBackReference
    open var datastore: Datastore? = null

    @OneToMany(mappedBy = "dataset", cascade = [CascadeType.ALL])
    @JsonManagedReference
    open var columns: MutableSet<com.qompliance.util.entity.metadata.schema.Column> = mutableSetOf()

    override fun toString(): String {
        return "Dataset(id=$id, name=$name, columns=$columns)"
    }

}
