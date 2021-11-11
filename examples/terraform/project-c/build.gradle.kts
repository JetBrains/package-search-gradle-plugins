plugins {
    id("org.jetbrains.gradle.terraform")
}


dependencies {
    terraformApi(projects.terraform.projectA)
}