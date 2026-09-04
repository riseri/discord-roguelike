package dev.riseri.core.relic

private val RELIC_CONTENT_ID_PATTERN = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

@JvmInline
value class RelicContentId(
    val value: String,
) {
    init {
        require(RELIC_CONTENT_ID_PATTERN.matches(value)) {
            "Relic content identifier must use lowercase kebab-case"
        }
    }
}

/** Static relic metadata consumed by core systems without exposing its storage format. */
data class RelicDefinition(
    val id: RelicContentId,
    val name: String,
    val description: String,
) {
    init {
        require(name.isNotBlank()) { "Relic name must not be blank" }
        require(description.isNotBlank()) { "Relic description must not be blank" }
    }
}
