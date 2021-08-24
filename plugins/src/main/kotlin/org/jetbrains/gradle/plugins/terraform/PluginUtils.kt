package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RelativePath
import org.gradle.api.logging.LogLevel
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.*
import org.jetbrains.gradle.plugins.terraform.tasks.*

internal fun Project.setupTerraformRepository() {
    repositories {
        ivy {
            name = "Terraform Executable Repository"
            url = uri("https://releases.hashicorp.com/terraform/")
            patternLayout {
                artifact("[revision]/[artifact]_[revision]_[classifier].zip")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("hashicorp", "terraform")
            }
        }
    }
}

internal fun Project.registerLifecycleTasks(): List<Task> {
    val terraformInit by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformShow by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformDestroyShow by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformPlan by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
        dependsOn(terraformInit)
        finalizedBy(terraformShow)
    }
    terraformShow.dependsOn(terraformPlan)
    val terraformDestroyPlan by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
        dependsOn(terraformInit)
        finalizedBy(terraformDestroyShow)
    }
    val terraformApply by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformDestroy by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
    }
    return listOf(
        terraformInit, terraformShow, terraformDestroyShow,
        terraformPlan, terraformDestroyPlan, terraformApply, terraformDestroy
    )
}

internal fun Project.createConfigurations(): List<Configuration> {
    val lambda: Configuration by configurations.creating {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
        }
    }
    val terraformApi by configurations.maybeCreating {
        isCanBeConsumed = true
    }
    val terraformImplementation by configurations.maybeCreating {
        isCanBeConsumed = true
        extendsFrom(terraformApi)
    }
    return listOf(lambda, terraformImplementation, terraformApi)
}

internal fun Project.createExtension(): Triple<TerraformExtension, NamedDomainObjectContainer<TerraformSourceSet>, TerraformSourceSet> {
    val terraformExtension = extensions.create<TerraformExtension>(
        TerraformPlugin.TERRAFORM_EXTENSION_NAME,
        project,
        TerraformPlugin.TERRAFORM_EXTENSION_NAME
    )

    val sourceSets =
        container { name -> TerraformSourceSet(project, name.toCamelCase()) }

    terraformExtension.extensions.add("sourceSets", sourceSets)

    return Triple(terraformExtension, sourceSets, sourceSets.create("main"))
}

internal inline fun <reified T : AbstractTerraformExec> TaskContainer.terraformRegister(
    name: String,
    sourceSet: TerraformSourceSet,
    crossinline action: T.() -> Unit
) = register<T>(name) {
    sourcesDirectory = sourceSet.runtimeExecutionDirectory
    dataDir = sourceSet.dataDir
    action()
}

