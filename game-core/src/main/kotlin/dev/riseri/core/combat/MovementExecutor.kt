package dev.riseri.core.combat

object MovementExecutor {
    fun execute(
        state: CombatState,
        action: GameAction.MoveUnit,
    ): ActionResult {
        if (state.status != CombatStatus.ACTIVE) throw InvalidActionException(InvalidActionReason.COMBAT_ENDED)
        if (state.phase != CombatPhase.PLAYER) throw InvalidActionException(InvalidActionReason.WRONG_PHASE)
        if (state.player.currentHp.value == 0) throw InvalidActionException(InvalidActionReason.ACTOR_DEFEATED)
        if (state.player.movedThisPhase) throw InvalidActionException(InvalidActionReason.ALREADY_MOVED)
        if (!state.grid.contains(action.destination)) {
            throw InvalidActionException(InvalidActionReason.DESTINATION_OUT_OF_BOUNDS)
        }
        if (action.destination in state.occupiedPositions(state.player.entityId)) {
            throw InvalidActionException(InvalidActionReason.DESTINATION_OCCUPIED)
        }
        if (action.destination !in state.reachablePlayerPositions()) {
            throw InvalidActionException(InvalidActionReason.DESTINATION_UNREACHABLE)
        }

        val from = state.player.position
        return ActionResult(
            state = state.copy(player = state.player.copy(position = action.destination, movedThisPhase = true)),
            events = listOf(GameEvent.EntityMoved(state.player.entityId, from, action.destination)),
        )
    }
}
