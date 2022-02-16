package com.qompliance.datamanager.policy.repository;

import com.qompliance.util.entity.policy.Policy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.stereotype.Repository

@RepositoryRestResource
@Repository
interface PolicyRepository : JpaRepository<Policy, Long> {

    @Query("SELECT p" +
            " FROM Policy p" +
            " LEFT JOIN FETCH p.requirements r" +
            " LEFT JOIN FETCH p.contextConditions c" +
            " WHERE EXISTS (" +
            "SELECT 1" +
            " FROM ContextCondition cx" +
            " WHERE cx.policy=p.id AND cx.attrId='tag' AND cx.attrVal IN :tags" +
            ")",
        nativeQuery = false)
    fun findPoliciesWithMatchingTags(@Param("tags") tags: Collection<String>): MutableSet<Policy>

    @Query("SELECT p" +
            " FROM Policy p" +
            " LEFT JOIN FETCH p.requirements r" +
            " LEFT JOIN FETCH p.contextConditions c" +
            " WHERE NOT EXISTS (" +
            "SELECT 1" +
            " FROM ContextCondition cx" +
            " WHERE cx.policy=p.id AND cx.attrId='tag'" +
            ")",
        nativeQuery = false)
    fun findPoliciesWithoutTags(): Set<Policy>

}