internal fun Project.elaborateSourceSet(
    sourceSet: TerraformSourceSet,
    terraformApi: Configuration,
    terraformImplementation: Configuration,
    lambdaConfiguration: Configuration,
    terraformExtract: TaskProvider<TerraformExtract>,
    terraformExtension: TerraformExtension,
    terraformInit: Task,
    terraformShow: Task,
    terraformPlan: Task,
    terraformApply: Task,
    terraformDestroyShow: Task,
    terraformDestroyPlan: Task,
    terraformDestroy: Task,
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
        from(lambdaConfiguration) {
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

    val terraformOutgoingElements =
        configurations.create("terraform${taskName}OutgoingElements") {
            isCanBeConsumed = true
            extendsFrom(terraformApi)
            outgoing.artifact(terraformModuleZip)
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(TerraformPlugin.Attributes.USAGE))
                attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(TerraformPlugin.Attributes.LIBRARY_ELEMENTS)
                )
                attribute(TerraformPlugin.Attributes.SOURCE_SET_NAME_ATTRIBUTE, sourceSet.name)
            }
        }

    val component = softwareComponentFactory.adhoc(buildString {
        append("terraform")
        if (sourceSet.name != "main") append(taskName)
    })

    components.add(component)

    component.addVariantsFromConfiguration(terraformOutgoingElements) {
        mapToMavenScope("runtime")
    }

    val publishingConfigurationAction = {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("terraform$taskName") {
                    groupId = project.group.toString()
                    artifactId = buildString {
                        append(project.name)
                        if (sourceSet.name != "main") append("-${sourceSet.name}")
                    }
                    version = project.version.toString()

                    from(component)
                }
            }
        }
    }

    if (project.plugins.has<MavenPublishPlugin>()) publishingConfigurationAction()
    else afterEvaluate { if (project.plugins.has<MavenPublishPlugin>()) publishingConfigurationAction() }

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
                attribute(TerraformPlugin.Attributes.SOURCE_SET_NAME_ATTRIBUTE, sourceSet.name)
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

    if (plugins.has<DistributionPlugin>()) {
        extensions.configure<DistributionContainer> {
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

    terraformInit.dependsOn(tfInit)
    val tfShow: TaskProvider<TerraformShow> = tasks.terraformRegister("terraform${taskName}Show", sourceSet) {
        inputPlanFile = sourceSet.outputBinaryPlan
        outputJsonPlanFile = sourceSet.outputJsonPlan
        sourceSet.tasksProvider.showActions.executeAllOn(this)
    }
    terraformShow.dependsOn(terraformShow)
    val tfPlan: TaskProvider<TerraformPlan> = tasks.terraformRegister("terraform${taskName}Plan", sourceSet) {
        dependsOn(tfInit)
        outputPlanFile = sourceSet.outputBinaryPlan
        variables = sourceSet.planVariables
        finalizedBy(tfShow)
        sourceSet.tasksProvider.planActions.executeAllOn(this)
        if (terraformExtension.showPlanOutputInConsole)
            logging.captureStandardOutput(LogLevel.LIFECYCLE)
    }
    terraformPlan.dependsOn(tfPlan)
    tfShow { dependsOn(tfPlan) }

    val tfApply: TaskProvider<TerraformApply> = tasks.terraformRegister("terraform${taskName}Apply", sourceSet) {
        dependsOn(tfPlan)
        planFile = sourceSet.outputBinaryPlan
        onlyIf {
            val canExecuteApply = terraformExtension.applySpec.isSatisfiedBy(this)
            if (!canExecuteApply) logger.warn(
                "Cannot execute $name. Please check " +
                        "your terraform extension in the script."
            )
            canExecuteApply
        }
        sourceSet.tasksProvider.applyActions.executeAllOn(this)
    }
    terraformApply.dependsOn(tfApply)

    val tfDestroyShow: TaskProvider<TerraformShow> =
        tasks.terraformRegister("terraform${taskName}DestroyShow", sourceSet) {
            inputPlanFile = sourceSet.runtimeExecutionDirectory
            outputJsonPlanFile = sourceSet.outputDestroyJsonPlan
            sourceSet.tasksProvider.destroyShowActions.executeAllOn(this)
        }
    terraformDestroyShow.dependsOn(tfDestroyShow)
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
    terraformDestroyPlan.dependsOn(tfDestroyPlan)
    tfDestroyShow { dependsOn(tfDestroyPlan) }

    val tfDestroy: TaskProvider<TerraformApply> = tasks.terraformRegister("terraform${taskName}Destroy", sourceSet) {
        dependsOn(tfDestroyPlan)
        planFile = sourceSet.outputDestroyBinaryPlan
        onlyIf {
            val canExecuteApply = terraformExtension.destroySpec.isSatisfiedBy(this)
            if (!canExecuteApply) logger.warn(
                "Cannot execute $name. Please check " +
                        "your terraform extension in the script."
            )
            canExecuteApply
        }
        sourceSet.tasksProvider.destroyActions.executeAllOn(this)
    }
    terraformDestroy.dependsOn(tfDestroy)
}