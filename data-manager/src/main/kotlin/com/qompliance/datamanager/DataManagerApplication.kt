package com.qompliance.datamanager

import org.apache.logging.log4j.kotlin.logger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication

@SpringBootApplication
@EntityScan("com.qompliance.util.entity.*")
class DataManagerApplication {
    val logger = logger()
}

fun main(args: Array<String>) {
    runApplication<DataManagerApplication>(*args)
}
