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
import com.embabel.common.util.loggerFor
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Common configuration and utilities
 */
@ConfigurationProperties(prefix = "embabel.coding")
data class CoderProperties(
    val primaryCodingModel: String = AnthropicModels.Companion.CLAUDE_37_SONNET,//OllamaModels.QWEN2_5_CODER,
    val fixCodingModel: String = AnthropicModels.Companion.CLAUDE_37_SONNET,
    // TODO shouldn't be hardcoded
    val defaultLocation: String = System.getProperty("user.dir") + "/embabel-agent-api",
) {

    init {
        loggerFor<CoderProperties>().info("Coding properties: {}", this)
    }

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
