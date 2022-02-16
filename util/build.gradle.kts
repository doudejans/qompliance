group = "com.qompliance.util"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.hibernate:hibernate-core:5.4.31.Final")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springdoc:springdoc-openapi-ui:1.5.11")
    implementation("org.springdoc:springdoc-openapi-data-rest:1.5.11")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
