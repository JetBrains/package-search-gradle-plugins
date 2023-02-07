@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "package-search-gradle-plugins"

pluginManagement {
    includeBuild("./plugins")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("dependencies.toml"))
        }
    }
}

include(
    ":examples:docker-jvm-app",
    ":examples:liquibase",
    ":examples:terraform",
    ":examples:terraform:project-a",
    ":examples:terraform:project-b",
    ":examples:terraform:project-c",
    ":examples:upx",
    ":examples:aws-lambda-custom-runtime"
)

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
