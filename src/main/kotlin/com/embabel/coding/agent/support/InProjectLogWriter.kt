package com.embabel.coding.agent.support

import com.embabel.coding.agent.CodeModificationRequest
import com.embabel.coding.agent.LogWriter
import com.embabel.coding.agent.SuccessfulCodeModification
import com.embabel.coding.domain.SoftwareProject
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

/**
 * Write to the /.embabel/log.jsonl file in the focus project
 */
@Service
class InProjectLogWriter(
    private val path: String = ".embabel/log.jsonl",
    private val objectMapper: ObjectMapper,
) : LogWriter {

    override fun logRequest(
        request: CodeModificationRequest,
        softwareProject: SoftwareProject,
    ) {
        val jsonLine = objectMapper.writeValueAsString(request) + "\n"
        softwareProject.appendToFile(
            path = path,
            content = jsonLine,
            createIfNotExists = true,
        )
    }

    override fun logResponse(
        request: SuccessfulCodeModification,
        softwareProject: SoftwareProject,
    ) {
        val jsonLine = objectMapper.writeValueAsString(request) + "\n"
        softwareProject.appendToFile(path = path, content = jsonLine, createIfNotExists = true)
    }
}