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

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.AgentCapabilities
import com.embabel.agent.api.annotation.fromForm
import com.embabel.agent.api.common.OperationContext
import com.embabel.coding.domain.SoftwareProject
import com.embabel.coding.domain.TaskFocus
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AgentRequirements(
    val projectName: String = "demo",
    val groupId: String = "com.example",
    val packageName: String,
//    val artifactId: String = "demo",
    val language: String = "kotlin",
    val requirements: String,
)


@AgentCapabilities(
    scan = true
)
@Profile("!test")
class AgentProjectCreator(
    private val coderProperties: CoderProperties,
    private val taskFocus: TaskFocus,
    properties: CoderProperties,
) {

    private val logger = LoggerFactory.getLogger(AgentProjectCreator::class.java)

    @Action
    fun askUserForAgentRequirements(): AgentRequirements =
        fromForm("Agent requirements")

    @Action(post = [CoderConditions.BUILD_NEEDED])
    fun createAgentProject(
        requirements: AgentRequirements,
        context: OperationContext,
    ): SoftwareProject {
        logger.info("Creating Agent project named {}", requirements.projectName)

        val workDir = coderProperties.root
        val newAgentProject = createProject(workDir, requirements)
        taskFocus.saveAndSwitch(newAgentProject)
        return newAgentProject
    }

    @Action(
        pre = [
            CoderConditions.BUILD_SUCCEEDED],
    )
    @AchievesGoal(description = "Create a new Embabel agent project")
    fun describeShinyNewAgentProject(
        softwareProject: SoftwareProject,
        agentRequirements: AgentRequirements,
    ): CodeExplanation =
        CodeExplanation(
            text = """
                Project root: ${softwareProject.root}
                Technologies used: ${softwareProject.tech}
                Coding style: ${softwareProject.codingStyle}
            """.trimIndent(),
            links = emptyList(),
        )

    /**
     * Create a project under the working directory with the given requirements.
     */
    private fun createProject(workingDir: File, requirements: AgentRequirements): SoftwareProject {
        logger.info("Creating project under {}", workingDir.absolutePath)
        ProjectCreator().invokeProjectCreatorWithArgs(
            workingDirectory = workingDir,
            additionalArgs = listOf(
                "--repo", if (requirements.language == "kotlin") "1" else "2",
                "--project-name", requirements.projectName,
                "--package", requirements.packageName,
            )
        )
        val projectDir = File(workingDir, requirements.projectName)

        return SoftwareProject(
            root = projectDir.absolutePath,
            tech = "Kotlin, Spring Boot, Maven, Spring Web, Spring Actuator, Spring DevTools",
            defaultCodingStyle = "Modern Kotlin with Spring Boot conventions. Clean architecture with separation of concerns.",
            buildCommand = "mvn test",
            wasCreated = true,
        )

    }
}

class ProjectCreator {

    /**
     * Invokes the project-creator tool with additional arguments
     *
     * @param workingDirectory The directory where the command should be executed
     * @param additionalArgs Additional arguments to pass to project-creator
     * @param timeoutSeconds Maximum time to wait for command completion
     * @return ProcessResult containing exit code, stdout, and stderr
     */
    fun invokeProjectCreatorWithArgs(
        workingDirectory: File,
        additionalArgs: List<String>,
        timeoutSeconds: Long = 300
    ): ProcessResult {
        if (!workingDirectory.exists()) {
            throw IllegalArgumentException("Directory does not exist: ${workingDirectory.absolutePath}")
        }
        if (!workingDirectory.isDirectory) {
            throw IllegalArgumentException("Path is not a directory: ${workingDirectory.absolutePath}")
        }

        val command = mutableListOf(
            "uvx",
            "--from",
            "git+https://github.com/embabel/project-creator.git",
            "project-creator"
        )
        command.addAll(additionalArgs)

        return executeCommand(command, workingDirectory, timeoutSeconds)
    }

    private fun executeCommand(
        command: List<String>,
        workingDirectory: File,
        timeoutSeconds: Long
    ): ProcessResult {
        try {
            val processBuilder = ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectErrorStream(false)

            val process = processBuilder.start()

            // Capture stdout and stderr
            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }

            // Wait for process completion with timeout
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

            if (!finished) {
                process.destroyForcibly()
                throw IOException("Command timed out after $timeoutSeconds seconds")
            }

            return ProcessResult(
                exitCode = process.exitValue(),
                stdout = stdout,
                stderr = stderr
            )
        } catch (e: Exception) {
            throw IOException("Failed to execute command: ${command.joinToString(" ")}", e)
        }
    }
}

/**
 * Data class to hold the result of process execution
 */
data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean
        get() = exitCode == 0
}
