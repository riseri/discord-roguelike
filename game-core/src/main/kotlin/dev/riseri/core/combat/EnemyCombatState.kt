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

data class EnemyDefinition(
    val id: EnemyContentId,
    val maxHp: HitPoints,
    val intentions: List<EnemyIntention>,
) {
    init {
        require(maxHp.value > 0) { "Enemy maximum hit points must be positive" }
        require(intentions.isNotEmpty()) { "Enemy must define at least one intention" }
        require(intentions.map { it.id }.distinct().size == intentions.size) {
            "Enemy intention identifiers must be unique"
        }
    }

    // Intentions are generated separately so the chosen action and RNG advance are captured in the
    // same authoritative transition before the player is asked to act.
    fun createCombatState(entityId: EntityId) =
        EnemyCombatState(
            entityId = entityId,
            enemyContentId = id,
            currentHp = maxHp,
            maxHp = maxHp,
            currentIntention = null,
        )
}

data class EnemyCombatState(
    val entityId: EntityId,
    val enemyContentId: EnemyContentId,
    val currentHp: HitPoints,
    val maxHp: HitPoints,
    val currentIntention: EnemyIntention?,
    val position: GridPosition = GridPosition(2, 3),
    val stunnedTurns: Int = 0,
) {
    init {
        require(maxHp.value > 0) { "Maximum hit points must be positive" }
        require(currentHp.value <= maxHp.value) {
            "Current hit points must not exceed maximum hit points"
        }
        require(stunnedTurns >= 0) { "Stun duration must not be negative" }
    }
}
