package dev.riseri.core.combat

import dev.riseri.core.relic.RelicContentId

internal data class TriggeredRelicEffect(
    val relicId: RelicContentId,
    val amount: Int,
)

/** Defines the explicit combat hooks used by the first tactical relics. */
internal object CombatRelicEffects {
    private val ironBulwark = RelicContentId("iron-bulwark")
    private val spikedArmor = RelicContentId("spiked-armor")
    private val berserkersRing = RelicContentId("berserkers-ring")

    fun guardBonus(relicIds: Set<RelicContentId>): TriggeredRelicEffect? = relicIds.trigger(ironBulwark, IRON_BULWARK_BLOCK)

    fun slashBonus(
        relicIds: Set<RelicContentId>,
        player: PlayerCombatState,
    ): TriggeredRelicEffect? =
        relicIds
            .trigger(berserkersRing, BERSERKERS_RING_DAMAGE)
            // Cross multiplication keeps the strict threshold exact without floating-point state.
            ?.takeIf {
                player.currentHp.value.toLong() * 100 <
                    player.maxHp.value.toLong() * BERSERKERS_RING_THRESHOLD_PERCENT
            }

    fun blockRetaliation(
        relicIds: Set<RelicContentId>,
        blockAbsorbed: Int,
        playerPosition: GridPosition,
        attackerPosition: GridPosition,
    ): TriggeredRelicEffect? =
        relicIds
            .trigger(spikedArmor, SPIKED_ARMOR_DAMAGE)
            ?.takeIf {
                blockAbsorbed > 0 && TacticalMovement.distance(playerPosition, attackerPosition) == 1
            }

    private fun Set<RelicContentId>.trigger(
        relicId: RelicContentId,
        amount: Int,
    ): TriggeredRelicEffect? = if (relicId in this) TriggeredRelicEffect(relicId, amount) else null

    private const val IRON_BULWARK_BLOCK = 5
    private const val SPIKED_ARMOR_DAMAGE = 4
    private const val BERSERKERS_RING_DAMAGE = 5
    private const val BERSERKERS_RING_THRESHOLD_PERCENT = 40
}
