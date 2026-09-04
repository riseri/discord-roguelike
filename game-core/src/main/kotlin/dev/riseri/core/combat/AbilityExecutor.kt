package dev.riseri.core.combat

enum class InvalidActionReason {
    COMBAT_ENDED,
    WRONG_PHASE,
    ACTOR_DEFEATED,
    TARGET_NOT_FOUND,
    INVALID_TARGET,
    TARGET_DEFEATED,
    TARGET_OUT_OF_RANGE,
    ALREADY_MOVED,
    DESTINATION_OUT_OF_BOUNDS,
    DESTINATION_OCCUPIED,
    DESTINATION_UNREACHABLE,
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
            AbilityId.SHIELD_BASH -> executeAttack(state, action, KnightAbilityValues.SHIELD_BASH_DAMAGE, stun = true)
        }
    }

    private fun executeSlash(
        state: CombatState,
        action: GameAction.UseAbility,
    ): ActionResult {
        val relicEffect = CombatRelicEffects.slashBonus(state.relicIds, state.player)
        return executeAttack(
            state,
            action,
            KnightAbilityValues.SLASH_DAMAGE + (relicEffect?.amount ?: 0),
            stun = false,
            relicEffect = relicEffect,
        )
    }

    private fun executeAttack(
        state: CombatState,
        action: GameAction.UseAbility,
        damage: Int,
        stun: Boolean,
        relicEffect: TriggeredRelicEffect? = null,
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
        if (TacticalMovement.distance(state.player.position, target.position) != 1) {
            throw InvalidActionException(InvalidActionReason.TARGET_OUT_OF_RANGE)
        }

        val damageDealt = minOf(damage, target.currentHp.value)
        val updatedTarget =
            target.copy(
                currentHp = HitPoints(target.currentHp.value - damageDealt),
                stunnedTurns = if (stun && damageDealt > 0) 1 else target.stunnedTurns,
            )
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
                if (relicEffect != null) {
                    add(GameEvent.RelicTriggered(relicEffect.relicId, state.player.entityId))
                }
                if (stun && !enemyDefeated) add(GameEvent.EntityStunned(target.entityId, 1))
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

        val relicEffect = CombatRelicEffects.guardBonus(state.relicIds)
        val blockGained = KnightAbilityValues.GUARD_BLOCK + (relicEffect?.amount ?: 0)
        val updatedPlayer = state.player.copy(block = Block(state.player.block.value + blockGained))

        return ActionResult(
            state = state.copy(player = updatedPlayer, phase = CombatPhase.ENEMY),
            events =
                buildList {
                    add(
                        GameEvent.AbilityUsed(
                            actorId = state.player.entityId,
                            abilityId = action.abilityId,
                            targetId = state.player.entityId,
                        ),
                    )
                    if (relicEffect != null) {
                        add(GameEvent.RelicTriggered(relicEffect.relicId, state.player.entityId))
                    }
                    add(GameEvent.BlockGained(entityId = state.player.entityId, amount = blockGained))
                },
        )
    }
}
