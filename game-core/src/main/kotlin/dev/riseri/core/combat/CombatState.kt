package dev.riseri.core.combat

enum class CombatPhase {
    PLAYER,
    ENEMY,
}

data class CombatState(
    val player: PlayerCombatState,
    val enemies: List<EnemyCombatState>,
    val phase: CombatPhase = CombatPhase.PLAYER,
    val rngState: CombatRngState = CombatRngState(0),
) {
    init {
        val entityIds = listOf(player.entityId) + enemies.map { it.entityId }
        require(entityIds.distinct().size == entityIds.size) {
            "Combat entity identifiers must be unique"
        }
    }
}
