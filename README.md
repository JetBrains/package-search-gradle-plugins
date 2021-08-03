# JetBrains Gradle Plugins

Collection of Gradle plugin by JetBrains.

## Docker plugin

Build Docker images easily in your build.

```kotlin
plugins {
    id("org.jetbrains.gradle.docker") version "{latestVersion}"
}

docker {
    host = "tcp Docker address"
    useTsl = true // default false
    dockerCertPath = file("/path/to/cert") // or dockerCertPath("path/to/cert")
    
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
            
        }
    }
}
```
