package com.qompliance.datamanager.metadata.controller

import com.qompliance.datamanager.metadata.repository.DatastoreRepository
import com.qompliance.util.entity.metadata.schema.Datastore
import org.springframework.data.rest.webmvc.BasePathAwareController
import org.springframework.data.rest.webmvc.RepositoryRestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@BasePathAwareController
@RepositoryRestController
class DatastoreController(val repository: DatastoreRepository) {

    data class DatastoresResp(val datastores: Set<Datastore>)

    @GetMapping("/datastores")
    @ResponseBody
    fun getAllDatastores(@RequestParam fromRefs: List<String>): DatastoresResp {
        val datastoreRefs = fromRefs.map { it.split('.')[0] }.toSet()
        val datasetRefs = fromRefs.map { it.split('.')[1] }.toSet()

        return DatastoresResp(repository.getDatastores(datastoreRefs, datasetRefs))
    }
}