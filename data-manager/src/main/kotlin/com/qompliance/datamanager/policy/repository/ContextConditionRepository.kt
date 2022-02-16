package com.qompliance.datamanager.policy.repository

import com.qompliance.util.entity.policy.ContextCondition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContextConditionRepository : JpaRepository<ContextCondition, Long>