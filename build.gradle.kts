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

    // Хеш SHA-512
    implementation("org.kotlincrypto.hash:sha2-512:0.2.7")
    // Форматирование строк
    implementation("com.github.pwittchen.kirai:library:1.4.1")
    // Версии
    implementation("io.github.z4kn4fein:semver:${project.properties["semver_version"]}")
    // Https
    implementation("io.ktor:ktor-client-core:${project.properties["ktor_version"]}")
    implementation("io.ktor:ktor-client-cio:${project.properties["ktor_version"]}")
    implementation("io.ktor:ktor-client-apache5:3.3.3")

    implementation("io.ktor:ktor-client-content-negotiation:${project.properties["ktor_version"]}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${project.properties["ktor_version"]}")
    // Сериализация
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${project.properties["serialization_version"]}")
    // Корутины
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.properties["coroutines_version"]}")
    // Логирование
    implementation("io.ktor:ktor-client-logging:3.3.3")
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")
    implementation("io.ktor:ktor-client-apache:3.3.3")

    // ===== ТЕСТОВЫЕ ===== //

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:${project.properties["ktor_version"]}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${project.properties["coroutines_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${project.properties["junit_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${project.properties["junit_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${project.properties["junit_version"]}")
    testImplementation("io.mockk:mockk:${project.properties["mockk_version"]}")
}

kotlin {
    jvmToolchain(8)
}

jacoco {
    toolVersion = "0.8.8"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "ru.pht.sprout.cli.App"
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
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