package com.embabel.coding.agent

import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.create
import com.embabel.coding.domain.SoftwareProject
import com.embabel.coding.domain.SoftwareProjectRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile

//@Agent(
//    description = "Explain code or perform changes to a software project or directory structure",
//)
@Profile("!test")
class SoftwareProjectLoader(
    private val softwareProjectRepository: SoftwareProjectRepository,
    private val coderProperties: CoderProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Action
    fun loadExistingProject(): SoftwareProject? {
//        val found = softwareProjectRepository.findById(coderProperties.defaultLocation)
//        if (found.isPresent) {
//            logger.info("Found existing project at ${coderProperties.defaultLocation}")
//        }
//        return found.orElse(null)
        TODO()
    }

    /**
     * Use an LLM to analyze the project.
     * This is expensive so we set cost high
     */
    @Action(cost = 10000.0)
    fun analyzeProject(): SoftwareProject =
        using(coderProperties.primaryCodingLlm).create<SoftwareProject>(
            """
                Analyze the project at ${TODO()}
                Use the file tools to read code and directories before analyzing it
            """.trimIndent(),
        ).also { project ->
            // So we don't need to do this again
            softwareProjectRepository.save(project)
        }

}