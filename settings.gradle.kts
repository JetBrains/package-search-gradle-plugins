@file:Suppress("UnstableApiUsage")

rootProject.name = "jetbrains-gradle-plugins"

pluginManagement {
    includeBuild("plugins")
    plugins {
        kotlin("jvm") version "1.5.21"
    }
}

include(":tests", ":tests:docker-jvm-app", ":tests:terraform", ":tests:liquibase")
