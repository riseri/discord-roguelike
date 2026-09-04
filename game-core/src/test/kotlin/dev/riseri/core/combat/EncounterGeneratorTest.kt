package dev.riseri.core.combat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncounterGeneratorTest {
    @Test
    fun `starter encounter contains two positioned goblins deterministically`() {
        val first = EncounterGenerator.generateStarter(definitions, CombatRngState(7_321))
        val second = EncounterGenerator.generateStarter(definitions.reversed(), CombatRngState(7_321))

        assertEquals(first, second)
        assertEquals(CombatRngState(7_321), first.nextRngState)
        assertEquals(2, first.encounter.enemies.size)
        assertEquals(listOf(EntityId("goblin-1"), EntityId("goblin-2")), first.encounter.enemies.map { it.entityId })
        assertEquals(
            setOf(GridPosition(6, 1), GridPosition(6, 4)),
            first.encounter.enemies
                .map { it.position }
                .toSet(),
        )
        assertTrue(first.encounter.enemies.all { it.enemyContentId == EnemyContentId("goblin") && it.maxHp == HitPoints(40) })
    }

    @Test
    fun `fixed seed and definition pool generate the same encounter`() {
        val first = EncounterGenerator.generate(definitions, CombatRngState(7_321))
        val second = EncounterGenerator.generate(definitions.reversed(), CombatRngState(7_321))

        assertEquals(first, second)
        assertNotEquals(CombatRngState(7_321), first.nextRngState)
    }

    @Test
    fun `generates a valid multi-enemy normal combat composition`() {
        val result = EncounterGenerator.generate(definitions, CombatRngState(42))

        assertEquals(2, result.encounter.enemies.size)
        assertEquals(
            2,
            result.encounter.enemies
                .map { it.entityId }
                .distinct()
                .size,
        )
        assertEquals(
            setOf(GridPosition(6, 1), GridPosition(6, 4)),
            result.encounter.enemies
                .map { it.position }
                .toSet(),
        )
        result.encounter.enemies.forEach { enemy ->
            val definition = definitions.single { it.id == enemy.enemyContentId }
            assertEquals(definition.maxHp, enemy.currentHp)
            assertEquals(definition.maxHp, enemy.maxHp)
            assertNull(enemy.currentIntention)
        }
    }

    @Test
    fun `rejects generation without available enemies`() {
        assertFailsWith<IllegalArgumentException> {
            EncounterGenerator.generate(emptyList(), CombatRngState(42))
        }
    }

    @Test
    fun `rejects overlapping encounter positions`() {
        val enemy = definitions.first().createCombatState(EntityId("goblin-1"))

        assertFailsWith<IllegalArgumentException> {
            CombatEncounter(listOf(enemy, enemy.copy(entityId = EntityId("goblin-2"))))
        }
    }

    private val definitions =
        listOf(
            EnemyDefinition(
                id = EnemyContentId("goblin"),
                maxHp = HitPoints(40),
                intentions = listOf(EnemyIntention(IntentionId("stab"), damage = 10)),
            ),
            EnemyDefinition(
                id = EnemyContentId("goblin-brute"),
                maxHp = HitPoints(70),
                intentions =
                    listOf(
                        EnemyIntention(IntentionId("punch"), damage = 8),
                        EnemyIntention(IntentionId("heavy-swing"), damage = 20),
                    ),
            ),
        )
}
