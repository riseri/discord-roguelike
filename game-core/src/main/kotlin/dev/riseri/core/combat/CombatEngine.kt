package dev.riseri.core.combat

object CombatEngine {
    /** Resolves a player action and all required enemy activity as one authoritative transition. */
    fun execute(
        state: CombatState,
        action: GameAction.UseAbility,
        enemyDefinitions: Map<EnemyContentId, EnemyDefinition>,
    ): ActionResult {
        val playerResult = AbilityExecutor.execute(state, action)
        if (playerResult.state.status != CombatStatus.ACTIVE) {
            return playerResult
        }

        val enemyResult = EnemyTurnExecutor.execute(playerResult.state, enemyDefinitions)
        return ActionResult(
            state = enemyResult.state,
            events = playerResult.events + enemyResult.events,
        )
    }
}
