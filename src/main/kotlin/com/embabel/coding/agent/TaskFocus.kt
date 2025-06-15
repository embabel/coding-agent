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

import com.embabel.coding.domain.SoftwareProject
import com.embabel.coding.domain.SoftwareProjectRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Stateful component that holds the focus of the agent.
 */
@Component
class TaskFocus(
    private val softwareProjectRepository: SoftwareProjectRepository,
) {

    private val logger = LoggerFactory.getLogger(TaskFocus::class.java)

    var softwareProject: SoftwareProject? = softwareProjectRepository.findAll().find {
        it.root.contains("embabel-agent-api")
    }

    fun setFocus(name: String): SoftwareProject? {
        val newFocus = softwareProjectRepository.findAll().find { it.root.contains(name) }
        if (newFocus != null) {
            logger.info("Found new focus: $name")
            softwareProject = newFocus
        } else {
            logger.warn("No project found with name: $name")
        }
        return newFocus
    }
}
