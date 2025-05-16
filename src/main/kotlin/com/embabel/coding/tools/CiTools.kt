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
package com.embabel.coding.tools

import com.embabel.agent.api.common.SelfToolCallbackPublisher
import com.embabel.agent.api.common.SelfToolGroup
import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.agent.tools.DirectoryBased
import com.embabel.common.core.types.Semver
import org.springframework.ai.tool.annotation.Tool

/**
 * Interface for Continuous Integration tools that enable building and testing projects.
 *
 * The CiTools interface provides functionality for executing build commands within a project's
 * root directory. It serves as a bridge between the agent and the underlying build system,
 * allowing for seamless integration with various build tools like Maven, Gradle, npm, etc.
 *
 * This interface extends:
 * - [SelfToolCallbackPublisher]: Automatically publishes methods annotated with @Tool to be
 *   available as callable tools by the agent system. This enables the methods to be discovered
 *   and invoked through the tool callback mechanism.
 * - [DirectoryBased]: Provides access to the root directory on the host machine where the
 *   project is located. This allows build commands to be executed in the correct context.
 *
 * Implementations of this interface are expected to handle the execution of build commands
 * and process their output appropriately.
 *
 * @see SelfToolCallbackPublisher
 * @see DirectoryBased
 * @see Ci
 * @see BuildOptions
 */
interface CiTools : SelfToolCallbackPublisher, DirectoryBased {

    /**
     * Builds the project using the specified command.
     *
     * This method executes the provided build command in the project's root directory.
     * It leverages the [Ci] implementation to handle the actual command execution and
     * returns the output (stdout/stderr) from the build process.
     *
     * The method is annotated with @Tool to make it available as a callable tool
     * within the agent system.
     *
     * @param command The build command to execute in the project root (e.g., "mvn clean install",
     *                "gradle build", "npm run build", etc.)
     * @return The output of the build process as a string, containing both stdout and stderr
     * @throws RuntimeException if the build process fails or cannot be executed
     */
    @Tool(description = "build the project using the given command in the root")
    fun buildProject(command: String): String {
        return Ci(root).build(BuildOptions(command, true))
    }

    companion object {
        /**
         * Creates a new CiTools implementation as a ToolGroup.
         *
         * This factory method creates an anonymous implementation of the CiTools interface
         * that also implements SelfToolGroup. The implementation provides the necessary
         * configuration for the tool group, including:
         * - The root directory where build commands will be executed
         * - The artifact identifier for the tool group
         * - A description of the tool group's purpose
         * - The required permissions for the tool group to function
         *
         * The created tool group requires HOST_ACCESS permission to execute commands
         * on the host machine.
         *
         * @param root The root directory path where the project is located and where
         *             build commands will be executed
         * @return A fully configured ToolGroup implementation of CiTools ready for use
         *         in the agent system
         * @see ToolGroup
         * @see SelfToolGroup
         * @see ToolGroupPermission.HOST_ACCESS
         */
        fun toolGroup(root: String): ToolGroup = object : CiTools, SelfToolGroup {
            override val root: String = root
            override val provider: String = "embabel"
            override val name: String = "pluggable-ci"
            override val version = Semver(0, 1, 0)

            override val description
                get() = ToolGroup.CI_DESCRIPTION

            override val permissions get() = setOf(ToolGroupPermission.HOST_ACCESS)

        }
    }
}
