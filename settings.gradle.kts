@file:Suppress("UnstableApiUsage")

rootProject.name = "jetbrains-gradle-plugins"

pluginManagement {
    includeBuild("plugins")
}

include(":tests")
