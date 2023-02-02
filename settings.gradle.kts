@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "package-search-gradle-plugins"

pluginManagement {
    plugins {
        val kotlinVersion = "1.7.10"
        val gradlePublishPluginVersion: String by settings
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.gradle.plugin-publish") version gradlePublishPluginVersion
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
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
