@file:Suppress("UnstableApiUsage")

pluginManagement {
    plugins {

        val properties = file("../gradle.properties")
            .readLines()
            .filter { it.isNotEmpty() && '=' in it }
            .map { it.split("=") }
            .associate { it[0] to it[1] }

        val kotlinVersion by properties
        kotlin("jvm") version kotlinVersion
    }
}

includeBuild("..")

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        publishAlwaysIf(System.getenv("CI") == "true")
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

include(
    ":liquibase",
    ":docker-jvm-app",
    ":terraform",
    ":terraform:project-a",
    ":terraform:project-b"
)
