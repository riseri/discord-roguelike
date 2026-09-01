package dev.riseri.core.combat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombatEngineTest {
    @Test
    fun `ability and enemy turns resolve as one transition`() {
        val definition =
            EnemyDefinition(
                id = EnemyContentId("goblin"),
                maxHp = HitPoints(40),
                intentions = listOf(EnemyIntention(IntentionId("stab"), damage = 10)),
            )
        val state =
            CombatState(
                player =
                    PlayerCombatState(
                        entityId = EntityId("knight"),
                        currentHp = HitPoints(100),
                        maxHp = HitPoints(100),
                        block = Block(0),
                    ),
                enemies =
                    listOf(
                        definition
                            .createCombatState(EntityId("goblin-1"))
                            .copy(currentIntention = definition.intentions.single()),
                    ),
                rngState = CombatRngState(42),
            )

        val result =
            CombatEngine.execute(
                state,
                GameAction.UseAbility(AbilityId.SLASH, EntityId("goblin-1")),
                mapOf(definition.id to definition),
            )

        assertEquals(
            HitPoints(25),
            result.state.enemies
                .single()
                .currentHp,
        )
        assertEquals(HitPoints(90), result.state.player.currentHp)
        assertEquals(CombatPhase.PLAYER, result.state.phase)
        assertTrue(
            result.state.enemies
                .single()
                .currentIntention != null,
        )
    }
}
