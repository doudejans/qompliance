package com.qompliance.compliancechecker.controller

import com.qompliance.compliancechecker.ComplianceValidationJob
import com.qompliance.compliancechecker.Config
import com.qompliance.compliancechecker.dto.Validation
import com.qompliance.compliancechecker.dto.ValidationResult
import com.qompliance.util.exception.AttributeValidationException
import com.qompliance.util.exception.PolicyValidationException
import org.apache.calcite.runtime.CalciteContextException
import org.apache.calcite.sql.parser.SqlParseException
import org.apache.calcite.sql.validate.SqlValidatorException
import org.apache.logging.log4j.kotlin.logger
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class ValidationController {

    val logger = logger()

    val config: Config = AnnotationConfigApplicationContext(Config::class.java).getBean(Config::class.java)

    @PostMapping("/validation")
    fun startValidation(@RequestBody validation: Validation): ValidationResult {
        val res = try {
            val job = ComplianceValidationJob(validation, config.defaultDecision)
            job.runJob()
        } catch (e: SqlParseException) {
            logger.debug(e.stackTraceToString())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Exception while parsing SQL: ${e.message}", e)
        } catch (e: CalciteContextException) {
            logger.debug(e.stackTraceToString())
            if (e.cause is SqlValidatorException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Exception while validating SQL: ${e.message}", e)
            } else {
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Exception", e)
            }
        } catch (e: AttributeValidationException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Exception while validating attributes: ${e.message}", e)
        } catch (e: PolicyValidationException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Exception while validating policy: ${e.message}", e)
        }
        return res
    }

}
