package dev.riseri.core.combat

enum class InvalidActionReason {
    COMBAT_ENDED,
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
    fun execute(
        state: CombatState,
        action: GameAction.UseAbility,
    ): ActionResult {
        // Terminal status takes precedence over stale phase or actor data so callers receive one
        // stable reason whenever they submit an action after combat has ended.
        if (state.status != CombatStatus.ACTIVE) {
            throw InvalidActionException(InvalidActionReason.COMBAT_ENDED)
        }
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

        val damageDealt = minOf(KnightAbilityValues.SLASH_DAMAGE, target.currentHp.value)
        val updatedTarget = target.copy(currentHp = HitPoints(target.currentHp.value - damageDealt))
        val updatedEnemies = state.enemies.map { if (it.entityId == target.entityId) updatedTarget else it }
        val enemyDefeated = updatedTarget.currentHp.value == 0
        val combatWon = enemyDefeated && updatedEnemies.all { it.currentHp.value == 0 }
        // Terminal events follow the action and damage events that caused them. Consumers can
        // therefore animate the hit before the defeat and combat-victory transitions.
        val events =
            buildList {
                add(
                    GameEvent.AbilityUsed(
                        actorId = state.player.entityId,
                        abilityId = action.abilityId,
                        targetId = target.entityId,
                    ),
                )
                add(
                    GameEvent.DamageDealt(
                        sourceId = state.player.entityId,
                        targetId = target.entityId,
                        amount = damageDealt,
                    ),
                )
                if (enemyDefeated) {
                    add(GameEvent.EntityDefeated(target.entityId))
                }
                if (combatWon) {
                    add(GameEvent.CombatWon)
                }
            }

        return ActionResult(
            state =
                state.copy(
                    enemies = updatedEnemies,
                    // A winning action ends resolution immediately; there is no enemy phase after
                    // combat becomes terminal.
                    phase = if (combatWon) CombatPhase.PLAYER else CombatPhase.ENEMY,
                    status = if (combatWon) CombatStatus.WON else CombatStatus.ACTIVE,
                ),
            events = events,
        )
    }

    private fun executeGuard(
        state: CombatState,
        action: GameAction.UseAbility,
    ): ActionResult {
        if (action.targetId != state.player.entityId) {
            // Distinguish a known but illegal enemy target from an identifier that is not part of
            // combat; callers can surface an invalid selection separately from stale input.
            val reason =
                if (state.enemies.any { it.entityId == action.targetId }) {
                    InvalidActionReason.INVALID_TARGET
                } else {
                    InvalidActionReason.TARGET_NOT_FOUND
                }
            throw InvalidActionException(reason)
        }

        val updatedPlayer =
            state.player.copy(block = Block(state.player.block.value + KnightAbilityValues.GUARD_BLOCK))

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
                        amount = KnightAbilityValues.GUARD_BLOCK,
                    ),
                ),
        )
    }
}
