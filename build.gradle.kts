import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

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
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
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

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Monitoring
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // OpenAPI documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

/*
──────────────────────────────────────────────────────
============== Api generation ==============
──────────────────────────────────────────────────────
*/
val openApiDir = file("${rootDir}/openapi")
val foundSpecifications = openApiDir.listFiles { f -> f.extension in listOf("yaml", "yml") } ?: emptyArray()
logger.lifecycle("Found ${foundSpecifications.size} specifications: " + foundSpecifications.joinToString { it.name })

foundSpecifications.forEach { specFile ->
    val ourDir = getAbsolutePath(specFile.nameWithoutExtension)
    val packageName = defineJavaPackageName(specFile.nameWithoutExtension)

    val taskName = buildGenerateApiTaskName(specFile.nameWithoutExtension)
    logger.lifecycle("Register task ${taskName} from ${ourDir.get()}")
    val basePackage = "com.example.${packageName}"

    tasks.register(taskName, GenerateTask::class) {
        generatorName.set("spring")
        inputSpec.set(specFile.absolutePath)
        outputDir.set(ourDir)

        configOptions.set(
            mapOf(
                "library" to "spring-cloud",
                "skipDefaultInterface" to "true",
                "useBeanValidation" to "true",
                "openApiNullable" to "false",
                "useTags" to "true",
                "useJakartaEe" to "true",
                "initializeCollections" to "false",
                "apiPackage" to "${basePackage}.api",
                "modelPackage" to "${basePackage}.dto",
                "configPackage" to "${basePackage}.config"
            )
        )
        additionalProperties.set(
            mapOf(
                "lombok" to "true",
                "lombokBuilder" to "true",
                "lombokNoArgsConstructor" to "true",
                "lombokAllArgsConstructor" to "true",
                "lombokBuilderDefault" to "true"
            )
        )
        doFirst {
            logger.lifecycle("$taskName: starting generation from ${specFile.name}")
        }
    }
}

fun getAbsolutePath(nameWithoutExtension: String): Provider<String> {
    return layout.buildDirectory
        .dir("generated-sources/openapi/${nameWithoutExtension}")
        .map { it.asFile.absolutePath }
}

fun defineJavaPackageName(name: String): String {
    val beforeDash = name.substringBefore('-')
    val match = Regex("^[a-z]+").find(beforeDash)
    return match?.value ?: beforeDash.lowercase()
}

fun buildGenerateApiTaskName(name: String): String {
    return buildTaskName("generate", name)
}

fun buildJarTaskName(name: String): String {
    return buildTaskName("jar", name)
}

fun buildTaskName(taskPrefix: String, name: String): String {
    val prepareName = name
        .split(Regex("[^A-Za-z0-9]"))
        .filter { it.isNotBlank() }
        .joinToString("") { it.replaceFirstChar(Char::uppercase) }

    return "${taskPrefix}-${prepareName}"
}

val withoutExtensionNames = foundSpecifications.map { it.nameWithoutExtension }
sourceSets.named("main") {
    withoutExtensionNames.forEach { name ->
        java.srcDir(layout.buildDirectory.dir("generated-sources/openapi/$name/src/main/java"))
    }
}

tasks.register("generateAllOpenApi") {
    foundSpecifications.forEach { specFile ->
        dependsOn(buildGenerateApiTaskName(specFile.nameWithoutExtension))
    }
    doLast {
        logger.lifecycle("generateAllOpenApi: all specifications has been generated")
    }
}

tasks.named("compileJava") {
    dependsOn("generateAllOpenApi")
}

tasks.withType<Test> {
    useJUnitPlatform()
}