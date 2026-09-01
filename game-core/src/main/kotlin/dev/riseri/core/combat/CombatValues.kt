package dev.riseri.core.combat

@JvmInline
value class EntityId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Entity identifier must not be blank" }
    }
}

@JvmInline
value class HitPoints(
    val value: Int,
) {
    init {
        require(value >= 0) { "Hit points must not be negative" }
    }
}

@JvmInline
value class Block(
    val value: Int,
) {
    init {
        require(value >= 0) { "Block must not be negative" }
    }
}
