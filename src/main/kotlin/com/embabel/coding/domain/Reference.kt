package com.embabel.coding.domain

import com.embabel.agent.tools.file.FileReadTools
import com.embabel.agent.tools.file.WellKnownFileContentTransformers
import com.embabel.common.ai.prompt.PromptContributor
import com.embabel.common.core.types.Described
import com.embabel.common.core.types.Named
import com.embabel.common.util.StringTransformer

/**
 * Reference
 * Must be added as tools and prompt contributor
 */
interface Reference : PromptContributor, Named, Described

/**
 * Reference to another project
 */
class ProjectReference(
    override val name: String,
    override val description: String,
    override val root: String,
) : Reference, FileReadTools {

    override val fileContentTransformers: List<StringTransformer>
        get() = listOf(WellKnownFileContentTransformers.removeApacheLicenseHeader)

    override fun contribution(): String {
        return """
            |Software project: $name
            |Description: $description
            |Root: $root
            |Use file tools to read files in the project.
            """".trimIndent()
    }
}

/**
 * Directory structure but not a software project
 */
class FilesReference(
    override val name: String,
    override val description: String,
    override val root: String,
) : Reference, FileReadTools {

    override val fileContentTransformers: List<StringTransformer>
        get() = listOf(WellKnownFileContentTransformers.removeApacheLicenseHeader)

    override fun contribution(): String {
        return """
            |Documentation: $name
            |Description: $description
            |Root: $root
            |Use file tools to read documents.
            """".trimIndent()
    }
}

class WebResourceReference(
    override val name: String,
    override val description: String,
    val url: String,
) : Reference {

    override fun contribution(): String =
        "Use the fetch tool to read the content of $url"

}
