val isCi = System.getenv("CI") == "true"
rootProject.name = if (isCi) "jetbrains-gradle-plugins" else file(".").nameWithoutExtension

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
