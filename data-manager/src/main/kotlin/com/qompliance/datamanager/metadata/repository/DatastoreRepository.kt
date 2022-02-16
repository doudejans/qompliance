package com.qompliance.datamanager.metadata.repository;

import com.qompliance.util.entity.metadata.schema.Datastore
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.stereotype.Repository

@RepositoryRestResource(exported = true)
@Repository
interface DatastoreRepository : JpaRepository<Datastore, Long> {
    @Query("SELECT d" +
        " FROM Datastore d" +
        " LEFT JOIN FETCH d.datasets s" +
        " LEFT JOIN FETCH s.columns c" +
        " LEFT JOIN FETCH d.storageClassifications z" +
        " WHERE d.name IN :datastoreRefs" +
        " AND s.name IN :datasetRefs",
        nativeQuery = false)
    fun getDatastores(@Param("datastoreRefs") datastoreRefs: Set<String>, @Param("datasetRefs") datasetRefs: Set<String>): MutableSet<Datastore>
}