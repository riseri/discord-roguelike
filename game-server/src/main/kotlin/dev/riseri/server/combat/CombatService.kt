package dev.riseri.server.combat

import dev.riseri.core.combat.CombatState
import dev.riseri.core.combat.EntityId
import dev.riseri.core.combat.GameAction
import dev.riseri.core.combat.GridPosition
import dev.riseri.core.combat.InvalidActionException
import dev.riseri.core.run.InvalidRunCombatException
import dev.riseri.server.content.EnemyDataLoader
import dev.riseri.server.run.RunService
import org.springframework.stereotype.Service

class NoActiveCombatException : IllegalStateException("No active combat encounter")

class InvalidCombatActionException(
    val code: String,
    message: String,
) : IllegalArgumentException(message)

/** Exposes combat from the active run while delegating every state transition to game-core. */
@Service
class CombatService(
    enemyDataLoader: EnemyDataLoader,
    private val runService: RunService,
) {
    private val enemyDefinitions = enemyDataLoader.loadFromClasspath()

    fun start(): CombatResponse =
        try {
            CombatResponse.from(runService.startCombat(enemyDefinitions).activeCombat!!)
        } catch (exception: InvalidRunCombatException) {
            throw InvalidCombatActionException(exception.reason.name, exception.message.orEmpty())
        }

    fun get(): CombatResponse = CombatResponse.from(runService.currentState().activeCombat ?: throw NoActiveCombatException())

    fun useAbility(request: UseAbilityRequest): CombatActionResponse {
        if (runService.currentState().activeCombat == null) {
            throw NoActiveCombatException()
        }
        val action =
            if (request.destination != null) {
                GameAction.MoveUnit(GridPosition(request.destination.x, request.destination.y))
            } else {
                val targetId =
                    try {
                        EntityId(request.targetId ?: "")
                    } catch (exception: IllegalArgumentException) {
                        throw InvalidCombatActionException("INVALID_TARGET", exception.message.orEmpty())
                    }
                GameAction.UseAbility(
                    request.abilityId ?: throw InvalidCombatActionException("INVALID_ABILITY", "Ability is required"),
                    targetId,
                )
            }
        val result =
            try {
                runService.executeCombat(action, enemyDefinitions)
            } catch (exception: InvalidActionException) {
                throw InvalidCombatActionException(exception.reason.name, exception.message.orEmpty())
            } catch (exception: InvalidRunCombatException) {
                throw InvalidCombatActionException(exception.reason.name, exception.message.orEmpty())
            }

        return CombatActionResponse.from(result.state.activeCombat!!, result.combatEvents)
    }
}
