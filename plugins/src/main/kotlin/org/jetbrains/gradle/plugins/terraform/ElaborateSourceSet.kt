package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RelativePath
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.gradle.plugins.executeAllOn
import org.jetbrains.gradle.plugins.maybeRegister
import org.jetbrains.gradle.plugins.terraform.tasks.CopyTerraformResourceFileInModules
import org.jetbrains.gradle.plugins.terraform.tasks.GenerateResourcesTerraformFile
import org.jetbrains.gradle.plugins.terraform.tasks.GenerateTerraformMetadata
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformExtract
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformInit
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformOutput
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformPlan
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformShow
import org.jetbrains.gradle.plugins.toCamelCase

internal fun Project.elaborateSourceSet(
    sourceSet: TerraformSourceSet,
    terraformApi: Configuration,
    terraformImplementation: Configuration,
    lambdaConfiguration: Configuration,
    terraformExtract: TaskProvider<TerraformExtract>,
    terraformExtension: TerraformExtension,
    terraformInit: TaskProvider<Task>,
    terraformShow: TaskProvider<Task>,
    terraformPlan: TaskProvider<Task>,
    terraformApply: TaskProvider<Task>,
    terraformDestroyShow: TaskProvider<Task>,
    terraformDestroyPlan: TaskProvider<Task>,
    terraformDestroy: TaskProvider<Task>,
    softwareComponentFactory: SoftwareComponentFactory
) {
    val taskName = sourceSet.name.capitalize()
    val terraformModuleMetadata =
        tasks.register<GenerateTerraformMetadata>("terraform${taskName}Metadata") {
            outputFile = file("${sourceSet.baseBuildDir}/tmp/metadata.json")
            metadata = sourceSet.metadata
        }

    fun CopySpec.sourcesCopySpec(action: CopySpec.() -> Unit = {}) {
        from(sourceSet.getSourceDependencies().flatMap { it.srcDirs } + sourceSet.srcDirs) {
            action()
            include { it.file.extension == "tf" || it.isDirectory }
        }
        from(sourceSet.getSourceDependencies().flatMap { it.resourcesDirs } + sourceSet.resourcesDirs) {
            into("resources")
        }
        if (sourceSet.addLambdasToResources) from(lambdaConfiguration) {
            into("resources")
        }
    }

    val terraformModuleZip = tasks.create<Zip>("terraform${taskName}Module") {
        from(terraformModuleMetadata)
        sourcesCopySpec {
            into("src/${sourceSet.metadata.group}/${sourceSet.metadata.moduleName}")
        }
        includeEmptyDirs = false
        exclude { it.file.endsWith(".terraform.lock.hcl") }
        duplicatesStrategy = DuplicatesStrategy.WARN
        archiveFileName.set("terraform${project.name.toCamelCase().capitalize()}${taskName}.tfmodule")
        destinationDirectory.set(file("$buildDir/terraform/archives"))
    }

    if (sourceSet.name == "main")
        createComponent(taskName, terraformApi, terraformModuleZip, sourceSet, softwareComponentFactory)

    val terraformRuntimeElements =
        configurations.create("terraform${taskName}RuntimeElements") {
            isCanBeResolved = true
            isCanBeConsumed = false
            extendsFrom(terraformImplementation)
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(TerraformPlugin.Attributes.USAGE))
                attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(TerraformPlugin.Attributes.LIBRARY_ELEMENTS)
                )
            }
        }

    val tmpRestFile = file("${sourceSet.baseBuildDir}/tmp/res.tf")

    val copyResFiles =
        tasks.register<CopyTerraformResourceFileInModules>("copy${taskName}ResFileInExecutionContext") {
            onlyIf { tmpRestFile.exists() }
            inputResFile = tmpRestFile
            runtimeContextDir = sourceSet.runtimeExecutionDirectory
        }

    val createResFile =
        tasks.register<GenerateResourcesTerraformFile>("generate${taskName}ResFile") {
            val resDir = sourceSet.runtimeExecutionDirectory.resolve("resources")
            onlyIf { resDir.exists() }
            resourcesDirectory = resDir
            outputResourceModuleFile = tmpRestFile
            finalizedBy(copyResFiles)
        }

    val copyExecutionContext = tasks.register<Sync>("generate${taskName}ExecutionContext") {
        dependsOn(terraformRuntimeElements)

        fun sourcesSpec(toDrop: Int): Action<CopySpec> = Action {

            eachFile {
                if (relativePath.segments.first() == "src") {
                    relativePath = RelativePath(
                        true,
                        *relativePath.segments.drop(toDrop).toTypedArray()
                    )
                }
            }
        }

        from(terraformRuntimeElements.resolve().map { zipTree(it) }, sourcesSpec(1))
        from(terraformModuleMetadata)
        sourcesCopySpec()
        exclude { it.name == "metadata.json" }
        includeEmptyDirs = false
        from(sourceSet.lockFile)
        into(sourceSet.runtimeExecutionDirectory)
        finalizedBy(createResFile)
    }

    val syncLockFile = tasks.register<Copy>("sync${taskName}LockFile") {
        from(sourceSet.runtimeExecutionDirectory.resolve(".terraform.lock.hcl"))
        into(sourceSet.lockFile.parentFile)
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    plugins.withId("org.gradle.distribution") {
        configure<DistributionContainer> {
            maybeRegister(sourceSet.name) {
                contents {
                    from(copyExecutionContext)
                    from(terraformExtract) {
                        rename {
                            buildString {
                                append("terraform")
                                if (OperatingSystem.current().isWindows)
                                    append(".exe")
                            }
                        }
                    }
                }
            }
        }
    }

    val tfInit: TaskProvider<TerraformInit> =
        tasks.terraformRegister("terraform${taskName}Init", sourceSet) {
            dependsOn(copyExecutionContext)
            finalizedBy(syncLockFile)
            sourceSet.tasksProvider.initActions.executeAllOn(this)
            if (terraformExtension.showInitOutputInConsole)
                logging.captureStandardOutput(LogLevel.LIFECYCLE)
        }

    terraformInit { dependsOn(tfInit) }
    val tfShow: TaskProvider<TerraformShow> = tasks.terraformRegister("terraform${taskName}Show", sourceSet) {
        inputPlanFile = sourceSet.outputBinaryPlan
        outputJsonPlanFile = sourceSet.outputJsonPlan
        sourceSet.tasksProvider.showActions.executeAllOn(this)
    }
    terraformShow { dependsOn(tfShow) }
    val tfPlan: TaskProvider<TerraformPlan> = tasks.terraformRegister("terraform${taskName}Plan", sourceSet) {
        dependsOn(tfInit)
        outputPlanFile = sourceSet.outputBinaryPlan
        variables = sourceSet.planVariables
        finalizedBy(tfShow)
        sourceSet.tasksProvider.planActions.executeAllOn(this)
        if (terraformExtension.showPlanOutputInConsole)
            logging.captureStandardOutput(LogLevel.LIFECYCLE)
    }
    terraformPlan { dependsOn(tfPlan) }
    tfShow { dependsOn(tfPlan) }

    val tfApply: TaskProvider<TerraformApply> = tasks.terraformRegisterApply(
        "terraform${taskName}Apply",
        sourceSet,
        tfPlan,
        sourceSet.applySpec,
        sourceSet.outputBinaryPlan,
        sourceSet.tasksProvider.applyActions
    )
    terraformApply { dependsOn(tfApply) }

    sourceSet.outputTasks.forEach { task ->
        task.configure {
            dependsOn(tfInit)
            attachSourceSet(sourceSet)
            val fileName = variables.joinToString("") { it.toCamelCase().capitalize() }.decapitalize()
            val extension = when (format) {
                TerraformOutput.Format.JSON -> ".json"
                TerraformOutput.Format.RAW -> ".txt"
            }
            outputFile = file("${sourceSet.baseBuildDir}/outputs/$fileName.$extension")
        }
    }

    val tfDestroyShow: TaskProvider<TerraformShow> =
        tasks.terraformRegister("terraform${taskName}DestroyShow", sourceSet) {
            inputPlanFile = sourceSet.outputBinaryPlan
            outputJsonPlanFile = sourceSet.outputDestroyJsonPlan
            sourceSet.tasksProvider.destroyShowActions.executeAllOn(this)
        }
    terraformDestroyShow { dependsOn(tfDestroyShow) }
    val tfDestroyPlan: TaskProvider<TerraformPlan> =
        tasks.terraformRegister("terraform${taskName}DestroyPlan", sourceSet) {
            dependsOn(tfInit)
            outputPlanFile = sourceSet.outputDestroyBinaryPlan
            isDestroy = true
            variables = sourceSet.planVariables
            finalizedBy(tfDestroyShow)
            sourceSet.tasksProvider.destroyPlanActions.executeAllOn(this)
            if (terraformExtension.showPlanOutputInConsole)
                logging.captureStandardOutput(LogLevel.LIFECYCLE)
        }
    terraformDestroyPlan { dependsOn(tfDestroyPlan) }
    tfDestroyShow { dependsOn(tfDestroyPlan) }

    val tfDestroy: TaskProvider<TerraformApply> = tasks.terraformRegisterApply(
        "terraform${taskName}Destroy",
        sourceSet,
        tfDestroyPlan,
        sourceSet.destroySpec,
        sourceSet.outputDestroyBinaryPlan,
        sourceSet.tasksProvider.destroyActions
    )
    terraformDestroy { dependsOn(tfDestroy) }
}