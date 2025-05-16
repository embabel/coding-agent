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
package com.embabel.coding

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.AgentCapabilities
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.tools.file.FileWriteTools
import com.embabel.coding.agent.CodeExplanation
import com.embabel.coding.agent.CoderConditions
import com.embabel.coding.domain.SoftwareProject
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import java.io.File
import kotlin.jvm.java
import kotlin.text.trimIndent

data class ProjectRecipe(
    val projectName: String = "demo",
)

object TestCoderConditions {
    const val TestProjectCreated = "testProjectCreated"
}

@AgentCapabilities(scan = false)
@Profile("!test")
class TestProjectCreator {

    private val logger = LoggerFactory.getLogger(TestProjectCreator::class.java)

    val zipFilePath = "/Users/rjohnson/Downloads/bot.zip"

    @Action(
        post = [TestCoderConditions.TestProjectCreated]
    )
    fun createTestProject(
        projectRecipe: ProjectRecipe,
        context: OperationContext,
    ): SoftwareProject {
        logger.info("Creating test project")

        val tempDir = FileWriteTools.createTempDir("thing")

        // TODO should not be hard coded
        val zipFile = File(zipFilePath)
        val projectDir = FileWriteTools.extractZipFile(zipFile = zipFile, tempDir = tempDir, delete = false)

        // Return the project coordinates
        context.setCondition(TestCoderConditions.TestProjectCreated, true)
//        context += springRecipe
        return SoftwareProject(
            root = projectDir.absolutePath,
            tech = "Kotlin, Spring Boot, Maven, Spring Web, Spring Actuator, Spring DevTools",
            defaultCodingStyle = "Modern Kotlin with Spring Boot conventions. Clean architecture with separation of concerns.",
            buildCommand = "mvn test",
        )
    }

    @Action(
        pre = [TestCoderConditions.TestProjectCreated,
            CoderConditions.BUILD_SUCCEEDED
        ]
    )
    @AchievesGoal("Create a new test project")
    fun describeShinyNewProject(softwareProject: SoftwareProject): CodeExplanation =
        CodeExplanation(
            text = """
                Project root: ${softwareProject.root}
                Technologies used: ${softwareProject.tech}
                Coding style: ${softwareProject.codingStyle}
            """.trimIndent(),
            links = emptyList(),
        )

}
