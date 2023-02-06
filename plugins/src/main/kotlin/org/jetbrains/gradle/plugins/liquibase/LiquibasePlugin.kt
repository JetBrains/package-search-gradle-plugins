@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")

package org.jetbrains.gradle.plugins.liquibase

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.container
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
import org.jetbrains.gradle.plugins.executeAllOn
import org.jetbrains.gradle.plugins.maybeRegisterTask
import org.jetbrains.gradle.plugins.toCamelCase

open class LiquibasePlugin : Plugin<Project> {

    companion object {

        const val LIQUIBASE_RUNTIME_CONFIGURATION_NAME = "liquibaseRuntime"
    }

    override fun apply(target: Project): Unit = with(target) {
        val activityContainer = container { Activity(it) }
        val liquibaseExtension = extensions.create<LiquibaseExtension>("liquibase")
        liquibaseExtension.extensions.add("activities", activityContainer)
        val liquibaseConfiguration = configurations.create(LIQUIBASE_RUNTIME_CONFIGURATION_NAME) {
            isCanBeResolved = true
            isCanBeConsumed = false
        }
        afterEvaluate { configureTasks(activityContainer, liquibaseConfiguration) }
    }

    private fun Project.configureTasks(
        activities: NamedDomainObjectContainer<Activity>,
        liquibaseConfiguration: Configuration
    ) {
        activities.forEach { activity: Activity ->

            val generatePropertiesFileTask =
                tasks.register<LiquibaseProperties>("generate${activity.name.capitalize()}PropertiesFileTask") {
                    outputPropertiesFile.set(file("$buildDir/properties/${activity.name.decapitalize()}.properties"))
                    activity.propertiesActions.executeAllOn(this)
                }

            LiquibaseCommand.values().forEach { liquibaseCommand ->
                val upperTask = tasks.maybeRegisterTask("liquibase${liquibaseCommand.command.capitalize()}") {
                    group = "liquibase"
                    description = liquibaseCommand.description
                }
                val task = registerLiquibaseTask(
                    activity,
                    liquibaseConfiguration,
                    liquibaseCommand,
                    generatePropertiesFileTask
                )
                upperTask { dependsOn(task) }
            }
        }
    }

    private fun Project.registerLiquibaseTask(
        activity: Activity,
        liquibaseConfiguration: Configuration,
        liquibaseCommand: LiquibaseCommand,
        generatePropertiesFileTask: TaskProvider<LiquibaseProperties>
    ) = tasks.register<JavaExec>(
        "liquibase${activity.name.toCamelCase().capitalize()}${liquibaseCommand.command.capitalize()}"
    ) {

        dependsOn(generatePropertiesFileTask)

        group = "liquibase"
        description = liquibaseCommand.description
        mainClass.set(activity.mainClassName)
        jvmArgs = activity.jvmArgs
        classpath(liquibaseConfiguration)

        val value: String? = when {
            properties.containsKey("liquibaseCommandValue") -> properties["liquibaseCommandValue"] as String
            liquibaseCommand == LiquibaseCommand.DB_DOC -> file("${project.project.buildDir}/database/docs").absolutePath
            else -> null
        }

        args = buildList {
            add("--defaultsFile=${generatePropertiesFileTask.get().outputPropertiesFile.get().absolutePath}")
            add(liquibaseCommand.command)
            value?.let { add(it) }
        }

        if (liquibaseCommand.requiresValue)
            doFirst {
                if (!properties.containsKey("liquibaseCommandValue"))
                    error("The Liquibase '${liquibaseCommand.command}' command requires a value")
            }

        activity.taskActionsMap[liquibaseCommand]?.executeAllOn(this)
    }
}

abstract class LiquibaseExtension : ExtensionAware

abstract class GenerateLiquibasePropertiesFile : DefaultTask()