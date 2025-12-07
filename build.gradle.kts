plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("jacoco")
}

group = "ru.pht.sprout"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // ===== РАБОЧИЕ ===== //

    // Версии
    implementation("io.github.z4kn4fein:semver:${project.properties["semver_version"]}")
    // Https
    implementation("io.ktor:ktor-client-core:${project.properties["ktor_version"]}")
    implementation("io.ktor:ktor-client-cio:${project.properties["ktor_version"]}")
    implementation("io.ktor:ktor-client-content-negotiation:${project.properties["ktor_version"]}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${project.properties["ktor_version"]}")
    // Сериализация
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${project.properties["serialization_version"]}")
    // Корутины
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.properties["coroutines_version"]}")

    // ===== ТЕСТОВЫЕ ===== //

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:${project.properties["ktor_version"]}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${project.properties["coroutines_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${project.properties["junit_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${project.properties["junit_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${project.properties["junit_version"]}")
    testImplementation("io.mockk:mockk:${project.properties["mockk_version"]}")
    testImplementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    testImplementation("org.slf4j:slf4j-simple:2.1.0-alpha1")
}


kotlin {
    jvmToolchain(8)
}

jacoco {
    toolVersion = "0.8.8"
}

tasks.test {
    useJUnitPlatform()
    testLogging { showStandardStreams = true }
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}