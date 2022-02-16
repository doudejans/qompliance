package com.qompliance.util.entity.policy

import com.fasterxml.jackson.annotation.JsonManagedReference
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.NaturalId
import javax.persistence.*

@Entity
@Table(name = "policy")
open class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    @NaturalId
    @Column(name = "name", nullable = false)
    open var name: String? = null

    @Column(name = "owner", nullable = true)
    open var owner: String? = null

    @Column(name = "decision", nullable = true)
    open var decision: String? = null

    @OneToMany(mappedBy = "policy", cascade = [CascadeType.ALL])
    @Fetch(FetchMode.JOIN)
    @JsonManagedReference
    open var contextConditions: MutableSet<ContextCondition> = mutableSetOf()

    @OneToMany(mappedBy = "policy", cascade = [CascadeType.ALL])
    @Fetch(FetchMode.JOIN)
    @JsonManagedReference
    open var requirements: MutableSet<Requirement> = mutableSetOf()

    override fun toString(): String {
        return "Policy(id=$id, name=$name, decision=$decision, contextConditions=$contextConditions, requirements=$requirements)"
    }

}
