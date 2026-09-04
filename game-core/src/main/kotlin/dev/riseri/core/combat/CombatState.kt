package dev.riseri.core.combat

import dev.riseri.core.relic.RelicContentId

enum class CombatPhase {
    PLAYER,
    ENEMY,
}

enum class CombatStatus {
    ACTIVE,
    WON,
    LOST,
}

data class CombatState(
    val player: PlayerCombatState,
    val enemies: List<EnemyCombatState>,
    val phase: CombatPhase = CombatPhase.PLAYER,
    val rngState: CombatRngState = CombatRngState(0),
    val status: CombatStatus = CombatStatus.ACTIVE,
    val grid: TacticalGrid = TacticalGrid(),
    val relicIds: Set<RelicContentId> = emptySet(),
) {
    init {
        val entityIds = listOf(player.entityId) + enemies.map { it.entityId }
        require(entityIds.distinct().size == entityIds.size) {
            "Combat entity identifiers must be unique"
        }
        require(grid.contains(player.position) && enemies.all { grid.contains(it.position) }) {
            "Combatant positions must be inside the tactical grid"
        }
    }

    fun occupiedPositions(excluding: EntityId? = null): Set<GridPosition> =
        buildSet {
            if (player.entityId != excluding && player.currentHp.value > 0) add(player.position)
            enemies.filter { it.entityId != excluding && it.currentHp.value > 0 }.forEach { add(it.position) }
        }

    fun reachablePlayerPositions(): Set<GridPosition> =
        if (status == CombatStatus.ACTIVE && phase == CombatPhase.PLAYER && !player.movedThisPhase) {
            TacticalMovement.reachable(grid, player.position, PLAYER_MOVEMENT, occupiedPositions(player.entityId))
        } else {
            emptySet()
        }

    companion object {
        const val PLAYER_MOVEMENT = 3
        const val ENEMY_MOVEMENT = 2
    }
}
