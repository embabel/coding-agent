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
import com.embabel.agent.api.common.ActionContext
import com.embabel.agent.mcpserver.McpResourcePublisher
import com.embabel.agent.mcpserver.SyncResourceSpecificationFactory
import com.embabel.agent.tools.file.FileWriteTools
import com.embabel.coding.domain.SoftwareProject
import io.modelcontextprotocol.server.McpServerFeatures
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.io.File

data class SpringRecipe(
    val projectName: String = "demo",
    val groupId: String = "com.example",
    val artifactId: String = "demo",
    val version: String = "0.0.1-SNAPSHOT",
    val bootVersion: String = "3.2.0",
    val language: String = "kotlin",
    val packaging: String = "jar",
    val javaVersion: String = "17",
    val dependencies: String = "web,actuator,devtools",
)

object SpringCoderConditions {
    const val SPRING_PROJECT_CREATED = "springProjectCreated"
}

@AgentCapabilities(
    scan = false
)
@Profile("!test")
class SpringCoder {

    private val logger = LoggerFactory.getLogger(SpringCoder::class.java)

    @Action
    fun askUserForSpringRecipe(): SpringRecipe =
        fromForm("Spring recipe")

    @Action(
        post = [SpringCoderConditions.SPRING_PROJECT_CREATED]
    )
    fun createSpringInitialzrProject(
        springRecipe: SpringRecipe,
        context: ActionContext,
    ): SoftwareProject {
        logger.info("Creating Spring Initialzr project")

        val tempDir = FileWriteTools.createTempDir("spring-initializr")

        // Create RestClient to call Spring Initialzr
        val restClient = RestClient.builder()
            .baseUrl("https://start.spring.io")
            .build()

        // Make the request to Spring Initialzr and save the response to a zip file
        val zipFile = File("$tempDir/${springRecipe.artifactId}.zip")
        val response = restClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/starter.zip")
                    .queryParam("name", springRecipe.projectName)
                    .queryParam("groupId", springRecipe.groupId)
                    .queryParam("artifactId", springRecipe.artifactId)
                    .queryParam("version", springRecipe.version)
                    .queryParam("bootVersion", springRecipe.bootVersion)
                    .queryParam("language", springRecipe.language)
                    .queryParam("packaging", springRecipe.packaging)
                    .queryParam("javaVersion", springRecipe.javaVersion)
                    .queryParam("dependencies", springRecipe.dependencies)
                    .build()
            }
            .retrieve()
            .toEntity(ByteArray::class.java)
            .body ?: throw RuntimeException("Failed to download Spring Initialzr project")

        // Save the response to a zip file
        zipFile.writeBytes(response)
        logger.info("Downloaded Spring Initialzr project to {}", zipFile.absolutePath)

        val projectDir = FileWriteTools.extractZipFile(
            zipFile = zipFile,
            tempDir = tempDir,
            delete = true,
        )
        logger.info("Extracted Spring Initialzr project to {}", projectDir.absolutePath)

        // Return the project coordinates
        context.setCondition(SpringCoderConditions.SPRING_PROJECT_CREATED, true)
        context += springRecipe
        return SoftwareProject(
            root = projectDir.absolutePath,
            tech = "Kotlin, Spring Boot, Maven, Spring Web, Spring Actuator, Spring DevTools",
            defaultCodingStyle = "Modern Kotlin with Spring Boot conventions. Clean architecture with separation of concerns.",
            buildCommand = "mvn test",
        )
    }

    @Action(
        pre = [SpringCoderConditions.SPRING_PROJECT_CREATED,
            CoderConditions.BUILD_SUCCEEDED],
    )
    @AchievesGoal(description = "Create a new Spring project")
    fun describeShinyNewSpringProject(
        softwareProject: SoftwareProject,
        springRecipe: SpringRecipe,
    ): CodeExplanation =
        CodeExplanation(
            text = """
                Project root: ${softwareProject.root}
                Technologies used: ${softwareProject.tech}
                Coding style: ${softwareProject.codingStyle}
            """.trimIndent(),
            links = emptyList(),
        )

}

@Component
class ResourceFactory : McpResourcePublisher {

    override fun resources(): List<McpServerFeatures.SyncResourceSpecification> {
        return listOf(
            SyncResourceSpecificationFactory.staticSyncResourceSpecification(
                "spring-coder",
                "Spring Coder",
                "A prompt that explains how this coding agent works",
                "A tool for creating and managing Spring projects",
            )
        )
    }

    override fun infoString(verbose: Boolean?, indent: Int): String {
        return toString()
    }
}