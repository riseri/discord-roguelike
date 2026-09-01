package dev.riseri.core.combat

data class CombatEncounter(
    val enemies: List<EnemyCombatState>,
) {
    init {
        require(enemies.isNotEmpty()) { "Combat encounter must contain at least one enemy" }
        require(enemies.map { it.entityId }.distinct().size == enemies.size) {
            "Combat encounter enemy entity identifiers must be unique"
        }
    }
}

data class EncounterGenerationResult(
    val encounter: CombatEncounter,
    val nextRngState: CombatRngState,
)

object EncounterGenerator {
    private const val ENEMIES_PER_ENCOUNTER = 2

    /** Generates the fixed-size normal encounter used by the M1 playable combat room. */
    fun generate(
        enemyDefinitions: Collection<EnemyDefinition>,
        rngState: CombatRngState,
    ): EncounterGenerationResult {
        require(enemyDefinitions.isNotEmpty()) { "Cannot generate an encounter without enemy definitions" }

        // Content loaders may return different collection implementations. Stable ordering ensures
        // that a seed and the same definition pool always select the same enemy composition.
        val orderedDefinitions = enemyDefinitions.sortedBy { it.id.value }
        var nextRngState = rngState
        val enemies =
            List(ENEMIES_PER_ENCOUNTER) { slot ->
                val random = nextRngState.nextInt(orderedDefinitions.size)
                nextRngState = random.nextState
                val definition = orderedDefinitions[random.value]
                definition.createCombatState(EntityId("${definition.id.value}-${slot + 1}"))
            }

        return EncounterGenerationResult(
            encounter = CombatEncounter(enemies),
            nextRngState = nextRngState,
        )
    }
}
