@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")

package org.jetbrains.gradle.plugins.liquibase

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.container
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.jetbrains.gradle.plugins.executeAllOn
import java.util.Properties

open class LiquibasePlugin : Plugin<Project> {

    companion object {

        const val LIQUIBASE_RUNTIME_CONFIGURATION_NAME = "liquibaseRuntime"

        private val commandParams = listOf(
            "excludeObjects",
            "includeObjects",
            "schemas",
            "snapshotFormat",
            "sql",
            "sqlFile",
            "delimiter",
            "rollbackScript"
        )
    }

    override fun apply(target: Project): Unit = with(target) {
        val activityContainer = container { Activity(it) }
        val liquibaseExtension = extensions.create<LiquibaseExtension>("liquibase")
        liquibaseExtension.extensions.add("activities", activityContainer)
        val liquibaseConfiguration = configurations.create(LIQUIBASE_RUNTIME_CONFIGURATION_NAME)
        afterEvaluate { configureTasks(activityContainer, liquibaseConfiguration) }
    }

    private fun Project.configureTasks(
        activities: NamedDomainObjectContainer<Activity>,
        liquibaseConfiguration: Configuration
    ) {
        LiquibaseCommand.values().forEach { liquibaseCommand ->
            val upperTask = tasks.create("liquibase${liquibaseCommand.command.capitalize()}") {
                group = "liquibase"
                description = liquibaseCommand.description
            }
            activities.forEach { activity: Activity ->
                val task = registerLiquibaseTask(activity, liquibaseConfiguration, liquibaseCommand)
                upperTask.dependsOn(task)
            }
        }
    }

    private fun Project.registerLiquibaseTask(
        activity: Activity,
        liquibaseConfiguration: Configuration,
        liquibaseCommand: LiquibaseCommand
    ) = tasks.register<JavaExec>("liquibase${activity.name.capitalize()}${liquibaseCommand.command.capitalize()}") {

        group = "liquibase"
        description = liquibaseCommand.description
        mainClass.set(activity.mainClassName)
        jvmArgs = activity.jvmArgs
        classpath(liquibaseConfiguration)

        args = buildList {
            activity.arguments.filterNot { it.key in commandParams }.map { "--${it.key}=${it.value}" }
                .takeIf { it.isNotEmpty() }
                ?.let { addAll(it) }
            add(liquibaseCommand.command)
            activity.arguments.filter { it.key in commandParams }.map { "--${it.key}=${it.value}" }
                .takeIf { it.isNotEmpty() }
                ?.let { addAll(it) }
            activity.parameters.map { "-D${it.key}=${it.value}" }
                .takeIf { it.isNotEmpty() }
                ?.let { addAll(it) }
            if (liquibaseCommand.requiresValue) {
                val value: String? = when {
                    properties.containsKey("liquibaseCommandValue") -> properties["liquibaseCommandValue"] as String
                    liquibaseCommand == LiquibaseCommand.DB_DOC -> project.file("${project.buildDir}/database/docs").absolutePath
                    else -> null
                }
                value?.let { add(it) }
            }
        }

        if (liquibaseCommand.requiresValue)
            doFirst {
                if (!properties.containsKey("liquibaseCommandValue"))
                    error("The Liquibase '${liquibaseCommand.command}' command requires a value")
            }

        activity.taskActionsMap[liquibaseCommand]?.executeAllOn(this)
    }
}

private fun Properties.toMap() = entries.associate { it.key.toString() to it.value.toString() }

open class Activity(val name: String) {

    val arguments: MutableMap<String, String?> = mutableMapOf("logLevel" to "info")
    val parameters: MutableMap<String, String?> = mutableMapOf()

    internal val taskActionsMap = mutableMapOf<LiquibaseCommand, MutableList<Action<JavaExec>>>()
        .withDefault { mutableListOf() }

    /**
     * Configures an [Action] that will be invoked against the task for
     * the given [command] for this Activity.
     */
    fun onCommand(command: LiquibaseCommand, action: Action<JavaExec>) {
        taskActionsMap.getValue(command).add(action)
    }

    /**
     * Configures an [Action] that will be invoked against all tasks for
     * this Activity.
     */
    fun onEachCommand(action: Action<JavaExec>) {
        LiquibaseCommand.values().forEach { onCommand(it, action) }
    }

    /**
     * Define the name of the Main class in Liquibase that the plugin should
     * call to run Liquibase itself.
     */
    var mainClassName = "liquibase.integration.commandline.Main"

    /**
     * Define the JVM arguments to use when running Liquibase.  This defaults
     * to an empty array, which is almost always what you want.
     */
    var jvmArgs = mutableListOf<String>()
}

abstract class LiquibaseExtension : ExtensionAware
