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
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.core.count
import com.embabel.agent.domain.io.UserInput
import com.embabel.chat.Conversation
import com.embabel.coding.domain.*
import com.embabel.coding.tools.BuildResult
import com.embabel.common.util.time
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import java.time.Duration


/**
 * Don't be stringly typed.
 */
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
    private val taskFocus: TaskFocus,
    private val coderProperties: CoderProperties,
    private val logWriter: LogWriter,
) {

    private val logger = LoggerFactory.getLogger(Coder::class.java)

    @Action
    fun loadExistingProject(): SoftwareProject? {
        logger.info("Working on project {}", taskFocus.softwareProject?.root ?: "None")
        return taskFocus.softwareProject
    }

    /**
     * Converts raw user input into a structured code modification request
     * Uses GitHub tools to search for issues if the user references them
     */
    @Action(
//        toolGroups = [CoreToolGroups.GITHUB]
    )
    fun codeModificationRequestFromUserInput(
        project: SoftwareProject,
        userInput: UserInput,
        conversation: Conversation? = null,
    ): CodeModificationRequest = using(llm = coderProperties.primaryCodingLlm)
        .withPromptContributors(listOfNotNull(conversation?.promptContributor()))
        .create(
            """
            Create a CodeModification request based on this user input: ${userInput.content}
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
        context: OperationContext,
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
    fun buildNeeded(context: OperationContext): Boolean {
        val last = context.lastResult()
        return last is CodeModificationReport || (
                last is SoftwareProject && last.wasCreated)
    }

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
    fun modifyProject(
        codeModificationRequest: CodeModificationRequest,
        project: SoftwareProject,
        context: OperationContext,
        conversation: Conversation?,
    ): CodeModificationReport {
        logger.info("✎ Modifying code according to request: ${codeModificationRequest.request}")
        val isFirstModification = context.count<CodeModificationRequest>() == 1
        if (isFirstModification) {
            logWriter.logRequest(codeModificationRequest, project)
        }
        project.flushChanges()
        // SoftwareProject tools are automatically added because it's a parameter to this function
        val report: String = context.promptRunner(
            llm = coderProperties.primaryCodingLlm,
            promptContributors = listOfNotNull(
                project,
                coderProperties.codeModificationDirections(),
                conversation?.promptContributor(),
            ),
        ).create(
            """
            Execute the following user request to modify code in the given project.
            Use the project information to help you understand the code.
            The project will be in git so you can safely modify content without worrying about backups.
            Return an explanation of what you did and why.

            DO NOT ASK FOR USER INPUT: DO WHAT YOU THINK IS NEEDED TO MODIFY THE PROJECT.

            DO NOT BUILD THE PROJECT UNLESS THE USER HAS REQUESTED IT
            AND IT IS NECESSARY TO DECIDE WHAT TO MODIFY.
            IF BUILDING IS NEEDED, BE SURE TO RUN UNIT TESTS.
            DO NOT BUILD *AFTER* MODIFYING CODE.

            User request:
            "${codeModificationRequest.request}"
            }
            """.trimIndent(),
        )
        return CodeModificationReport(
            text = report,
            filesChanged = project.getChanges().map { it.path }.toList()
        )
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
        conversation: Conversation?,
        context: OperationContext,
    ): CodeModificationReport {
        project.flushChanges()
        val report: String = context.promptRunner(
            llm = coderProperties.fixCodingLlm,
            promptContributors = listOfNotNull(
                project,
                buildFailure,
                coderProperties.codeModificationDirections(),
                conversation?.promptContributor(),
            ),
        ).create(
            """
            Modify code in the given project to fix the broken build.

            Use the project information to help you understand the code.
            The project will be in git so you can safely modify content without worrying about backups.
            Return an explanation of what you did and why.
            Consider the build failure report.

            DO NOT BUILD THE PROJECT. JUST MODIFY CODE.
            Consider the following user request for the necessary functionality:
            "${codeModificationRequest.request}"
            """.trimIndent(),
        )
        return CodeModificationReport(
            text = report,
            filesChanged = project.getChanges().map { it.path }.toList()
        )
    }

    /**
     * Final step in the agent flow
     * Returns the code modification completion report to the user
     * Only triggered when the build is successful (or not needed)
     */
    @Action(pre = [CoderConditions.BUILD_SUCCEEDED])
    @AchievesGoal(
        description = "Modify project code as per code modification request",
    )
    fun shareCodeModificationReport(
        codeModificationReport: CodeModificationReport,
        softwareProject: SoftwareProject,
        operationContext: OperationContext,
    ): SuccessfulCodeModification {
        val suggestedCommitMessage = operationContext.promptRunner(
            llm = coderProperties.primaryCodingLlm,
        ).generateText(
            """
            Generate a concise git commit message for the following code modification report:
            ${codeModificationReport.text}
            """.trimIndent(),
        )
        logger.info("Sharing code modification report: ${codeModificationReport.text}")
        val success = SuccessfulCodeModification(
            request = CodeModificationRequest(codeModificationReport.text),
            report = codeModificationReport,
            suggestedCommitMessage = suggestedCommitMessage,
        )
        logWriter.logResponse(success, softwareProject)
        return success
    }

}
