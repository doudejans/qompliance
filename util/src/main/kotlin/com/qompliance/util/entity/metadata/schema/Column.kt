package com.qompliance.util.entity.metadata.schema

import com.fasterxml.jackson.annotation.JsonBackReference
import javax.persistence.*
import javax.persistence.Column

@Entity
@Table(name = "schema_column")
open class Column {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    @Column(name = "name", nullable = false)
    open var name: String? = null

    @ManyToOne
    @JoinColumn(name = "dataset_id", nullable = false)
    @JsonBackReference
    open var dataset: Dataset? = null

    @Column(name = "type", nullable = false)
    open var type: String? = null

    override fun toString(): String {
        return "Column(id=$id, name=$name)"
    }

}
