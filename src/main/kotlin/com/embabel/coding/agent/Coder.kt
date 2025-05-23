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

import com.embabel.agent.api.annotation.*
import com.embabel.agent.api.common.ActionContext
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.io.UserInput
import com.embabel.coding.domain.SoftwareProject
import com.embabel.coding.domain.SoftwareProjectRepository
import com.embabel.coding.tools.BuildResult
import com.embabel.common.util.time
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import java.time.Duration

data class CodeModificationRequest(
    @get:JsonPropertyDescription("Request to modify code")
    val request: String,
)


data class CodeModificationReport(
    @get:JsonPropertyDescription("Report of the modifications made to code")
    val text: String,
) : HasContent {
    override val content: String
        get() = TODO("Not yet implemented")
}

object CoderConditions {
    const val BUILD_NEEDED = "buildNeeded"
    const val BUILD_FAILED = "buildFailed"
    const val BUILD_SUCCEEDED = "buildSucceeded"
    const val BUILD_WAS_LAST_ACTION = "buildWasLastAction"
}

/**
 * Embabel coding agent.
 *
 * The Coder agent is responsible for modifying code in a software project based on user requests.
 * The agent flow is as follows:
 *
 * 1. loadExistingProject: Loads the project from the repository
 * 2. codeModificationRequestFromUserInput: Converts user input into a structured code modification request
 * 3. modifyCode: Makes the requested changes to the codebase
 * 4. build/buildWithCommand: Builds the project if needed (triggered by the BuildNeeded condition)
 * 5. fixBrokenBuild: If the build fails, attempts to fix the issues
 * 6. shareCodeModificationReport: Returns the final report of changes made
 *
 * The agent uses conditions to control the flow:
 * - BuildNeeded: Triggered after code modification to determine if a build is required
 * - BuildSucceeded/BuildFailed: Tracks build status
 * - BuildWasLastAction: Helps determine the next step in the flow
 */
