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
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.domain.library.InternetResource
import com.embabel.agent.domain.library.InternetResources
import com.embabel.coding.domain.SoftwareProject
import org.springframework.context.annotation.Profile

data class CodeExplanation(
    val text: String,
    override val links: List<InternetResource>,
) : HasContent, InternetResources {
    override val content: String
        get() = """
            $text
            
            ${links.joinToString("\n")}
        """.trimIndent()
}


@Agent(
    description = "Explain code in a software project or directory structure",
)
@Profile("!test")
class CodeExplainer(
    private val coderProperties: CoderProperties,
) {

    @Action
    @AchievesGoal(description = "Code has been explained to the user")
    fun explainCode(
        userInput: UserInput,
        project: SoftwareProject,
        context: OperationContext,
    ): CodeExplanation = context.ai()
        .withLlm(coderProperties.primaryCodingLlm)
        .withPromptElements(project)
        .create(
            """
                Execute the following user request to explain something about the given project.
                Use the file tools to read code and directories.
                Use the project information to help you understand the code.
                List any resources from the internet that will help the user
                understand any complex concepts and provide useful background reading.
                For example, provide links for a potentially unfamiliar algorithm.

                User request:
                "${userInput.content}"
            }
            """.trimIndent(),
        )

}
