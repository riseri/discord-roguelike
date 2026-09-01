package dev.riseri.core.combat

@JvmInline
value class EnemyContentId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Enemy content identifier must not be blank" }
    }
}

@JvmInline
value class IntentionId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Intention identifier must not be blank" }
    }
}

data class EnemyIntention(
    val id: IntentionId,
    val damage: Int,
) {
    init {
        require(damage >= 0) { "Intention damage must not be negative" }
    }
}

data class EnemyCombatState(
    val entityId: EntityId,
    val enemyContentId: EnemyContentId,
    val currentHp: HitPoints,
    val maxHp: HitPoints,
    val currentIntention: EnemyIntention?,
) {
    init {
        require(maxHp.value > 0) { "Maximum hit points must be positive" }
        require(currentHp.value <= maxHp.value) {
            "Current hit points must not exceed maximum hit points"
        }
    }
}
