package dev.riseri.core.combat

data class CombatState(
    val player: PlayerCombatState,
    val enemies: List<EnemyCombatState>,
) {
    init {
        val entityIds = listOf(player.entityId) + enemies.map { it.entityId }
        require(entityIds.distinct().size == entityIds.size) {
            "Combat entity identifiers must be unique"
        }
    }
}
