package dev.riseri.core.combat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class EnemyCombatStateTest {
    @Test
    fun `constructs an enemy with a visible intention reference`() {
        val state =
            EnemyCombatState(
                entityId = EntityId("goblin-brute-1"),
                enemyContentId = EnemyContentId("goblin-brute"),
                currentHp = HitPoints(70),
                maxHp = HitPoints(70),
                currentIntentionId = IntentionId("heavy-swing"),
            )

        assertEquals(EntityId("goblin-brute-1"), state.entityId)
        assertEquals(EnemyContentId("goblin-brute"), state.enemyContentId)
        assertEquals(HitPoints(70), state.currentHp)
        assertEquals(HitPoints(70), state.maxHp)
        assertEquals(IntentionId("heavy-swing"), state.currentIntentionId)
    }

    @Test
    fun `allows an enemy to exist before an intention is generated`() {
        val state =
            EnemyCombatState(
                entityId = EntityId("goblin-1"),
                enemyContentId = EnemyContentId("goblin"),
                currentHp = HitPoints(40),
                maxHp = HitPoints(40),
                currentIntentionId = null,
            )

        assertNull(state.currentIntentionId)
    }

    @Test
    fun `supports multiple instances of the same enemy content`() {
        val enemies =
            listOf(
                EnemyCombatState(
                    entityId = EntityId("goblin-1"),
                    enemyContentId = EnemyContentId("goblin"),
                    currentHp = HitPoints(40),
                    maxHp = HitPoints(40),
                    currentIntentionId = IntentionId("stab"),
                ),
                EnemyCombatState(
                    entityId = EntityId("goblin-2"),
                    enemyContentId = EnemyContentId("goblin"),
                    currentHp = HitPoints(25),
                    maxHp = HitPoints(40),
                    currentIntentionId = IntentionId("stab"),
                ),
            )

        assertEquals(2, enemies.map { it.entityId }.distinct().size)
        assertEquals(1, enemies.map { it.enemyContentId }.distinct().size)
    }

    @Test
    fun `allows zero current hit points for a defeated enemy`() {
        val state =
            EnemyCombatState(
                entityId = EntityId("goblin-1"),
                enemyContentId = EnemyContentId("goblin"),
                currentHp = HitPoints(0),
                maxHp = HitPoints(40),
                currentIntentionId = null,
            )

        assertEquals(HitPoints(0), state.currentHp)
    }

    @Test
    fun `rejects a blank enemy content identifier`() {
        assertFailsWith<IllegalArgumentException> { EnemyContentId(" ") }
    }

    @Test
    fun `rejects a blank intention identifier`() {
        assertFailsWith<IllegalArgumentException> { IntentionId("") }
    }

    @Test
    fun `rejects zero maximum hit points`() {
        assertFailsWith<IllegalArgumentException> {
            EnemyCombatState(
                entityId = EntityId("goblin-1"),
                enemyContentId = EnemyContentId("goblin"),
                currentHp = HitPoints(0),
                maxHp = HitPoints(0),
                currentIntentionId = null,
            )
        }
    }

    @Test
    fun `rejects current hit points above maximum hit points`() {
        assertFailsWith<IllegalArgumentException> {
            EnemyCombatState(
                entityId = EntityId("goblin-1"),
                enemyContentId = EnemyContentId("goblin"),
                currentHp = HitPoints(41),
                maxHp = HitPoints(40),
                currentIntentionId = null,
            )
        }
    }
}
