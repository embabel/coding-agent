package com.embabel.coding.agent

import com.embabel.coding.domain.SoftwareProject
import com.embabel.coding.domain.SoftwareProjectRepository
import org.springframework.stereotype.Component

/**
 * Stateful component that holds the focus of the agent.
 */
@Component
class TaskFocus(private val softwareProjectRepository: SoftwareProjectRepository) {

    var softwareProject: SoftwareProject? = softwareProjectRepository.findAll().find {
        it.root.contains("embabel-agent-api")
    }

    fun setFocus(name: String): SoftwareProject? {
        val newFocus = softwareProjectRepository.findAll().find { it.root.contains(name) }
        if (newFocus != null) {
            softwareProject = newFocus
        }
        return newFocus
    }
}

