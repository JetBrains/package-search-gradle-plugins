plugins {
    id("org.jetbrains.gradle.terraform")
}


dependencies {
    api(projects.terraform.projectA)
}