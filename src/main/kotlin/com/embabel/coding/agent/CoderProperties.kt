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

import com.embabel.agent.config.models.AnthropicModels
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria
import com.embabel.common.ai.prompt.PromptContributor
import org.springframework.boot.context.properties.ConfigurationProperties
import java.io.File

/**
 * Common configuration and utilities
 * @param primaryCodingModel The primary coding model to use for code modifications.
 * @param fixCodingModel The model to use for fixing broken builds.
 * @param projectRoot The root directory for the coding agent. Defaults to the parent
 * of the current working directory.
 * @param findNestedProjects Whether to search for nested projects in the project root.
 * For example, nested Maven projects. Slows down loading if set to true.
 */
@ConfigurationProperties(prefix = "embabel.coder")
data class CoderProperties(
    val primaryCodingModel: String = AnthropicModels.Companion.CLAUDE_37_SONNET,
    val fixCodingModel: String = AnthropicModels.Companion.CLAUDE_40_OPUS,
    val projectRoot: String? = null,
    val findNestedProjects: Boolean = false,
    val defaultProject: String? = null,
    private val codeModificationDirections: String = """
        Use the file tools to read code and directories.
        Use the web tools if you are asked to use a technology you don't know about.
        ALWAYS LOOK FOR THE FILES IN THE PROJECT LOCALLY USING FILE TOOLS, NOT THE WEB OR GITHUB.
        Make multiple small, focused edits using the editFile tool.
    """.trimIndent()
) {

    val root: File = File(projectRoot ?: System.getProperty("user.dir")).parentFile

    fun codeModificationDirections() = PromptContributor.fixed(
        codeModificationDirections,
    )

    /**
     * Primary coding Llm
     */
    val primaryCodingLlm = LlmOptions.Companion(
        criteria = ModelSelectionCriteria.Companion.byName(primaryCodingModel),
    )

    val fixCodingLlm = LlmOptions.Companion(
        criteria = ModelSelectionCriteria.Companion.byName(fixCodingModel),
    )
}
