package dev.riseri.server.content

import dev.riseri.core.relic.RelicContentId
import dev.riseri.core.relic.RelicDefinition
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.InputStream

class RelicDataLoadException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/** Keeps relic resource access and serialization outside game-core. */
@Component
class RelicDataLoader(
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    fun load(input: InputStream): Map<RelicContentId, RelicDefinition> {
        val root =
            try {
                objectMapper.readTree(input)
            } catch (exception: Exception) {
                throw RelicDataLoadException("Relic data is not valid JSON", exception)
            }

        val relicsNode = root.requiredArray("relics", "relic data")
        if (relicsNode.isEmpty) {
            throw RelicDataLoadException("relic data.relics must contain at least one relic")
        }

        val definitions = relicsNode.mapIndexed(::parseRelic)
        val duplicateId =
            definitions
                .groupingBy { it.id }
                .eachCount()
                .entries
                .find { it.value > 1 }
                ?.key
        if (duplicateId != null) {
            throw RelicDataLoadException("Relic data contains duplicate id '${duplicateId.value}'")
        }

        // associateBy preserves source order, making reward adapters independent of hash ordering.
        return definitions.associateBy { it.id }
    }

    fun loadFromClasspath(resourcePath: String = "/relics/relics.json"): Map<RelicContentId, RelicDefinition> {
        val input =
            javaClass.getResourceAsStream(resourcePath)
                ?: throw RelicDataLoadException("Relic data resource '$resourcePath' was not found")
        return input.use(::load)
    }

    private fun parseRelic(
        index: Int,
        node: JsonNode,
    ): RelicDefinition {
        val location = "relic[$index]"
        val id = node.requiredText("id", location)
        val name = node.requiredText("name", location)
        val description = node.requiredText("description", location)

        return try {
            RelicDefinition(RelicContentId(id), name, description)
        } catch (exception: IllegalArgumentException) {
            throw RelicDataLoadException("Invalid $location: ${exception.message}", exception)
        }
    }

    private fun JsonNode.requiredArray(
        field: String,
        location: String,
    ): JsonNode {
        val value = get(field)
        if (value == null || !value.isArray) {
            throw RelicDataLoadException("$location.$field must be an array")
        }
        return value
    }

    private fun JsonNode.requiredText(
        field: String,
        location: String,
    ): String {
        val value = get(field)
        if (value == null || !value.isString || value.stringValue().isBlank()) {
            throw RelicDataLoadException("$location.$field must be a non-blank string")
        }
        return value.stringValue()
    }
}
