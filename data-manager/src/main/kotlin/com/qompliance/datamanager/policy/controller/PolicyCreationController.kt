package com.qompliance.datamanager.policy.controller

import com.qompliance.datamanager.policy.parser.PolicyConverter
import com.qompliance.datamanager.policy.parser.PolicyParser
import com.qompliance.datamanager.policy.repository.PolicyRepository
import com.qompliance.util.entity.policy.Policy
import org.apache.logging.log4j.kotlin.logger
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class PolicyCreationController(val repository: PolicyRepository) {
    val logger = logger()

    @PostMapping("/yaml/policies", consumes = [MediaType.ALL_VALUE])
    @ResponseBody
    fun newPolicyFromYaml(@RequestBody body: String): Policy {
        val policy = PolicyParser.parse(body)
        val policyEntity = PolicyConverter.convertToPolicyEntity(policy)
        logger.info("Saving $policyEntity")
        return repository.save(policyEntity)
    }

    @GetMapping("/yaml/policies")
    @ResponseBody
    fun policiesToYaml(): List<String> {
        val parsedPolicies = PolicyConverter.convertToParsedPolicies(repository.findAll())
        val res = mutableListOf<String>()
        for (policy in parsedPolicies) {
            res.add(PolicyParser.toYamlString(policy))
        }
        return res
    }

    @GetMapping("/yaml/policies/{id}")
    @ResponseBody
    fun policyToYaml(@PathVariable id: Long): String {
        val parsedPolicy = PolicyConverter.convertToParsedPolicy(repository.findById(id).get())
        return PolicyParser.toYamlString(parsedPolicy)
    }
}