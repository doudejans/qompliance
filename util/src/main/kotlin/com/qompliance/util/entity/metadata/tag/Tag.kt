package com.qompliance.util.entity.metadata.tag

import com.fasterxml.jackson.annotation.JsonManagedReference
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.NaturalId
import javax.persistence.*

@Entity
@Table(name = "tag")
open class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    @NaturalId
    @Column(name = "name", nullable = false)
    open var name: String? = null

    @ManyToMany(cascade = [CascadeType.ALL])
    @Fetch(FetchMode.JOIN)
    @JoinTable(
        name = "tag_data_ref",
        joinColumns = [JoinColumn(name = "tag_id")],
        inverseJoinColumns = [JoinColumn(name = "data_ref_id")]
    )
    @JsonManagedReference
    open var dataRefs: MutableSet<DataRef> = mutableSetOf()

    override fun toString(): String {
        return "Tag(id=$id, name=$name, dataRefs=$dataRefs)"
    }

}