@Agent(
    description = "Perform changes to a software project or directory structure",
)
@Profile("!test")
class Coder(
    private val softwareProjectRepository: SoftwareProjectRepository,
    private val coderProperties: CoderProperties,
) {

    private val logger = LoggerFactory.getLogger(Coder::class.java)

    /**
     * First step in the agent flow: Load the project to be modified
     * Retrieves the project from the repository using the default location
     */
    @Action
    fun loadExistingProject(): SoftwareProject? =
        // TODO need to select
        softwareProjectRepository.findAll().firstOrNull()


    /**
     * Converts raw user input into a structured code modification request
     * Uses GitHub tools to search for issues if the user references them
     */
    @Action(toolGroups = [CoreToolGroups.GITHUB])
    fun codeModificationRequestFromUserInput(
        project: SoftwareProject,
        userInput: UserInput
    ): CodeModificationRequest = using(llm = coderProperties.primaryCodingLlm).create(
        """
        Create a CodeModification request based on this user input: ${userInput.content}
        If the user wants you to pick up an issue from GitHub, search for it at ${project.url}.
        Search for the milestone the user suggests.
        Use the GitHub tools.
        Create a CodeModificationRequest from the issue.
        """.trimIndent()
    )


    /**
     * The LLM will determine the command to use to build the project.
     * Only use as a last resort, so we mark it as expensive.
     *
     * This is a fallback build method when the standard build method isn't sufficient
     * Triggered by the BuildNeeded condition after code modifications
     */
    @Action(
        cost = 10000.0,
        canRerun = true,
        pre = [CoderConditions.BUILD_NEEDED],
        post = [CoderConditions.BUILD_SUCCEEDED],
    )
    fun buildWithCommand(
        project: SoftwareProject,
        context: ActionContext,
    ): BuildResult {
        val (rawOutput, ms) = time {
            context.promptRunner(
                llm = coderProperties.primaryCodingLlm,
                promptContributors = listOf(project),
            ).generateText("Build the project")
        }
        return project.ci.parseBuildOutput(rawOutput, Duration.ofMillis(ms))
    }

    /**
     * Standard build method with lower cost than buildWithCommand
     * Triggered by the BuildNeeded condition after code modifications
     */
    @Action(
        cost = 500.0,
        canRerun = true,
        pre = [CoderConditions.BUILD_NEEDED],
        post = [CoderConditions.BUILD_SUCCEEDED],
    )
    fun build(
        project: SoftwareProject,
    ): BuildResult = project.build()

    /**
     * Condition that determines if a build is needed
     * Triggered when the last action was a code modification
     */
    @Condition(name = CoderConditions.BUILD_NEEDED)
    fun buildNeeded(context: OperationContext): Boolean =
        context.lastResult() is CodeModificationReport

    /**
     * Condition that checks if the last action was a build
     * Used to determine the next step in the flow
     */
    @Condition(name = CoderConditions.BUILD_WAS_LAST_ACTION)
    fun buildWasLastAction(context: OperationContext): Boolean =
        context.lastResult() is BuildResult

    /**
     * Condition that checks if the build was successful
     * Used to determine if the agent should proceed to sharing the report
     */
    @Condition(name = CoderConditions.BUILD_SUCCEEDED)
    internal fun buildSucceeded(buildResult: BuildResult): Boolean = buildResult.status?.success == true

    /**
     * Condition that checks if the build failed
     * Used to determine if the agent should attempt to fix the build
     */
    @Condition(name = CoderConditions.BUILD_FAILED)
    fun buildFailed(buildResult: BuildResult): Boolean = buildResult.status?.success == false

    /**
     * Core action that modifies code based on the user request
     * Sets the BuildNeeded condition after completion
     */
    @Action(
        canRerun = true,
        post = [CoderConditions.BUILD_NEEDED],
        toolGroups = [
//            ToolGroup.GITHUB,
            CoreToolGroups.WEB
        ]
    )
    fun modifyCode(
        codeModificationRequest: CodeModificationRequest,
        project: SoftwareProject,
        context: ActionContext,
    ): CodeModificationReport {
        logger.info("✎ Modifying code according to request: ${codeModificationRequest.request}")
        val report: String = context.promptRunner(
            llm = coderProperties.primaryCodingLlm,
            promptContributors = listOf(project),
        ).create(
            """
            Execute the following user request to modify code in the given project.
            Use the file tools to read code and directories on the local system, not GitHub.
            Use the project information to help you understand the code.
            The project will be in git so you can safely modify content without worrying about backups.
            Return an explanation of what you did and why.

            DO NOT ASK FOR USER INPUT: DO WHAT YOU THINK IS NEEDED TO MODIFY THE PROJECT.

            Use the web tools if you are asked to use a technology you don't know about.
            ALWAYS LOOK FOR THE FILES IN THE PROJECT LOCALLY USING FILE TOOLS, NOT THE WEB.

            DO NOT BUILD THE PROJECT UNLESS THE USER HAS REQUESTED IT
            AND IT IS NECESSARY TO DECIDE WHAT TO MODIFY.
            IF BUILDING IS NEEDED, BE SURE TO RUN UNIT TESTS.
            DO NOT BUILD *AFTER* MODIFYING CODE.

            Make multiple small, focused edits using the editFile tool.

            User request:
            "${codeModificationRequest.request}"
            }
            """.trimIndent(),
        )
        return CodeModificationReport(report)
    }

    /**
     * Action to fix a broken build
     * Triggered when the build fails after code modifications
     * Uses a specialized LLM (fixCodingLlm) to address build failures
     */
    @Action(
        canRerun = true,
        pre = [CoderConditions.BUILD_FAILED, CoderConditions.BUILD_WAS_LAST_ACTION],
        post = [CoderConditions.BUILD_SUCCEEDED],
        toolGroups = [CoreToolGroups.WEB],
    )
    fun fixBrokenBuild(
        codeModificationRequest: CodeModificationRequest,
        project: SoftwareProject,
        buildFailure: BuildResult,
        context: ActionContext,
    ): CodeModificationReport {
        val report: String = context.promptRunner(
            llm = coderProperties.fixCodingLlm,
            promptContributors = listOf(project, buildFailure),
        ).create(
            """
            Modify code in the given project to fix the broken build.
            Use the file tools to read code and directories.
            Use the project information to help you understand the code.
            The project will be in git so you can safely modify content without worrying about backups.
            Return an explanation of what you did and why.
            Consider the build failure report.

            DO NOT BUILD THE PROJECT. JUST MODIFY CODE.
            Consider the following user request for the necessary functionality:
            "${codeModificationRequest.request}"
            """.trimIndent(),
        )
        return CodeModificationReport(report)
    }

    /**
     * Final step in the agent flow
     * Returns the code modification report to the user
     * Only triggered when the build is successful (or not needed)
     */
    @Action(pre = [CoderConditions.BUILD_SUCCEEDED])
    @AchievesGoal(description = "Modify project code as per user request")
    fun shareCodeModificationReport(codeModificationReport: CodeModificationReport) = codeModificationReport

}
