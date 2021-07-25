package org.jetbrains.gradle.plugins.docker

import java.io.Serializable

sealed class JvmImageName : Serializable {

    abstract val imageName: String
    abstract val imageTag: String

    override fun toString() = "$imageName:$imageTag"

    object OpenJRE8Slim : JvmImageName() {

        override val imageName = "openjdk"
        override val imageTag = "8-jre-slim"
    }

    object OpenJDK8Slim : JvmImageName() {

        override val imageName = "openjdk"
        override val imageTag = "8-jdk-slim"
    }

    object OpenJRE11Slim : JvmImageName() {

        override val imageName = "openjdk"
        override val imageTag = "11-jre-slim"
    }

    object OpenJDK11Slim : JvmImageName() {

        override val imageName = "openjdk"
        override val imageTag = "11-jdk"
    }

    object OpenJRE8 : JvmImageName() {

        override val imageName = "openjdk"
        override val imageTag = "8-jre"
    }

    object OpenJDK8 : JvmImageName() {

        override val imageName = "openjdk"
        override val imageTag = "8-jdk"
    }

    object OpenJRE11 : JvmImageName() {

        override val imageName = "openjdk"
        override val imageTag = "11-jre"
    }

    object OpenJDK11 : JvmImageName() {

        override val imageName = "openjdk"
        override val imageTag = "11-jdk"
    }

    data class Custom(override val imageName: String, override val imageTag: String) : JvmImageName() {

        override fun toString() = super.toString()
    }
}
