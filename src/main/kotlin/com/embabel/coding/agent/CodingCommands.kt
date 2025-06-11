package com.embabel.coding.agent

import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class CodingCommands(private val taskFocus: TaskFocus) {

    @ShellMethod("Get current task focus")
    fun focus(): String {
        return taskFocus.softwareProject?.root ?: "No project is currently focused."
    }

    @ShellMethod("Get current task focus")
    fun setFocus(@ShellOption name: String): String {
        return taskFocus.setFocus(name)?.root ?: "No project found with name containing '$name'."
    }
}