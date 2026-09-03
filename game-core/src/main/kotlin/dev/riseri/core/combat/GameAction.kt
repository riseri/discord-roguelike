package dev.riseri.core.combat

enum class AbilityId {
    SLASH,
    GUARD,
    SHIELD_BASH,
}

/** Authoritative base values shared by combat resolution and server presentation models. */
object KnightAbilityValues {
    const val SLASH_DAMAGE = 15
    const val GUARD_BLOCK = 12
    const val SHIELD_BASH_DAMAGE = 8
}

sealed interface GameAction {
    data class MoveUnit(
        val destination: GridPosition,
    ) : GameAction

    data class UseAbility(
        val abilityId: AbilityId,
        val targetId: EntityId,
    ) : GameAction
}
