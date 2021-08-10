package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.setValue
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformInit
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformPlan
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformShow
import java.io.File

open class TerraformSourceSet(private val project: Project, private val name: String) : Named {

    private val baseBuildDir
        get() = "${project.buildDir}/terraform/$name"

    /**
     * The main directory in which Terraform will be executed.
     * Should contain the sources. Defaults to `"src/$name/terraform"`
     */
    var srcDir: File = project.file("src/$name/terraform")

    var dataDir: File = project.file("$baseBuildDir/data")

    var outputJsonPlan: File = project.file("$baseBuildDir/plan.json")
    var outputDestroyJsonPlan: File = project.file("$baseBuildDir/destroyPlan.json")
    var outputBinaryPlan: File = project.file("$baseBuildDir/plan.bin")
    var outputDestroyBinaryPlan: File = project.file("$baseBuildDir/destroyPlan.bin")

    internal val tasksProvider = TasksProvider()

    /**
     * Variables used to execute `terraform plan` and `terraform destroy`.
     */
    var planVariables by project.objects.mapProperty<String, String>()

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
    fun commands(action: Action<TasksProvider>) {
        action.execute(tasksProvider)
    }

    class TasksProvider {
        internal val initActions = mutableListOf<Action<TerraformInit>>()
        internal val planActions = mutableListOf<Action<TerraformPlan>>()
        internal val destroyPlanActions = mutableListOf<Action<TerraformPlan>>()
        internal val applyActions = mutableListOf<Action<TerraformApply>>()
        internal val destroyActions = mutableListOf<Action<TerraformApply>>()
        internal val showActions = mutableListOf<Action<TerraformShow>>()
        internal val destroyShowActions = mutableListOf<Action<TerraformShow>>()

        /**
         * Configures an [Action]<[TerraformInit]> to be executed against the respective task.
         */
        fun terraformInit(action: Action<TerraformInit>) {
            initActions.add(action)
        }


        /**
         * Configures an [Action]<[TerraformInit]> to be executed against the respective task.
         */
        fun terraformShow(action: Action<TerraformShow>) {
            showActions.add(action)
        }

        /**
         * Configures an [Action]<[TerraformInit]> to be executed against the respective task.
         */
        fun terraformDestroyShow(action: Action<TerraformShow>) {
            showActions.add(action)
        }

        /**
         * Configures an [Action]<[TerraformPlan]> to be executed against the respective task.
         */
        fun terraformPlan(action: Action<TerraformPlan>) {
            planActions.add(action)
        }


        /**
         * Configures an [Action]<[TerraformPlan]> to be executed against the respective task.
         */
        fun terraformDestroyPlan(action: Action<TerraformPlan>) {
            destroyPlanActions.add(action)
        }

        /**
         * Configures an [Action]<[TerraformApply]> to be executed against the respective task.
         */
        fun terraformApply(action: Action<TerraformApply>) {
            applyActions.add(action)
        }


        /**
         * Configures an [Action]<[TerraformApply]> to be executed against the respective task.
         */
        fun terraformDestroy(action: Action<TerraformApply>) {
            destroyActions.add(action)
        }

    }

    override fun getName() = name

}
