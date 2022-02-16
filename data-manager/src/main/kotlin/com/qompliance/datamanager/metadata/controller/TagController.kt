package com.qompliance.datamanager.metadata.controller

import com.qompliance.datamanager.metadata.repository.TagRepository
import com.qompliance.util.entity.metadata.tag.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class TagController(val repository: TagRepository) {

    data class TagsResp(val tags: Set<Tag>)

    @GetMapping("/tags")
    fun findByDataRefs(@RequestParam dataRefs: List<String>): TagsResp {
        return TagsResp(repository.getTagsByDataRef(dataRefs))
    }

}
