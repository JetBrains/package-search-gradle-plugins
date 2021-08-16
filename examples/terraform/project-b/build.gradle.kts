plugins {
    id("org.jetbrains.gradle.terraform")
    `maven-publish`
}

terraform {

}

dependencies {
    terraformApi(project(":examples:terraform:project-a"))
}
