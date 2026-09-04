package dev.riseri.core.run

import dev.riseri.core.combat.Block
import dev.riseri.core.combat.CombatEngine
import dev.riseri.core.combat.CombatRngState
import dev.riseri.core.combat.CombatState
import dev.riseri.core.combat.CombatStatus
import dev.riseri.core.combat.EncounterGenerator
import dev.riseri.core.combat.EnemyContentId
import dev.riseri.core.combat.EnemyDefinition
import dev.riseri.core.combat.EnemyTurnExecutor
import dev.riseri.core.combat.EntityId
import dev.riseri.core.combat.GameAction
import dev.riseri.core.combat.GameEvent
import dev.riseri.core.combat.PlayerCombatState

enum class InvalidRunCombatReason {
    RUN_ENDED,
    ROOM_ALREADY_COMPLETED,
    COMBAT_ALREADY_STARTED,
    NO_ACTIVE_COMBAT,
}

class InvalidRunCombatException(
    val reason: InvalidRunCombatReason,
) : IllegalArgumentException(reason.name)

data class RunCombatActionResult(
    val state: RunState,
    val combatEvents: List<GameEvent>,
    val runEvents: List<RunEvent>,
)

/** Owns the state boundary between a run and its current deterministic combat encounter. */
object RunCombatEngine {
    fun start(
        state: RunState,
        enemyDefinitions: Map<EnemyContentId, EnemyDefinition>,
    ): RunState {
        requireCombatCanStart(state)

        val encounterRngState = CombatRngState(state.rngState.value)
        val generated =
            if (state.currentRoomId == state.dungeonGraph.startRoomId) {
                EncounterGenerator.generateStarter(enemyDefinitions.values, encounterRngState)
            } else {
                // Only the first room uses the fixed onboarding encounter. Later rooms consume
                // the continuing run stream to create a fresh supported enemy composition.
                EncounterGenerator.generate(enemyDefinitions.values, encounterRngState)
            }
        val initialCombat =
            CombatState(
                player =
                    PlayerCombatState(
                        entityId = EntityId("knight"),
                        currentHp = state.playerHp,
                        maxHp = state.playerMaxHp,
                        block = Block(0),
                    ),
                enemies = generated.encounter.enemies,
                rngState = generated.nextRngState,
            )
        val readyCombat = EnemyTurnExecutor.generateIntentions(initialCombat, enemyDefinitions).state

        // The run RNG advances with encounter setup so the next run-owned random decision resumes
        // from the exact state reached by combat.
        return state.copy(
            rngState = RunRngState(readyCombat.rngState.value),
            activeCombat = readyCombat,
        )
    }

    fun execute(
        state: RunState,
        action: GameAction,
        enemyDefinitions: Map<EnemyContentId, EnemyDefinition>,
    ): RunCombatActionResult {
        if (state.status != RunStatus.ACTIVE) {
            throw InvalidRunCombatException(InvalidRunCombatReason.RUN_ENDED)
        }
        val combat =
            state.activeCombat
                ?: throw InvalidRunCombatException(InvalidRunCombatReason.NO_ACTIVE_COMBAT)
        val combatResult = CombatEngine.execute(combat, action, enemyDefinitions)

        val runEvents =
            when (combatResult.state.status) {
                CombatStatus.ACTIVE -> emptyList()
                CombatStatus.WON -> listOf(RunEvent.RoomCompleted(state.currentRoomId))
                CombatStatus.LOST -> listOf(RunEvent.RunLost)
            }
        val updatedState =
            state.copy(
                status =
                    if (combatResult.state.status == CombatStatus.LOST) {
                        RunStatus.LOST
                    } else {
                        state.status
                    },
                playerHp = combatResult.state.player.currentHp,
                completedRoomIds =
                    if (combatResult.state.status == CombatStatus.WON) {
                        state.completedRoomIds + state.currentRoomId
                    } else {
                        state.completedRoomIds
                    },
                rngState = RunRngState(combatResult.state.rngState.value),
                activeCombat = combatResult.state,
            )

        return RunCombatActionResult(updatedState, combatResult.events, runEvents)
    }

    private fun requireCombatCanStart(state: RunState) {
        when {
            state.status != RunStatus.ACTIVE -> {
                throw InvalidRunCombatException(InvalidRunCombatReason.RUN_ENDED)
            }

            state.currentRoomId in state.completedRoomIds -> {
                throw InvalidRunCombatException(InvalidRunCombatReason.ROOM_ALREADY_COMPLETED)
            }

            state.activeCombat != null -> {
                throw InvalidRunCombatException(InvalidRunCombatReason.COMBAT_ALREADY_STARTED)
            }
        }
    }
}
