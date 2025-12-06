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
    implementation("io.github.z4kn4fein:semver:${project.properties["semver_version"]}")
    implementation("io.ktor:ktor-client-core:${project.properties["ktor_version"]}")
    implementation("io.ktor:ktor-client-cio:${project.properties["ktor_version"]}")
    implementation("io.ktor:ktor-client-content-negotiation:${project.properties["ktor_version"]}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${project.properties["ktor_version"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.properties["coroutines_version"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${project.properties["serialization_version"]}")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:${project.properties["ktor_version"]}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${project.properties["coroutines_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${project.properties["junit_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${project.properties["junit_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${project.properties["junit_version"]}")
    testImplementation("io.mockk:mockk:${project.properties["mockk_version"]}")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

kotlin {
    jvmToolchain(8)
}

jacoco {
    toolVersion = "0.8.8"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}