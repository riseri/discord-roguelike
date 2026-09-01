package dev.riseri.core.combat

enum class InvalidActionReason {
    WRONG_PHASE,
    ACTOR_DEFEATED,
    TARGET_NOT_FOUND,
    INVALID_TARGET,
    TARGET_DEFEATED,
}

class InvalidActionException(
    val reason: InvalidActionReason,
) : IllegalArgumentException(reason.name)

object AbilityExecutor {
    private const val SLASH_DAMAGE = 15
    private const val GUARD_BLOCK = 12

    fun execute(
        state: CombatState,
        action: GameAction.UseAbility,
    ): ActionResult {
        if (state.phase != CombatPhase.PLAYER) {
            throw InvalidActionException(InvalidActionReason.WRONG_PHASE)
        }
        if (state.player.currentHp.value == 0) {
            throw InvalidActionException(InvalidActionReason.ACTOR_DEFEATED)
        }

        return when (action.abilityId) {
            AbilityId.SLASH -> executeSlash(state, action)
            AbilityId.GUARD -> executeGuard(state, action)
        }
    }

    private fun executeSlash(
        state: CombatState,
        action: GameAction.UseAbility,
    ): ActionResult {
        if (action.targetId == state.player.entityId) {
            throw InvalidActionException(InvalidActionReason.INVALID_TARGET)
        }

        val target =
            state.enemies.find { it.entityId == action.targetId }
                ?: throw InvalidActionException(InvalidActionReason.TARGET_NOT_FOUND)

        if (target.currentHp.value == 0) {
            throw InvalidActionException(InvalidActionReason.TARGET_DEFEATED)
        }

        val damageDealt = minOf(SLASH_DAMAGE, target.currentHp.value)
        val updatedTarget = target.copy(currentHp = HitPoints(target.currentHp.value - damageDealt))
        val updatedEnemies = state.enemies.map { if (it.entityId == target.entityId) updatedTarget else it }

        return ActionResult(
            state = state.copy(enemies = updatedEnemies, phase = CombatPhase.ENEMY),
            events =
                listOf(
                    GameEvent.AbilityUsed(
                        actorId = state.player.entityId,
                        abilityId = action.abilityId,
                        targetId = target.entityId,
                    ),
                    GameEvent.DamageDealt(
                        sourceId = state.player.entityId,
                        targetId = target.entityId,
                        amount = damageDealt,
                    ),
                ),
        )
    }

    private fun executeGuard(
        state: CombatState,
        action: GameAction.UseAbility,
    ): ActionResult {
        if (action.targetId != state.player.entityId) {
            val reason =
                if (state.enemies.any { it.entityId == action.targetId }) {
                    InvalidActionReason.INVALID_TARGET
                } else {
                    InvalidActionReason.TARGET_NOT_FOUND
                }
            throw InvalidActionException(reason)
        }

        val updatedPlayer =
            state.player.copy(block = Block(state.player.block.value + GUARD_BLOCK))

        return ActionResult(
            state = state.copy(player = updatedPlayer, phase = CombatPhase.ENEMY),
            events =
                listOf(
                    GameEvent.AbilityUsed(
                        actorId = state.player.entityId,
                        abilityId = action.abilityId,
                        targetId = state.player.entityId,
                    ),
                    GameEvent.BlockGained(
                        entityId = state.player.entityId,
                        amount = GUARD_BLOCK,
                    ),
                ),
        )
    }
}
