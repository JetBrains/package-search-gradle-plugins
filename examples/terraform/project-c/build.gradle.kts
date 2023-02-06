plugins {
    id("org.jetbrains.gradle.terraform")
}


dependencies {
    api(projects.examples.terraform.projectA)
}