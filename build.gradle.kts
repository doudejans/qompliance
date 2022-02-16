group = "com.qompliance"
version = "0.1-SNAPSHOT"

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-allopen:1.5.31")
    }
}

plugins {
    id("java")
    id("org.springframework.boot") version "2.5.5" apply false
    id("io.spring.dependency-management") version "1.0.11.RELEASE" apply false
    kotlin("jvm") version "1.6.20-M1" apply false
    kotlin("plugin.spring") version "1.6.20-M1" apply false
    kotlin("plugin.jpa") version "1.6.20-M1" apply false
}

subprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    apply {
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("org.jetbrains.kotlin.plugin.jpa")
    }

    dependencies {
        implementation("org.apache.logging.log4j:log4j-api:2.17.0")
        implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.0")
        implementation("org.apache.logging.log4j:log4j-core:2.17.0")
        implementation("org.apache.logging.log4j:log4j-api-kotlin:1.1.0")
    }

    configurations {
        all {
            exclude("org.springframework.boot", "spring-boot-starter-logging")
        }
    }
}
