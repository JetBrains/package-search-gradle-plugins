import java.util.*

plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish")
    kotlin("plugin.serialization")
    kotlin("jvm")
}

val localProperties = file("local.properties").takeIf { it.exists() }

allprojects {
    group = "org.jetbrains.gradle"
    version = System.getenv("GITHUB_REF")?.substringAfterLast("/") ?: "0.0.1"

    localProperties?.let { extra.setAll(Properties(it)) }

    repositories {
        mavenCentral()
    }
}

dependencies {
    val dockerJavaVersion: String by project
    val serializationVersion: String by project
    api("com.github.docker-java:docker-java:$dockerJavaVersion")
    api("com.github.docker-java:docker-java-transport-httpclient5:$dockerJavaVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    api(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                languageVersion = "1.5"
            }
        }
    }
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

pluginBundle {
    website = "https://github.com/JetBrains/jetbrains-gradle-plugins"
    vcsUrl = "https://github.com/JetBrains/jetbrains-gradle-plugins.git"
    tags = listOf("docker", "container", "terraform", "cloud", "aws", "azure", "google", "liquibase", "migrations")
}

gradlePlugin {
    plugins {
        create("dockerPlugin") {
            id = "org.jetbrains.gradle.docker"
            displayName = "JetBrains Docker Plugin"
            description = "Build and push Docker images from your build."
            implementationClass = "org.jetbrains.gradle.plugins.docker.DockerPlugin"
        }
        create("terraformPlugin") {
            id = "org.jetbrains.gradle.terraform"
            displayName = "JetBrains Terraform Plugin"
            description = "Source sets plugin for controlling terraform projects from Gradle, batteries included."
            implementationClass = "org.jetbrains.gradle.plugins.terraform.TerraformPlugin"
        }
        create("liquibasePlugin") {
            id = "org.jetbrains.gradle.liquibase"
            displayName = "JetBrains Liquibase Plugin"
            description = "Run migrations from Gradle using the Liquibase runtime."
            implementationClass = "org.jetbrains.gradle.plugins.liquibase.LiquibasePlugin"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}

fun Properties(from: File) = Properties().apply { load(from.inputStream().buffered()) }

fun <T : Any, R> ExtraPropertiesExtension.setAll(map: Map<T, R>) =
    map.forEach { (k, v) -> set(k.toString(), v.toString()) }
