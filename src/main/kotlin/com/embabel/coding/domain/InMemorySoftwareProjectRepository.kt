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

import com.embabel.examples.common.InMemoryCrudRepository
import org.springframework.stereotype.Service

@Service
class InMemorySoftwareProjectRepository : SoftwareProjectRepository,
    InMemoryCrudRepository<SoftwareProject>(
        idGetter = { it.root },
        idSetter = { _, _ -> TODO("shouldn't be called") },
    ) {

    init {
        save(EmbabelAgentApi)
    }
}

val EmbabelAgentApi = SoftwareProject(
    root = System.getProperty("user.dir") + "../embabel-agent/embabel-agent/api",
    url = "https://github.com/embabel/embabel-agent-api",
    buildCommand = "mvn test",
    tech = """
        |Kotlin, Spring Boot, Maven, Jacoco, Spring AI
        |JUnit 5, mockk
        |"""".trimMargin(),
)
