package org.jetbrains.gradle.plugins.liquibase

import org.gradle.api.Action
import org.gradle.api.tasks.JavaExec

open class Activity(val name: String) {

    internal val taskActionsMap =
        mutableMapOf<LiquibaseCommand, MutableList<Action<JavaExec>>>()
            .withDefault { mutableListOf() }

    internal val propertiesActions = mutableListOf<Action<LiquibaseProperties>>()

    fun properties(action: Action<LiquibaseProperties>) = propertiesActions.add(action)

    /**
     * Configures an [Action] that will be invoked against the task for
     * the given [command] for this Activity.
     */
    fun onCommand(command: LiquibaseCommand, action: Action<JavaExec>) {
        taskActionsMap.getOrPut(command) { mutableListOf() }.add(action)
    }

    /**
     * Configures an [Action] that will be invoked against all tasks for
     * this Activity.
     */
    fun onEachCommand(action: JavaExec.(LiquibaseCommand) -> Unit) {
        LiquibaseCommand.values().forEach { onCommand(it) { action(it) } }
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