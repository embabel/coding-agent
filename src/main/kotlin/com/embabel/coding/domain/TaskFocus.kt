package com.embabel.coding.domain

import com.embabel.coding.agent.CoderProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Stateful component that holds the focus of the agent.
 */
@Component
class TaskFocus(
    private val softwareProjectRepository: SoftwareProjectRepository,
    private val coderProperties: CoderProperties,
) {

    private val logger = LoggerFactory.getLogger(TaskFocus::class.java)

    var softwareProject: SoftwareProject? = null

    init {
        coderProperties.defaultProject?.let { setFocus(it) }
    }

    fun setFocus(name: String): SoftwareProject? {
        val newFocus = softwareProjectRepository.findAll().find { it.root.contains(name) }
        if (newFocus != null) {
            logger.info("Set new focus: $name")
            softwareProject = newFocus
        } else {
            logger.warn("Cannot set focus: No project found with name: $name")
        }
        return newFocus
    }

    fun saveAndSwitch(newAgentProject: SoftwareProject) {
        logger.info("Switching focus to new project: ${newAgentProject.root}")
        softwareProjectRepository.save(newAgentProject)
        softwareProject = newAgentProject
    }
}