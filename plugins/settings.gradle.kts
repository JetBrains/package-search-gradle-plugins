val isCi = System.getenv("CI") == "true"
rootProject.name = if (isCi) "jetbrains-gradle-plugins" else file(".").nameWithoutExtension
