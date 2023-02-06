package org.jetbrains.gradle.plugins.docker

import java.io.Serializable

/**
 * Hard coded list of JVM images available in the Docker Registry.
 */
sealed class JvmBaseImages : Serializable {

    abstract val imageName: String
    abstract val imageTag: String

    override fun toString() = "$imageName:$imageTag"

    object OpenJRE16Slim : JvmBaseImages() {

        override val imageName = "openjdk"
        override val imageTag = "16-jre-slim"
    }

    object OpenJDK16Slim : JvmBaseImages() {

        override val imageName = "openjdk"
        override val imageTag = "16-jdk"
    }

    object OpenJRE8Slim : JvmBaseImages() {

        override val imageName = "openjdk"
        override val imageTag = "8-jre-slim"
    }

    object OpenJDK8Slim : JvmBaseImages() {

        override val imageName = "openjdk"
        override val imageTag = "8-jdk-slim"
    }

    object OpenJRE11Slim : JvmBaseImages() {

        override val imageName = "openjdk"
        override val imageTag = "11-jre-slim"
    }

    object OpenJDK11Slim : JvmBaseImages() {

        override val imageName = "openjdk"
        override val imageTag = "11-jdk"
    }

    object OpenJRE8 : JvmBaseImages() {

        override val imageName = "openjdk"
        override val imageTag = "8-jre"
    }

    object OpenJDK8 : JvmBaseImages() {

        override val imageName = "openjdk"
        override val imageTag = "8-jdk"
    }

    object OpenJRE11 : JvmBaseImages() {

        override val imageName = "openjdk"
        override val imageTag = "11-jre"
    }

    object OpenJDK11 : JvmBaseImages() {

        override val imageName = "openjdk"
        override val imageTag = "11-jdk"
    }

    data class Custom(override val imageName: String, override val imageTag: String) : JvmBaseImages() {

        override fun toString() = super.toString()
    }
}
