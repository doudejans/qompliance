package com.qompliance.util.entity.metadata.tag

import com.fasterxml.jackson.annotation.JsonBackReference
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import javax.persistence.*

@Entity
@Table(name = "data_ref")
open class DataRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    @Column(name = "ref_id", nullable = true)
    open var refId: String? = null

    // The following attribute may go unused, depending on if we end up implementing row-level policies.
    @Column(name = "row_condition", nullable = true)
    open var rowCondition: String? = null

    @ManyToMany(mappedBy = "dataRefs")
    @Fetch(FetchMode.JOIN)
    @JsonBackReference
    open var tags: MutableSet<Tag> = mutableSetOf()

    override fun toString(): String {
        return "DataRef(id=$id, refId=$refId, rowCondition=$rowCondition)"
    }

}
