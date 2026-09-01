package dev.riseri.server.content

import dev.riseri.core.combat.EnemyContentId
import dev.riseri.core.combat.EnemyDefinition
import dev.riseri.core.combat.EnemyIntention
import dev.riseri.core.combat.HitPoints
import dev.riseri.core.combat.IntentionId
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.InputStream

class EnemyDataLoadException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/**
 * Owns serialization and resource access so file formats never leak into the authoritative core.
 * The JSON tree is validated field by field to report the content location of malformed data.
 */
class EnemyDataLoader(
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    fun load(input: InputStream): Map<EnemyContentId, EnemyDefinition> {
        val root =
            try {
                objectMapper.readTree(input)
            } catch (exception: Exception) {
                throw EnemyDataLoadException("Enemy data is not valid JSON", exception)
            }

        val enemiesNode = root.requiredArray("enemies", "enemy data")
        if (enemiesNode.isEmpty) {
            throw EnemyDataLoadException("enemy data.enemies must contain at least one enemy")
        }
        val definitions = enemiesNode.mapIndexed { index, node -> parseEnemy(node, index) }
        val duplicateId =
            definitions
                .groupingBy { it.id }
                .eachCount()
                .entries
                .find { it.value > 1 }
                ?.key
        if (duplicateId != null) {
            throw EnemyDataLoadException("Enemy data contains duplicate id '${duplicateId.value}'")
        }
        return definitions.associateBy { it.id }
    }

    fun loadFromClasspath(resourcePath: String = "/enemies/enemies.json"): Map<EnemyContentId, EnemyDefinition> {
        val input =
            javaClass.getResourceAsStream(resourcePath)
                ?: throw EnemyDataLoadException("Enemy data resource '$resourcePath' was not found")
        return input.use(::load)
    }

    private fun parseEnemy(
        node: JsonNode,
        index: Int,
    ): EnemyDefinition {
        val location = "enemy[$index]"
        val id = node.requiredText("id", location)
        val hp = node.requiredPositiveInt("hp", location)
        val intentions =
            node.requiredArray("intentions", location).mapIndexed { intentionIndex, intention ->
                val intentionLocation = "$location.intention[$intentionIndex]"
                EnemyIntention(
                    id = IntentionId(intention.requiredText("id", intentionLocation)),
                    damage = intention.requiredNonNegativeInt("damage", intentionLocation),
                )
            }
        if (intentions.isEmpty()) {
            throw EnemyDataLoadException("$location.intentions must contain at least one intention")
        }

        return try {
            EnemyDefinition(EnemyContentId(id), HitPoints(hp), intentions)
        } catch (exception: IllegalArgumentException) {
            throw EnemyDataLoadException("Invalid $location: ${exception.message}", exception)
        }
    }

    private fun JsonNode.requiredArray(
        field: String,
        location: String,
    ): JsonNode {
        val value = get(field)
        if (value == null || !value.isArray) {
            throw EnemyDataLoadException("$location.$field must be an array")
        }
        return value
    }

    private fun JsonNode.requiredText(
        field: String,
        location: String,
    ): String {
        val value = get(field)
        if (value == null || !value.isString || value.stringValue().isBlank()) {
            throw EnemyDataLoadException("$location.$field must be a non-blank string")
        }
        return value.stringValue()
    }

    private fun JsonNode.requiredPositiveInt(
        field: String,
        location: String,
    ): Int {
        val value = requiredInt(field, location)
        if (value <= 0) {
            throw EnemyDataLoadException("$location.$field must be positive")
        }
        return value
    }

    private fun JsonNode.requiredNonNegativeInt(
        field: String,
        location: String,
    ): Int {
        val value = requiredInt(field, location)
        if (value < 0) {
            throw EnemyDataLoadException("$location.$field must not be negative")
        }
        return value
    }

    private fun JsonNode.requiredInt(
        field: String,
        location: String,
    ): Int {
        val value = get(field)
        if (value == null || !value.isIntegralNumber || !value.canConvertToInt()) {
            throw EnemyDataLoadException("$location.$field must be an integer")
        }
        return value.asInt()
    }
}
