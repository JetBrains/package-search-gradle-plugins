plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.5.20"
}

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
        }
        main {
            dependencies {
                api("com.github.docker-java:docker-java:3.2.11")
                api("com.github.docker-java:docker-java-transport-httpclient5:3.2.11")
            }
        }
    }
}

gradlePlugin {
    plugins {
        create("containerBuilderPlugin") {
            id = "org.jetbrains.gradle.docker"
            implementationClass = "org.jetbrains.gradle.plugins.docker.DockerPlugin"
        }
        create("terraformPlugin") {
            id = "com.jetbrains.packagesearch.terraform"
            implementationClass = "org.jetbrains.gradle.plugins.terraform.TerraformPlugin"
        }
        create("liquibasePlugin") {
            id = "com.jetbrains.packagesearch.liquibase"
            implementationClass = "org.jetbrains.gradle.plugins.liquibase.LiquibasePlugin"
        }
    }
}
