package dev.riseri.core.combat

data class PlayerCombatState(
    val entityId: EntityId,
    val currentHp: HitPoints,
    val maxHp: HitPoints,
    val block: Block,
    val position: GridPosition = GridPosition(1, 3),
    val movedThisPhase: Boolean = false,
) {
    init {
        require(maxHp.value > 0) { "Maximum hit points must be positive" }
        require(currentHp.value <= maxHp.value) {
            "Current hit points must not exceed maximum hit points"
        }
    }
}
