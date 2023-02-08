package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
import org.jetbrains.gradle.plugins.property
import org.jetbrains.gradle.plugins.terraform.tasks.*
import org.jetbrains.gradle.plugins.toCamelCase
import java.io.File

open class TerraformSourceSet(private val project: Project, private val name: String) : Named {

    val baseBuildDir
        get() = "${project.buildDir}/terraform/$name"

    internal var applySpec = project.objects.property(
        Spec<TerraformApply> {
            it.logger.error(
                """
                    Please specify a criteria with which execute terraform apply:
                    terraform {
                        executeApplyOnlyIf { System.getenv("CAN_EXECUTE_TF_APPLY") == "true" }
                    }
                    Or disable the check:
                    terraform {
                        executeApplyOnlyIf { true }
                    }
                """.trimIndent()
            )
            false
        }
    )

    internal var destroySpec = project.objects.property(
        Spec<TerraformApply> {
            it.logger.error(
                """
                    Please specify a criteria with which execute terraform destroy:
                    terraform {
                        executeDestroyOnlyIf { System.getenv("CAN_EXECUTE_TF_DESTROY") == "true" }
                    }
                    Or disable the check:
                    terraform {
                        executeDestroyOnlyIf { true }
                    }
                """.trimIndent()
            )
            false
        }
    )

    /**
     * Adds the lamdas collected in the configuration as resources for this source set.
     */
    var addLambdasToResources = false

    /**
     * Adds the runtimes collected in the configuration as resources for this source set.
     */
    var addRuntimesToResources = false

    /**
     * Register the check to execute `terraform apply`. If `false`, `terraform apply` cannot be executed.
     * It is meant to avoid accidental executions while experimenting with the plugin.
     */
    fun executeApplyOnlyIf(action: Spec<TerraformApply>) {
        applySpec.set(action)
    }

    /**
     * Register the check to execute `terraform destroy`. If `false`, `terraform destroy` cannot be executed.
     * It is meant to avoid accidental executions while experimenting with the plugin.
     */
    fun executeDestroyOnlyIf(action: Spec<TerraformApply>) {
        destroySpec.set(action)
    }

    /**
     * The main directory in which Terraform will be executed.
     * Should contain the sources. Defaults to `"src/$name/terraform"`
     */
    var srcDirs = mutableSetOf(project.file("src/$name/terraform"))

    var resourcesDirs = mutableSetOf(project.file("src/$name/resources"))

    var dataDir: File = project.file("$baseBuildDir/data")

    var lockFile: File = project.file("src/$name/terraform/.terraform.lock.hcl")

    var stateFile: File = project.file("src/$name/terraform/terraform.tfstate")

    var metadata = TerraformModuleMetadata(
        group = project.group.toString(),
        moduleName = buildString {
            append(project.name)
            if (name != "main") append("-$name")
        },
        version = project.version.toString()
    )

    fun metadata(action: Action<TerraformModuleMetadata>) {
        action.execute(metadata)
    }

    var runtimeExecutionDirectory = project.file("$baseBuildDir/runtimeExecution")

    var outputJsonPlan: File = project.file("$baseBuildDir/plan.json")
    var outputDestroyJsonPlan: File = project.file("$baseBuildDir/destroyPlan.json")
    var outputBinaryPlan: File = project.file("$baseBuildDir/plan.bin")
    var outputDestroyBinaryPlan: File = project.file("$baseBuildDir/destroyPlan.bin")
    var dependsOn = setOf<TerraformSourceSet>()
        private set

    internal val outputTasks = mutableListOf<TaskProvider<TerraformOutput>>()

    internal val tasksProvider = TasksProvider()

    fun dependsOn(sourceSet: TerraformSourceSet) {
        if (sourceSet != this) {
            dependsOn = dependsOn + sourceSet
        }
    }

    fun dependsOn(sourceSet: NamedDomainObjectProvider<TerraformSourceSet>) {
        if (sourceSet != this) {
            dependsOn = dependsOn + sourceSet.get()
        }
    }

    /**
     * Variables used to execute `terraform plan` and `terraform destroy`.
     *
     * NOTE: use [filePlanVariables] for passing variables generated from tasks.
     */
    var planVariables = emptyMap<String, String?>()

    /**
     * Variables saved in files used to execute `terraform plan` and `terraform destroy`.
     * The files will be read a UTF-8 and the entire content will be used as value for the variables.
     *
     * Use this map to pass tasks outputs as variable for `terraform plan`.
     *
     * Example:
     * ```kotlin
     *
     * val myFile = file("output.txt")
     * task("generator") {
     *     doLast {
     *         myFile.writeText("bhamamama")
     *     }
     * }
     *
     * // .....
     *
     * terraform {
     *     sourceSets {
     *         main {
     *             filePlanVariables = mapOf("dr_kelso" to myFile)
     *         }
     *     }
     * }
     *
     * ```
     */
    var filePlanVariables = emptyMap<String, File>()

    /**
     * Variable that will be used to execute `terraform plan` and `terraform destroy`.
     *
     * NOTE: use overload with [File] for passing a variable generated from a task.
     */
    fun planVariable(key: String, value: String?) {
        planVariables = planVariables.toMutableMap().also { it[key] = value }
    }

    /**
     * Variable that will be read from file and used to execute `terraform plan` and `terraform destroy`.
     *
     * NOTE: use overload with [File] for passing a variable generated from a task.
     *
     * Use this fucction to pass tasks outputs as variable for `terraform plan`.
     *
     * Example:
     * ```kotlin
     *
     * val myFile = file("output.txt")
     * task("generator") {
     *     doLast {
     *         myFile.writeText("bhamamama")
     *     }
     * }
     *
     * // .....
     *
     * terraform {
     *     sourceSets {
     *         main {
     *             planVariable("dr_kelso", myFile)
     *         }
     *     }
     * }
     *
     */
    fun planVariable(key: String, value: File) {
        filePlanVariables = filePlanVariables.toMutableMap().also { it[key] = value }
    }

    /**
     * Creates a task to read an output from the TF state of the project.
     */
    fun outputVariable(vararg names: String, action: Action<TerraformOutput> = Action {}) =
        project.tasks.register<TerraformOutput>(
            "terraform${name.toCamelCase().capitalize()}Output${
                names.joinToString("") {
                    it.toCamelCase().capitalize()
                }
            }"
        ) {
            variables = names.toList()
            action(this)
        }.also { outputTasks.add(it) }

    /**
     * Adds a directory into which look for Terraform `.tf` sources.
     */
    fun srcDir(string: String) {
        srcDirs.add(project.file(string))
    }

    /**
     * Adds a directory into which look for resources.
     */
    fun resourceDir(string: String) {
        resourcesDirs.add(project.file(string))
    }

    /**
     * Allows access to generated tasks for this source set.
     */
    fun commands(action: Action<TasksProvider>) {
        action.execute(tasksProvider)
    }

    override fun getName() = name

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
}
