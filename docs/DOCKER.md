## Docker plugin

Build Docker images easily in your build. Say that you need to pack in an image all the stuff in a directory, with
the `Dockerfile` in root of the project:

```kotlin
plugins {
    id("org.jetbrains.gradle.docker") version "{latestVersion}"
}

version = "1.0.0" // will be used for image tag version

docker {
    registries {
        dockerHub("username", System.getenv("DOCKER_HUB_PASSWORD"))
    }
    images {
        create("my-image") {
            files { from("Dockerfile", "path/to/content/directory") }
        }
    }
}

```

It will create many tasks:

- `dockerMyImageBuild`: Builds the image
- `pushMyImageToDockerHub`: Will push the previously built image to the Docker Hub

If you are building a JVM applications try using `setupJvmApp()`, see full example below.

See an example [here](../examples/docker-jvm-app/build.gradle.kts)

### Full example

```kotlin
plugins {
    id("org.jetbrains.gradle.docker") version "{latestVersion}"
    java
    application
}

application {
    mainCLass.set("com.mycompany.MainClass")
}

docker {

    // optional, it will use command line otherwise
    useDockerRestApi {
        host = "tcp://localhost:2375"
        useTsl = true // default false
        dockerCertPath = file("/path/to/cert") // or dockerCertPath("path/to/cert")
    }

    registries {
        dockerHub("username", "password")
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
        create("other-image") {
            // image name defaults to otherImage
            // image version defaults to "project.version.toString()"

            setupJvmApp(JvmBaseImages.OpenJRE16Slim)
            // setupJvmApp("openjdk", "16-jre-slim")
            // setupJvmApp("openjdk:16-jre-slim")
        }
    }
}
```
