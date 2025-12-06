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
    implementation("io.github.z4kn4fein:semver:${project.properties["semver-version"]}")
    implementation("io.ktor:ktor-client-core:${project.properties["ktor-version"]}")
    implementation("io.ktor:ktor-client-cio:${project.properties["ktor-version"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.properties["coroutines-version"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${project.properties["serialization-version"]}")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${project.properties["coroutines-version"]}")
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