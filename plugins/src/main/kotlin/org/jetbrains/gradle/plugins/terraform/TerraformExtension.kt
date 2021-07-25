package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Named
import org.gradle.api.Project
import javax.inject.Inject

open class TerraformExtension @Inject constructor(val project: Project, private val name: String) : Named {

    var version = "1.0.3"

    override fun getName() =
        name
}
