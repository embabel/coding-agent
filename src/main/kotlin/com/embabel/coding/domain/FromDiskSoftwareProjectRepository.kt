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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

/**
 * Look under disk
 */
@Service
class FromDiskSoftwareProjectRepository(
    val root: File = File(System.getProperty("user.dir")).parentFile,
) : SoftwareProjectRepository,
    InMemoryCrudRepository<SoftwareProject>(
        idGetter = { it.root },
        idSetter = { _, _ -> TODO("shouldn't be called") },
    ) {

    private val logger = LoggerFactory.getLogger(FromDiskSoftwareProjectRepository::class.java)

    init {
        this.saveAll(findProjectsUnderRoot())
    }

    private fun findProjectsUnderRoot(): List<SoftwareProject> {
        val rootFileTools = FileTools.readOnly(root.absolutePath)
        logger.info("Looking under {} for projects", rootFileTools.root)
        val pomFiles = rootFileTools.findFiles("**/pom.xml")
            // TODO why is this needed?
            .filterNot { it.contains("..") }
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
