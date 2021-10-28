val rootProperties = file("../gradle.properties")
    .readLines()
    .asSequence()
    .filter { it.isNotEmpty() && '=' in it }
    .map { it.split("=") }
    .associate { it[0] to it[1] }

allprojects {
    group = "org.jetbrains.gradle"
    version = System.getenv("GITHUB_REF")?.substringAfterLast("/") ?: "0.0.1"

    rootProperties.forEach { (k, v) -> extra[k] = v }

    repositories {
        mavenCentral()
    }
}
