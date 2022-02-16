group = "com.qompliance.compliancechecker"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":util"))

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.apache.calcite:calcite-core:1.28.0")

    implementation("io.github.erdtman:java-json-canonicalization:1.1")
}
