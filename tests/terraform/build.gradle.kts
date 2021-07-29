import org.jetbrains.gradle.plugins.terraform.main

plugins {
    id("org.jetbrains.terraform")
}

terraform {
    sourceSets {
        main {
            tasksProvider {

            }
        }
    }
}
