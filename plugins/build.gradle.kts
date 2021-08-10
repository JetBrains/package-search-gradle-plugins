plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.15.0"
}

allprojects {
    group = "org.jetbrains.gradle"
    version = System.getenv("GITHUB_REF")?.substringAfterLast("/") ?: "0.0.1"
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.github.docker-java:docker-java:3.2.11")
    api("com.github.docker-java:docker-java-transport-httpclient5:3.2.11")
}

kotlin {
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
        }
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
