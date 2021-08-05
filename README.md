# JetBrains Gradle Plugins

Collection of Gradle plugin by JetBrains.

## Docker plugin

Build Docker images easily in your build.

```kotlin
plugins {
    id("org.jetbrains.gradle.docker") version "{latestVersion}"
}

docker {
    
    // optional, it will use command line otherwise
    useDockerRestApi {
        host = "tcp://localhost:2375"
        useTsl = true // default false
        dockerCertPath = file("/path/to/cert") // or dockerCertPath("path/to/cert")
    }
    
    repositories {
        create("my-custom-business-registry") {
            username = System.getenv("REGISTRY_USERNAME")
            password = System.getenv("REGISTRY_PASSWORD")
            url = System.getenv("REGISTRY_URL")
            imageNamePrefix = System.getenv("REGISTRY_URL") + "/container/whatever"
        }
    }
    
    images {
        // assuming project.name == "my-project"
        myProject {
            files { 
                from("Dockerfile")
                from(file("/docker/whatever"))
            }
            buildArgs = mapOf("MY_ENV_VAR" to System.getenv("MY_ENV_VAR"))
            imageName = "myProject"
            imaveVersion = project.version.toString()
        }
    }
}
```
