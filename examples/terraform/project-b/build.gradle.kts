plugins {
    id("org.jetbrains.gradle.terraform")
    `maven-publish`
    distribution
}

terraform {
    version = "1.0.4"

    sourceSets {
        main {
            planVariables("example" to "ciao")
            metadata {
                group = "org.example"
                moduleName = "example-b"
            }
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                developers {
                    developer {
                        name.set("Lamberto Basti")
                        email.set("basti dot lamberto at gmail dot com")
                    }
                }
                scm {
                    //whatever
                }
            }
        }
    }
}

dependencies {
    //terraformApi(project(":examples:terraform:project-a"))
}
