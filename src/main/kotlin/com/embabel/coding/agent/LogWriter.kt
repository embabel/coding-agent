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

import com.embabel.coding.domain.CodeModificationRequest
import com.embabel.coding.domain.SoftwareProject
import com.embabel.coding.domain.SuccessfulCodeModification

/**
 * Log the changes we've made to the codebase.
 */
interface LogWriter {

    /**
     * Write a log entry for a code modification request.
     */
    fun logRequest(
        request: CodeModificationRequest,
        softwareProject: SoftwareProject,
    )

    /**
     * Write a log entry for a successful code modification response.
     */
    fun logResponse(
        request: SuccessfulCodeModification,
        softwareProject: SoftwareProject,
    )
}
