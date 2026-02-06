import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.springframework.boot.gradle.tasks.bundling.BootJar

val versions = mapOf(
    "springCloudStarterOpenfeign" to "4.1.1",
    "feignMicrometerVersion" to "13.6"
)

plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.13.0"
}

group = "org.example"
version = "0.0.1-SNAPSHOT"
description = "transaction-app"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
repositories {
    mavenCentral()
    mavenLocal()
}
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.15.0")
    }
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // OpenFeign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:${versions["springCloudStarterOpenfeign"]}")
    implementation("io.github.openfeign:feign-micrometer:${versions["feignMicrometerVersion"]}")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    // Flyway
    implementation("org.flywaydb:flyway-database-postgresql")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // Monitoring
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // OpenAPI documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("ch.qos.logback:logback-core:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Apache ShardingSphere JDBC
    implementation("org.apache.shardingsphere:shardingsphere-jdbc-core:5.4.1") {
        exclude(group = "org.glassfish.jaxb", module = "jaxb-runtime")
        exclude(group = "org.glassfish.jaxb", module = "jaxb-core")
        exclude(group = "jakarta.xml.bind", module = "jakarta.xml.bind-api")
    }

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Настройка обработки дубликатов для bootJar
tasks.withType<BootJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

/*
──────────────────────────────────────────────────────
=============== OpenAPI Generation ===================
──────────────────────────────────────────────────────
*/
val openApiDir = file("${rootDir}/openapi")
val foundSpecifications = openApiDir.listFiles { _, name -> name.endsWith(".yaml") || name.endsWith(".yml") } ?: emptyArray()
logger.lifecycle("Found ${foundSpecifications.size} OpenAPI specifications: ${foundSpecifications.joinToString { it.name }}")

foundSpecifications.forEach { specFile ->
    val outputDirPath = layout.buildDirectory.dir("generated-sources/openapi/${specFile.nameWithoutExtension}")
    val taskName = "generate${specFile.nameWithoutExtension.split(Regex("[^A-Za-z0-9]"))
        .joinToString("") { it.replaceFirstChar(Char::uppercase) }}"
    val basePackage = "org.example.transactionapp"

    tasks.register<GenerateTask>(taskName) {
        generatorName.set("spring")
        inputSpec.set(specFile.absolutePath)
        outputDir.set(outputDirPath.get().asFile.absolutePath)
        apiPackage.set("$basePackage.api")
        modelPackage.set("$basePackage.dto")
        library.set("spring-cloud")
        additionalProperties.set(
            mapOf(
                "configPackage" to "$basePackage.config", // <- сюда
                "useBeanValidation" to "true",
                "openApiNullable" to "false",
                "skipDefaultInterface" to "true",
                "useTags" to "true",
                "useJakartaEe" to "true",
                "initializeCollections" to "false",
                "lombok" to "true",
                "lombokBuilder" to "true",
                "lombokNoArgsConstructor" to "true",
                "lombokAllArgsConstructor" to "true",
                "lombokBuilderDefault" to "true"
            )
        )
        doFirst {
            logger.lifecycle("$taskName: Generating code from ${specFile.name}")
        }
    }
}
// Добавляем сгенерированные папки в main sourceSet
sourceSets.named("main") {
    java {
        foundSpecifications.forEach { specFile ->
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/${specFile.nameWithoutExtension}/src/main/java"))
        }
    }
}

// Собираем все OpenAPI генерации в одну задачу
tasks.register("generateAllOpenApi") {
    foundSpecifications.forEach { specFile ->
        dependsOn("generate${specFile.nameWithoutExtension.split(Regex("[^A-Za-z0-9]"))
            .joinToString("") { it.replaceFirstChar(Char::uppercase) } }")
    }
    doLast {
        logger.lifecycle("All OpenAPI specifications have been generated")
    }
}

// Генерация перед компиляцией
tasks.named("compileJava") {
    dependsOn("generateAllOpenApi")
}

// Используем JUnit Platform для тестов
tasks.withType<Test> {
    useJUnitPlatform()
}