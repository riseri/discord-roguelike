package dev.riseri.core.combat

enum class AbilityId {
    SLASH,
    GUARD,
}

sealed interface GameAction {
    data class UseAbility(
        val abilityId: AbilityId,
        val targetId: EntityId,
    ) : GameAction
}
