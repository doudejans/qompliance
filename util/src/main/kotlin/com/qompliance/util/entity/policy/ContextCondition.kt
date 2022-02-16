package com.qompliance.util.entity.policy

import com.fasterxml.jackson.annotation.JsonBackReference
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import javax.persistence.*

@Entity
@Table(name = "context_condition")
open class ContextCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    @ManyToOne
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonBackReference
    open var policy: Policy? = null

    @Column(name = "attr_id", nullable = false)
    open var attrId: String? = null

    @Column(name = "attr_val", nullable = true)
    open var attrVal: String? = null

    override fun toString(): String {
        return "ContextCondition(id=$id, attrId=$attrId, attrVal=$attrVal)"
    }
}