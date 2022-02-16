package com.qompliance.datamanager.policy.controller

import com.qompliance.datamanager.policy.repository.PolicyRepository
import com.qompliance.util.entity.policy.Policy
import org.apache.logging.log4j.kotlin.logger
import org.springframework.data.rest.webmvc.BasePathAwareController
import org.springframework.data.rest.webmvc.RepositoryRestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@BasePathAwareController
@RepositoryRestController
class PolicyController(val repository: PolicyRepository) {

    val logger = logger()

    data class PolicyResp(val policies: Set<Policy>)

    @GetMapping("/policies")
    @ResponseBody
    fun findByTags(@RequestParam tags: List<String>): PolicyResp {
        if (tags.isEmpty()) {
            return PolicyResp(repository.findPoliciesWithoutTags())
        }

        return PolicyResp(repository.findPoliciesWithMatchingTags(tags).toSet())
    }

}
