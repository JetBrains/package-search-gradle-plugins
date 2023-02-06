@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "package-search-gradle-plugins"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../dependencies.toml"))
        }
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
