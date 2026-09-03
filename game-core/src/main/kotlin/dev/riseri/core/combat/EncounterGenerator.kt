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
    private val STARTER_ENEMY_CONTENT_ID = EnemyContentId("goblin")

    /** Creates the fixed two-Goblin tactical encounter used by the playable vertical slice. */
    fun generateStarter(
        enemyDefinitions: Collection<EnemyDefinition>,
        rngState: CombatRngState,
    ): EncounterGenerationResult {
        val goblin =
            enemyDefinitions.singleOrNull { it.id == STARTER_ENEMY_CONTENT_ID }
                ?: throw IllegalArgumentException("Starter encounter requires the goblin enemy definition")

        return EncounterGenerationResult(
            encounter =
                CombatEncounter(
                    listOf(
                        goblin.createCombatState(EntityId("goblin-1")).copy(position = GridPosition(6, 1)),
                        goblin.createCombatState(EntityId("goblin-2")).copy(position = GridPosition(6, 4)),
                    ),
                ),
            // The fixed starter composition makes no random selection. Preserving the state keeps
            // subsequent intention generation reproducible from the run's original seed.
            nextRngState = rngState,
        )
    }

    /** Generates a fixed-size normal encounter while retaining multi-enemy encounter support. */
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
