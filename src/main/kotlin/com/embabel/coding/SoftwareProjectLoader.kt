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

import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.create
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile


//@Agent(
//    description = "Explain code or perform changes to a software project or directory structure",
//)
@Profile("!test")
class SoftwareProjectLoader(
    private val projectRepository: ProjectRepository,
    private val coderProperties: CoderProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Action
    fun loadExistingProject(): SoftwareProject? {
        val found = projectRepository.findById(coderProperties.defaultLocation)
        if (found.isPresent) {
            logger.info("Found existing project at ${coderProperties.defaultLocation}")
        }
        return found.orElse(null)
    }

    /**
     * Use an LLM to analyze the project.
     * This is expensive so we set cost high
     */
    @Action(cost = 10000.0)
    fun analyzeProject(): SoftwareProject =
        using(coderProperties.primaryCodingLlm).create<SoftwareProject>(
            """
                Analyze the project at ${coderProperties.defaultLocation}
                Use the file tools to read code and directories before analyzing it
            """.trimIndent(),
        ).also { project ->
            // So we don't need to do this again
            projectRepository.save(project)
        }

}
