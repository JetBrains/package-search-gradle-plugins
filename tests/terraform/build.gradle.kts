import org.jetbrains.gradle.plugins.terraform.main

plugins {
    id("org.jetbrains.gradle.terraform")
}

terraform {
    sourceSets {
        main {
            tasksProvider {

            }
        }
    }
}
