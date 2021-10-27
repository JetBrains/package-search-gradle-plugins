package org.jetbrains.gradle.plugins.terraform

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class TerraformModuleMetadata(var group: String, var moduleName: String, var version: String) : JavaSerializable