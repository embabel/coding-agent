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
package com.embabel.coding.domain

import com.embabel.agent.domain.InMemoryCrudRepository
import com.embabel.agent.tools.file.FileTools
import com.embabel.coding.agent.CoderProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Look for parallel directories
 */
@Service
class FromDiskSoftwareProjectRepository(
    private val properties: CoderProperties,
) : SoftwareProjectRepository,
    InMemoryCrudRepository<SoftwareProject>(
        idGetter = { it.root },
        idSetter = { _, _ -> TODO("shouldn't be called") },
    ) {

    private val logger = LoggerFactory.getLogger(FromDiskSoftwareProjectRepository::class.java)

    init {
        this.saveAll(findProjectsUnderRoot())
        logger.info("Loaded {} projects from disk", this.count())
        logger.info(
            "Projects:\n\t{}", this.findAll().sortedBy { it.root }.joinToString("\n\t") { it.root })
    }

    private fun findProjectsUnderRoot(): List<SoftwareProject> {
        val rootFileTools = FileTools.readOnly(properties.root.absolutePath)
        logger.info("Looking under {} for projects", rootFileTools.root)
        val pomFiles = rootFileTools.findFiles(
            glob = "**/pom.xml",
            findHighest = !properties.findNestedProjects
        )
        logger.info("Found {} Maven projects", pomFiles.size)
        return pomFiles
            .map { it.replace("pom.xml", "") }
            .map { root ->
                SoftwareProject(
                    root = root,
                    url = "TODO: call git",
                    buildCommand = "mvn test",
                    // TODO fix this
                    tech = """
                        |Maven, Java, Spring Boot
                        |JUnit 5, Mockito
                        |""".trimMargin(),
                )
            }
    }
}
