package com.embabel.coding.tools

import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupDescription
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.agent.tools.mcp.McpToolGroup
import io.modelcontextprotocol.client.McpSyncClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoderToolGroupsConfiguration(
    private val mcpSyncClients: List<McpSyncClient>,
) {

    @Bean
    fun gitToolsGroup(): ToolGroup {
        return McpToolGroup(
            description = ToolGroupDescription(description = "git tools", role = "git"),
            name = "docker-git",
            provider = "Docker",
            permissions = setOf(
                ToolGroupPermission.INTERNET_ACCESS,
                ToolGroupPermission.HOST_ACCESS,
            ),
            clients = mcpSyncClients,
            filter = {
                (it.toolDefinition.name().contains("git_"))
            },
        )
    }
}