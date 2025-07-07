package com.embabel.coding.domain

import com.embabel.agent.domain.library.HasContent
import com.embabel.common.core.MobyNameGenerator
import com.embabel.common.core.types.Timed
import com.embabel.common.core.types.Timestamped
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Duration
import java.time.Instant

@JsonTypeInfo(
    use = JsonTypeInfo.Id.DEDUCTION,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CodeModificationRequest::class),
    JsonSubTypes.Type(value = SuccessfulCodeModification::class),
)
sealed interface LogEntry

/**
 * Will be logged.
 */
data class SuccessfulCodeModification(
    val request: CodeModificationRequest,
    val report: CodeModificationReport,
    val suggestedCommitMessage: String,
) : Timestamped, Timed, HasContent, LogEntry {
    override val timestamp: Instant = Instant.now()

    override val runningTime: Duration
        get() = Duration.between(request.timestamp, timestamp)

    override val content: String
        get() = "Code modification completed in ${runningTime.seconds} seconds\n${report.content}"

}

data class CodeModificationRequest(
    @get:JsonPropertyDescription("Request to modify code")
    val request: String,
    val id: String = MobyNameGenerator.generateName(),
) : Timestamped, LogEntry {

    override val timestamp: Instant = Instant.now()
}

/**
 * What the agent did to modify the code.
 * Node that this might not be the final report,
 * as the agent might need to build the project
 * and fix any issues that arise.
 */
data class CodeModificationReport(
    @get:JsonPropertyDescription("Report of the modifications made to code")
    val text: String,
    val filesChanged: List<String>
) : HasContent {

    override val content: String
        get() = text
}