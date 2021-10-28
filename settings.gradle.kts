@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "jetbrains-gradle-plugins"

pluginManagement {
    plugins {
        val kotlinVersion: String by settings
        val gradlePublishPluginVersion: String by settings
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.gradle.plugin-publish") version gradlePublishPluginVersion
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
