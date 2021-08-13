plugins {
    id("org.jetbrains.gradle.terraform")
    `maven-publish`
}

terraform {

}

dependencies {
    terraformApi(projects.examples.terraform.projectA)
}
