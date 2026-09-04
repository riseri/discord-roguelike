package dev.riseri.core.combat

import dev.riseri.core.relic.RelicContentId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CombatRelicEffectsTest {
    @Test
    fun `Iron Bulwark adds Block after Guard announces its trigger`() {
        val state = state(relicIds = setOf(ironBulwark), playerBlock = 3)

        val result = AbilityExecutor.execute(state, GameAction.UseAbility(AbilityId.GUARD, knightId))

        assertEquals(Block(20), result.state.player.block)
        assertEquals(
            listOf(
                GameEvent.AbilityUsed(knightId, AbilityId.GUARD, knightId),
                GameEvent.RelicTriggered(ironBulwark, knightId),
                GameEvent.BlockGained(knightId, 17),
            ),
            result.events,
        )
    }

    @Test
    fun `Berserker's Ring adds Slash damage only below forty percent health`() {
        val active = state(relicIds = setOf(berserkersRing), playerHp = 39)
        val atThreshold = state(relicIds = setOf(berserkersRing), playerHp = 40)

        val activeResult = AbilityExecutor.execute(active, slash())
        val thresholdResult = AbilityExecutor.execute(atThreshold, slash())

        assertEquals(
            HitPoints(20),
            activeResult.state.enemies
                .single()
                .currentHp,
        )
        assertEquals(
            listOf(
                GameEvent.AbilityUsed(knightId, AbilityId.SLASH, enemyId),
                GameEvent.RelicTriggered(berserkersRing, knightId),
                GameEvent.DamageDealt(knightId, enemyId, 20),
            ),
            activeResult.events,
        )
        assertEquals(
            HitPoints(25),
            thresholdResult.state.enemies
                .single()
                .currentHp,
        )
        assertFalse(thresholdResult.events.any { it is GameEvent.RelicTriggered })
    }

    @Test
    fun `combat relics do not bypass authoritative melee range`() {
        val state =
            state(
                relicIds = setOf(berserkersRing),
                playerHp = 39,
                enemyPosition = GridPosition(4, 3),
            )

        val exception = assertFailsWith<InvalidActionException> { AbilityExecutor.execute(state, slash()) }

        assertEquals(InvalidActionReason.TARGET_OUT_OF_RANGE, exception.reason)
        assertEquals(HitPoints(40), state.enemies.single().currentHp)
    }

    @Test
    fun `Spiked Armor retaliates after a moved adjacent attack is absorbed`() {
        val state =
            state(
                relicIds = setOf(spikedArmor),
                playerBlock = 8,
                enemyPosition = GridPosition(3, 3),
                enemyHp = 40,
            ).copy(phase = CombatPhase.ENEMY)

        val first = EnemyTurnExecutor.execute(state, definitions)
        val second = EnemyTurnExecutor.execute(state, definitions)

        assertEquals(first, second)
        assertEquals(
            HitPoints(36),
            first.state.enemies
                .single()
                .currentHp,
        )
        assertEquals(HitPoints(100), first.state.player.currentHp)
        assertEquals(Block(0), first.state.player.block)
        assertEquals(
            GridPosition(2, 3),
            first.state.enemies
                .single()
                .position,
        )
        assertEquals(
            listOf(
                GameEvent.EntityMoved(enemyId, GridPosition(3, 3), GridPosition(2, 3)),
                GameEvent.EnemyActionUsed(enemyId, stab.id, knightId),
                GameEvent.BlockAbsorbed(knightId, 8),
                GameEvent.RelicTriggered(spikedArmor, knightId),
                GameEvent.DamageDealt(knightId, enemyId, 4),
                GameEvent.EnemyIntentionGenerated(enemyId, stab),
            ),
            first.events,
        )
    }

    @Test
    fun `lethal Spiked Armor retaliation ends combat without advancing enemy intentions`() {
        val state =
            state(
                relicIds = setOf(spikedArmor),
                playerBlock = 8,
                enemyHp = 4,
            ).copy(phase = CombatPhase.ENEMY)

        val result = EnemyTurnExecutor.execute(state, definitions)

        assertEquals(
            HitPoints(0),
            result.state.enemies
                .single()
                .currentHp,
        )
        assertEquals(CombatStatus.WON, result.state.status)
        assertEquals(CombatPhase.PLAYER, result.state.phase)
        assertTrue(result.state.occupiedPositions().contains(GridPosition(1, 3)))
        assertFalse(result.state.occupiedPositions().contains(GridPosition(2, 3)))
        assertEquals(
            listOf(
                GameEvent.EnemyActionUsed(enemyId, stab.id, knightId),
                GameEvent.BlockAbsorbed(knightId, 8),
                GameEvent.RelicTriggered(spikedArmor, knightId),
                GameEvent.DamageDealt(knightId, enemyId, 4),
                GameEvent.EntityDefeated(enemyId),
                GameEvent.CombatWon,
            ),
            result.events,
        )
    }

    @Test
    fun `Spiked Armor does not trigger when Block absorbs no damage`() {
        val state = state(relicIds = setOf(spikedArmor)).copy(phase = CombatPhase.ENEMY)

        val result = EnemyTurnExecutor.execute(state, definitions)

        assertEquals(
            HitPoints(40),
            result.state.enemies
                .single()
                .currentHp,
        )
        assertFalse(result.events.any { it is GameEvent.RelicTriggered })
    }

    private fun state(
        relicIds: Set<RelicContentId>,
        playerHp: Int = 100,
        playerBlock: Int = 0,
        enemyPosition: GridPosition = GridPosition(2, 3),
        enemyHp: Int = 40,
    ) = CombatState(
        player = PlayerCombatState(knightId, HitPoints(playerHp), HitPoints(100), Block(playerBlock)),
        enemies =
            listOf(
                definition
                    .createCombatState(enemyId)
                    .copy(
                        currentHp = HitPoints(enemyHp),
                        currentIntention = stab,
                        position = enemyPosition,
                    ),
            ),
        rngState = CombatRngState(42),
        relicIds = relicIds,
    )

    private fun slash() = GameAction.UseAbility(AbilityId.SLASH, enemyId)

    private val knightId = EntityId("knight")
    private val enemyId = EntityId("goblin-1")
    private val ironBulwark = RelicContentId("iron-bulwark")
    private val spikedArmor = RelicContentId("spiked-armor")
    private val berserkersRing = RelicContentId("berserkers-ring")
    private val stab = EnemyIntention(IntentionId("stab"), damage = 8)
    private val definition = EnemyDefinition(EnemyContentId("goblin"), HitPoints(40), listOf(stab))
    private val definitions = mapOf(definition.id to definition)
}
