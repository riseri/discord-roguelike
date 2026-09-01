package dev.riseri.core.combat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CombatTerminalStateTest {
    @Test
    fun `lethal player damage defeats the final enemy and wins combat`() {
        val result =
            AbilityExecutor.execute(
                combatState(enemies = listOf(enemy("goblin-1", currentHp = 10))),
                GameAction.UseAbility(AbilityId.SLASH, EntityId("goblin-1")),
            )

        assertEquals(
            HitPoints(0),
            result.state.enemies
                .single()
                .currentHp,
        )
        assertEquals(CombatStatus.WON, result.state.status)
        assertEquals(
            listOf(
                GameEvent.EntityDefeated(EntityId("goblin-1")),
                GameEvent.CombatWon,
            ),
            result.events.takeLast(2),
        )
    }

    @Test
    fun `defeating one enemy does not win while another enemy is alive`() {
        val result =
            AbilityExecutor.execute(
                combatState(
                    enemies =
                        listOf(
                            enemy("goblin-1", currentHp = 10),
                            enemy("goblin-2", currentHp = 40),
                        ),
                ),
                GameAction.UseAbility(AbilityId.SLASH, EntityId("goblin-1")),
            )

        assertEquals(CombatStatus.ACTIVE, result.state.status)
        assertEquals(CombatPhase.ENEMY, result.state.phase)
        assertTrue(GameEvent.EntityDefeated(EntityId("goblin-1")) in result.events)
        assertTrue(GameEvent.CombatWon !in result.events)
    }

    @Test
    fun `lethal enemy damage clamps player HP and ends combat failure`() {
        val result =
            EnemyTurnExecutor.execute(
                combatState(
                    enemies =
                        listOf(
                            enemy(
                                entityId = "goblin-brute-1",
                                currentHp = 70,
                                maxHp = 70,
                                contentId = "goblin-brute",
                                intention = EnemyIntention(IntentionId("crushing-blow"), damage = 120),
                            ),
                        ),
                    phase = CombatPhase.ENEMY,
                ),
            )

        assertEquals(HitPoints(0), result.state.player.currentHp)
        assertEquals(CombatStatus.LOST, result.state.status)
        assertEquals(
            listOf(
                GameEvent.EntityDefeated(EntityId("knight")),
                GameEvent.CombatLost,
            ),
            result.events.takeLast(2),
        )
    }

    @Test
    fun `player death prevents later enemies and normal continuation`() {
        val result =
            EnemyTurnExecutor.execute(
                combatState(
                    enemies =
                        listOf(
                            enemy(
                                entityId = "goblin-brute-1",
                                currentHp = 70,
                                maxHp = 70,
                                contentId = "goblin-brute",
                                intention = EnemyIntention(IntentionId("crushing-blow"), damage = 120),
                            ),
                            enemy(
                                entityId = "goblin-2",
                                intention = EnemyIntention(IntentionId("stab"), damage = 10),
                            ),
                        ),
                    phase = CombatPhase.ENEMY,
                ),
            )

        assertEquals(
            listOf(EntityId("goblin-brute-1")),
            result.events
                .filterIsInstance<GameEvent.EnemyActionUsed>()
                .map { it.enemyId },
        )
        assertEquals(
            EnemyIntention(IntentionId("stab"), damage = 10),
            result.state.enemies[1].currentIntention,
        )
        assertFailsWith<IllegalArgumentException> {
            EnemyTurnExecutor.execute(result.state)
        }

        val exception =
            assertFailsWith<InvalidActionException> {
                AbilityExecutor.execute(
                    result.state,
                    GameAction.UseAbility(AbilityId.GUARD, EntityId("knight")),
                )
            }
        assertEquals(InvalidActionReason.COMBAT_ENDED, exception.reason)
    }

    private fun combatState(
        enemies: List<EnemyCombatState>,
        phase: CombatPhase = CombatPhase.PLAYER,
    ) = CombatState(
        player =
            PlayerCombatState(
                entityId = EntityId("knight"),
                currentHp = HitPoints(100),
                maxHp = HitPoints(100),
                block = Block(0),
            ),
        enemies = enemies,
        phase = phase,
        rngState = CombatRngState(42),
    )

    private fun enemy(
        entityId: String,
        currentHp: Int = 40,
        maxHp: Int = 40,
        contentId: String = "goblin",
        intention: EnemyIntention? = EnemyIntention(IntentionId("stab"), damage = 10),
    ) = EnemyCombatState(
        entityId = EntityId(entityId),
        enemyContentId = EnemyContentId(contentId),
        currentHp = HitPoints(currentHp),
        maxHp = HitPoints(maxHp),
        currentIntention = intention,
    )
}
