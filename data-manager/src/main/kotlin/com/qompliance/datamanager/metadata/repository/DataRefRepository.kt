package com.qompliance.datamanager.metadata.repository;

import com.qompliance.util.entity.metadata.tag.DataRef
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.stereotype.Repository

@RepositoryRestResource
@Repository
interface DataRefRepository : JpaRepository<DataRef, Long>
