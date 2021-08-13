package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.RelativePath
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.*
import org.jetbrains.gradle.plugins.executeAllOn
import org.jetbrains.gradle.plugins.maybeCreating
import org.jetbrains.gradle.plugins.terraform.tasks.*
import org.jetbrains.gradle.plugins.toCamelCase
import java.io.File
import javax.inject.Inject

open class TerraformPlugin @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory
) : Plugin<Project> {

    object Attributes {
        const val USAGE = "terraform"
        const val LIBRARY_ELEMENTS = "ZIP_ARCHIVE"

        val SOURCE_SET_NAME_ATTRIBUTE: Attribute<String> = Attribute.of("terraform.sourceset.name", String::class.java)
    }

    companion object {

        const val TERRAFORM_EXTRACT_TASK_NAME = "terraformExtract"
        const val TERRAFORM_EXTENSION_NAME = "terraform"
        const val TASK_GROUP = "terraform"
    }

    override fun apply(target: Project): Unit = with(target) {

        setupTerraformRepository()

        val (lambda, terraformImplementation, terraformApi) = createConfigurations()

        val (terraformExtension, sourceSets) = createExtension()

        val (terraformInit, terraformShow, terraformDestroyShow,
            terraformPlan, terraformDestroyPlan, terraformApply,
            terraformDestroy) = registerLifecycleTasks()

        afterEvaluate {
            val copyLambdas by tasks.registering(Sync::class) {
                from(lambda)
                into(terraformExtension.lambdasDirectory)
            }
            tasks.create<TerraformExtract>(TERRAFORM_EXTRACT_TASK_NAME) {
                configuration = generateTerraformDetachedConfiguration(terraformExtension.version)
                val executableName = evaluateTerraformName(terraformExtension.version)
                outputExecutable = File(buildDir, "terraform/$executableName")
            }
            sourceSets.forEach { sourceSet: TerraformSourceSet ->
                val taskName = sourceSet.name.capitalize()
                val createSourcesZip = tasks.create<Zip>("${sourceSet.name}Zip") {
                    from(listOf(sourceSet.srcDirs, sourceSet.getSourceDependencies())) {
                        include { it.name.endsWith(".tf") }
                        eachFile {
                            relativePath = RelativePath(true,
                                buildString {
                                    append(project.name)
                                    if (sourceSet.name != "main") append("-${sourceSet.name}")
                                },
                                *relativePath.segments
                            )
                        }
                    }
                    archiveFileName.set("terraform${taskName}.zip")
                    destinationDirectory.set(file("$buildDir/terraform/archives"))
                }

                val terraformOutgoingElements =
                    configurations.create("terraform${taskName}OutgoingElements") {
                        isCanBeConsumed = true
                        extendsFrom(terraformApi)
                        outgoing.artifact(createSourcesZip)
                        attributes {
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Attributes.USAGE))
                            attribute(
                                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                objects.named(Attributes.LIBRARY_ELEMENTS)
                            )
                            attribute(Attributes.SOURCE_SET_NAME_ATTRIBUTE, sourceSet.name)
                        }
                    }

                val component = softwareComponentFactory.adhoc(buildString {
                    append("terraform")
                    if (sourceSet.name != "main") append(taskName)
                })

                components.add(component)

                component.addVariantsFromConfiguration(terraformOutgoingElements) {
                    mapToMavenScope("compile")
                }

                if (project.plugins.has<MavenPublishPlugin>()) {
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

                val terraformRuntimeElements =
                    configurations.create("terraform${taskName}RuntimeElements") {
                        isCanBeResolved = true
                        isCanBeConsumed = false
                        extendsFrom(terraformImplementation)
                        attributes {
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Attributes.USAGE))
                            attribute(
                                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                objects.named(Attributes.LIBRARY_ELEMENTS)
                            )
                            attribute(Attributes.SOURCE_SET_NAME_ATTRIBUTE, sourceSet.name)
                        }
                    }

                val runtimeDirectory = File(sourceSet.baseBuildDir, "runtimeElements")

                val copyExecutionContext = tasks.create<Sync>("copy${taskName}ExecutionContext") {
                    from(terraformRuntimeElements.resolve().map { zipTree(it) }, zipTree(createSourcesZip.archiveFile))
                    from(sourceSet.lockFile)
                    into(runtimeDirectory)
                }

                val tfInit: TaskProvider<TerraformInit> =
                    tasks.register<TerraformInit>("terraform${taskName}Init") {
                        dependsOn(copyExecutionContext)
                        sourcesDirectory = runtimeDirectory
                        dataDir = sourceSet.dataDir
                        doLast {
                            val localLockFile = runtimeDirectory.resolve(".terraform.lock.hcl")
                            if (localLockFile.exists() && localLockFile.isFile) {
                                localLockFile.copyTo(sourceSet.lockFile, true)
                            }
                        }
                        sourceSet.tasksProvider.initActions.executeAllOn(this)
                    }
                terraformInit.dependsOn(tfInit)
                val tfShow = tasks.create<TerraformShow>("terraform${taskName}Show") {
                    sourcesDirectory = runtimeDirectory
                    dataDir = sourceSet.dataDir
                    inputPlanFile = sourceSet.outputBinaryPlan
                    outputJsonPlanFile = sourceSet.outputJsonPlan
                    sourceSet.tasksProvider.showActions.executeAllOn(this)
                }
                terraformShow.dependsOn(terraformShow)
                val tfPlan = tasks.register<TerraformPlan>("terraform${taskName}Plan") {
                    dependsOn(tfInit, copyLambdas)
                    sourcesDirectory = runtimeDirectory
                    dataDir = sourceSet.dataDir
                    outputPlanFile = sourceSet.outputBinaryPlan
                    variables = sourceSet.planVariables
                    finalizedBy(tfShow)
                    sourceSet.tasksProvider.planActions.executeAllOn(this)
                }
                terraformPlan.dependsOn(tfPlan)
                tfShow.dependsOn(tfPlan)

                val tfApply = tasks.register<TerraformApply>("terraform${taskName}Apply") {
                    dependsOn(tfPlan)
                    sourcesDirectory = runtimeDirectory
                    dataDir = sourceSet.dataDir
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

                val tfDestroyShow = tasks.create<TerraformShow>("terraform${taskName}DestroyShow") {
                    inputPlanFile = runtimeDirectory
                    dataDir = sourceSet.dataDir
                    sourcesDirectory = sourceSet.srcDirs.first()
                    outputJsonPlanFile = sourceSet.outputDestroyJsonPlan
                    sourceSet.tasksProvider.destroyShowActions.executeAllOn(this)
                }
                terraformDestroyShow.dependsOn(tfDestroyShow)
                val tfDestroyPlan = tasks.register<TerraformPlan>("terraform${taskName}DestroyPlan") {
                    dependsOn(tfInit, copyLambdas)
                    sourcesDirectory = runtimeDirectory
                    dataDir = sourceSet.dataDir
                    outputPlanFile = sourceSet.outputDestroyBinaryPlan
                    isDestroy = true
                    variables = sourceSet.planVariables
                    finalizedBy(tfDestroyShow)
                    sourceSet.tasksProvider.destroyPlanActions.executeAllOn(this)
                }
                terraformDestroyPlan.dependsOn(tfDestroyPlan)
                tfDestroyShow.dependsOn(tfDestroyPlan)

                val tfDestroy = tasks.register<TerraformApply>("terraform${taskName}Destroy") {
                    dependsOn(tfDestroyPlan)
                    sourcesDirectory = runtimeDirectory
                    dataDir = sourceSet.dataDir
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

        }
    }

    private fun Project.setupTerraformRepository() {
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

    private fun Project.registerLifecycleTasks(): List<Task> {
        val terraformInit by tasks.creating {
            group = TASK_GROUP
        }
        val terraformShow by tasks.creating {
            group = TASK_GROUP
        }
        val terraformDestroyShow by tasks.creating {
            group = TASK_GROUP
        }
        val terraformPlan by tasks.creating {
            group = TASK_GROUP
            dependsOn(terraformInit)
            finalizedBy(terraformShow)
        }
        terraformShow.dependsOn(terraformPlan)
        val terraformDestroyPlan by tasks.creating {
            group = TASK_GROUP
            dependsOn(terraformInit)
            finalizedBy(terraformDestroyShow)
        }
        val terraformApply by tasks.creating {
            group = TASK_GROUP
        }
        val terraformDestroy by tasks.creating {
            group = TASK_GROUP
        }
        return listOf(
            terraformInit, terraformShow, terraformDestroyShow,
            terraformPlan, terraformDestroyPlan, terraformApply, terraformDestroy
        )
    }

    private fun Project.createConfigurations(): List<Configuration> {
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

    private fun Project.createExtension(): Pair<TerraformExtension, NamedDomainObjectContainer<TerraformSourceSet>> {
        val terraformExtension = extensions.create<TerraformExtension>(
            TERRAFORM_EXTENSION_NAME,
            project,
            TERRAFORM_EXTENSION_NAME
        )

        val sourceSets =
            container { name -> TerraformSourceSet(project, name.toCamelCase()) }

        terraformExtension.extensions.add("sourceSets", sourceSets)

        sourceSets.create("main")
        return terraformExtension to sourceSets
    }
}
