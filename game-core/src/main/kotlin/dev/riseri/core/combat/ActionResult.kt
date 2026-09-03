package dev.riseri.core.combat

data class ActionResult(
    val state: CombatState,
    // Events stay in resolution order so callers can present the authoritative sequence without
    // reconstructing combat rules from the final state.
    val events: List<GameEvent>,
)

sealed interface GameEvent {
    data class EntityMoved(
        val entityId: EntityId,
        val from: GridPosition,
        val to: GridPosition,
    ) : GameEvent

    data class EntityStunned(
        val entityId: EntityId,
        val turns: Int,
    ) : GameEvent

    data class AbilityUsed(
        val actorId: EntityId,
        val abilityId: AbilityId,
        val targetId: EntityId,
    ) : GameEvent

    data class DamageDealt(
        val sourceId: EntityId,
        val targetId: EntityId,
        val amount: Int,
    ) : GameEvent

    data class BlockGained(
        val entityId: EntityId,
        val amount: Int,
    ) : GameEvent

    data class EnemyIntentionGenerated(
        val enemyId: EntityId,
        val intention: EnemyIntention,
    ) : GameEvent

    data class EnemyActionUsed(
        val enemyId: EntityId,
        val intentionId: IntentionId,
        val targetId: EntityId,
    ) : GameEvent

    data class BlockAbsorbed(
        val entityId: EntityId,
        val amount: Int,
    ) : GameEvent

    data class EntityDefeated(
        val entityId: EntityId,
    ) : GameEvent

    data object CombatWon : GameEvent

    data object CombatLost : GameEvent
}
