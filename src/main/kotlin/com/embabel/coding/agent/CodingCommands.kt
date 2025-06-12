/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
