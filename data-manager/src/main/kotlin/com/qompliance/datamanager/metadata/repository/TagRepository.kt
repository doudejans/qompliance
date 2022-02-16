package com.qompliance.datamanager.metadata.repository;

import com.qompliance.util.entity.metadata.tag.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.stereotype.Repository

@RepositoryRestResource
@Repository
interface TagRepository : JpaRepository<Tag, Long> {

    /**
     * Important to note here is that due to how Hibernate works, the query below only returns Tags with the DataRefs
     * that actually apply (and thus not all DataRefs for that tag). This can be changed by adding an additional join
     * on the DataRefs. However, for our purposes this is fine. See: https://stackoverflow.com/a/51177569/15681986
     * Also note that we join the tags again on the DataRefs to prevent N+1 queries.
     */
    @Query("SELECT t" +
            " FROM Tag t" +
            " LEFT JOIN FETCH t.dataRefs d" +
            " JOIN FETCH d.tags s" +
            " WHERE d.refId IN :dataRefs")
    fun getTagsByDataRef(@Param("dataRefs") dataRefs: Collection<String>): Set<Tag>
}