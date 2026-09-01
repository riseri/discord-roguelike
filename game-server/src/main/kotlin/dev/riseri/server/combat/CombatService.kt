package dev.riseri.server.combat

import dev.riseri.core.combat.Block
import dev.riseri.core.combat.CombatEngine
import dev.riseri.core.combat.CombatRngState
import dev.riseri.core.combat.CombatState
import dev.riseri.core.combat.EncounterGenerator
import dev.riseri.core.combat.EnemyTurnExecutor
import dev.riseri.core.combat.EntityId
import dev.riseri.core.combat.GameAction
import dev.riseri.core.combat.HitPoints
import dev.riseri.core.combat.InvalidActionException
import dev.riseri.core.combat.PlayerCombatState
import dev.riseri.server.content.EnemyDataLoader
import org.springframework.stereotype.Service

class NoActiveCombatException : IllegalStateException("No active combat encounter")

class InvalidCombatActionException(
    val code: String,
    message: String,
) : IllegalArgumentException(message)

/** Keeps the single M1 encounter in memory while delegating every combat rule to game-core. */
@Service
class CombatService(
    enemyDataLoader: EnemyDataLoader,
) {
    private val enemyDefinitions = enemyDataLoader.loadFromClasspath()
    private var state: CombatState? = null

    @Synchronized
    fun start(seed: Long): CombatResponse {
        val generated = EncounterGenerator.generateStarter(enemyDefinitions.values, CombatRngState(seed))
        val initialState =
            CombatState(
                player =
                    PlayerCombatState(
                        entityId = EntityId("knight"),
                        currentHp = HitPoints(100),
                        maxHp = HitPoints(100),
                        block = Block(0),
                    ),
                enemies = generated.encounter.enemies,
                rngState = generated.nextRngState,
            )
        val readyState = EnemyTurnExecutor.generateIntentions(initialState, enemyDefinitions).state
        state = readyState
        return CombatResponse.from(readyState)
    }

    @Synchronized
    fun get(): CombatResponse = CombatResponse.from(state ?: throw NoActiveCombatException())

    @Synchronized
    fun useAbility(request: UseAbilityRequest): CombatResponse {
        val currentState = state ?: throw NoActiveCombatException()
        val targetId =
            try {
                EntityId(request.targetId)
            } catch (exception: IllegalArgumentException) {
                throw InvalidCombatActionException("INVALID_TARGET", exception.message.orEmpty())
            }
        val action = GameAction.UseAbility(request.abilityId, targetId)
        val result =
            try {
                CombatEngine.execute(currentState, action, enemyDefinitions)
            } catch (exception: InvalidActionException) {
                throw InvalidCombatActionException(exception.reason.name, exception.message.orEmpty())
            }

        // Assign only after the core transition succeeds so rejected actions cannot corrupt the
        // encounter that the client can subsequently read or retry.
        state = result.state
        return CombatResponse.from(result.state)
    }
}
