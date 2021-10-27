allprojects {
    group = "org.jetbrains.gradle"
    version = System.getenv("GITHUB_REF")?.substringAfterLast("/") ?: "0.0.1"

    repositories {
        mavenCentral()
    }
}
