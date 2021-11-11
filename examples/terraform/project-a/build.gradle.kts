plugins {
    id("org.jetbrains.gradle.terraform")
    `maven-publish`
}

terraform {
    sourceSets {
        create("test") {
            dependsOn(main)
        }
    }
}
