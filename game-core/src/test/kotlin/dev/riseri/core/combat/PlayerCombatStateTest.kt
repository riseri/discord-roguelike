package dev.riseri.core.combat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerCombatStateTest {
    @Test
    fun `constructs the Knight's initial combat state`() {
        val state =
            PlayerCombatState(
                entityId = EntityId("knight"),
                currentHp = HitPoints(100),
                maxHp = HitPoints(100),
                block = Block(0),
            )

        assertEquals(EntityId("knight"), state.entityId)
        assertEquals(HitPoints(100), state.currentHp)
        assertEquals(HitPoints(100), state.maxHp)
        assertEquals(Block(0), state.block)
    }

    @Test
    fun `allows zero current hit points for a defeated player`() {
        val state =
            PlayerCombatState(
                entityId = EntityId("knight"),
                currentHp = HitPoints(0),
                maxHp = HitPoints(100),
                block = Block(0),
            )

        assertEquals(HitPoints(0), state.currentHp)
    }

    @Test
    fun `rejects a blank entity identifier`() {
        assertFailsWith<IllegalArgumentException> { EntityId(" ") }
    }

    @Test
    fun `rejects negative hit points`() {
        assertFailsWith<IllegalArgumentException> { HitPoints(-1) }
    }

    @Test
    fun `rejects negative block`() {
        assertFailsWith<IllegalArgumentException> { Block(-1) }
    }

    @Test
    fun `rejects zero maximum hit points`() {
        assertFailsWith<IllegalArgumentException> {
            PlayerCombatState(
                entityId = EntityId("knight"),
                currentHp = HitPoints(0),
                maxHp = HitPoints(0),
                block = Block(0),
            )
        }
    }

    @Test
    fun `rejects current hit points above maximum hit points`() {
        assertFailsWith<IllegalArgumentException> {
            PlayerCombatState(
                entityId = EntityId("knight"),
                currentHp = HitPoints(101),
                maxHp = HitPoints(100),
                block = Block(0),
            )
        }
    }
}
