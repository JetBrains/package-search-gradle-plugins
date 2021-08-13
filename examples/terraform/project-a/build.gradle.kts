plugins {
    id("org.jetbrains.gradle.terraform")
}

terraform {
    sourceSets {
        create("test") {
            dependsOn(main)
        }
    }
}
