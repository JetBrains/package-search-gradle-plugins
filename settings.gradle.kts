@file:Suppress("UnstableApiUsage")

rootProject.name = "jetbrains-gradle-plugins"

pluginManagement {
    includeBuild("plugins")
    plugins {
        kotlin("jvm") version "1.5.21"
    }
}

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
    ":examples",
    ":examples:docker-jvm-app",
    ":examples:terraform",
    ":examples:terraform:project-a",
    ":examples:terraform:project-b",
    ":examples:liquibase"
)
