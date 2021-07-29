package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformInit
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformPlan

open class TerraformDirectorySet(private val project: Project, private val name: String) : Named {

    /**
     * The main directory in which Terraform will be executed.
     * Should contain the sources. Defaults to `"src/$name/terraform"`
     */
    var srcDir = project.file("src/$name/terraform")

    internal val tasksProvider = TasksProvider()

    /**
     * Sets the main directory in which Terraform will be executed.
     * Should contain the sources.
     */
    fun srcDir(string: String) {
        srcDir = project.file(string)
    }

    /**
     * Allows access to generated tasks for this source set.
     */
    fun tasksProvider(action: Action<TasksProvider>) {
        action.execute(tasksProvider)
    }

    class TasksProvider {
        internal val initActions = mutableListOf<Action<TerraformInit>>()
        internal val planActions = mutableListOf<Action<TerraformPlan>>()
        internal val applyActions = mutableListOf<Action<TerraformApply>>()

        /**
         * Configures an [Action]<[TerraformInit]> to be executed against the respective task.
         */
        fun terraformInit(action: Action<TerraformInit>) {
            initActions.add(action)
        }

        /**
         * Configures an [Action]<[TerraformPlan]> to be executed against the respective task.
         */
        fun terraformPlan(action: Action<TerraformPlan>) {
            planActions.add(action)
        }

        /**
         * Configures an [Action]<[TerraformApply]> to be executed against the respective task.
         */
        fun terraformApply(action: Action<TerraformApply>) {
            applyActions.add(action)
        }
    }

    override fun getName() = name

}
