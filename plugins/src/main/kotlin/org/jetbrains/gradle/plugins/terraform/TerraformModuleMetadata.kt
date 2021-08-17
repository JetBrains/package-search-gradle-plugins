package org.jetbrains.gradle.plugins.terraform

import kotlinx.serialization.Serializable

@Serializable
data class TerraformModuleMetadata(var group: String, var moduleName: String) : java.io.Serializable