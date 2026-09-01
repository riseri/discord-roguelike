package dev.riseri.core.combat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AbilityExecutorTest {
    @Test
    fun `Slash damages one enemy and emits presentation events`() {
        val state = combatState()

        val result =
            AbilityExecutor.execute(
                state,
                GameAction.UseAbility(AbilityId.SLASH, EntityId("goblin-1")),
            )

        assertEquals(HitPoints(25), result.state.enemies[0].currentHp)
        assertEquals(HitPoints(40), state.enemies[0].currentHp)
        assertEquals(
            listOf(
                GameEvent.AbilityUsed(
                    actorId = EntityId("knight"),
                    abilityId = AbilityId.SLASH,
                    targetId = EntityId("goblin-1"),
                ),
                GameEvent.DamageDealt(
                    sourceId = EntityId("knight"),
                    targetId = EntityId("goblin-1"),
                    amount = 15,
                ),
            ),
            result.events,
        )
    }

    @Test
    fun `Slash clamps lethal damage at zero hit points`() {
        val state =
            combatState(
                enemy = enemy(currentHp = 10),
            )

        val result =
            AbilityExecutor.execute(
                state,
                GameAction.UseAbility(AbilityId.SLASH, EntityId("goblin-1")),
            )

        assertEquals(
            HitPoints(0),
            result.state.enemies
                .single()
                .currentHp,
        )
        assertEquals(
            GameEvent.DamageDealt(EntityId("knight"), EntityId("goblin-1"), 10),
            result.events.last(),
        )
    }

    @Test
    fun `Guard grants Block to the player and emits presentation events`() {
        val state = combatState(playerBlock = 3)

        val result =
            AbilityExecutor.execute(
                state,
                GameAction.UseAbility(AbilityId.GUARD, EntityId("knight")),
            )

        assertEquals(Block(15), result.state.player.block)
        assertEquals(Block(3), state.player.block)
        assertEquals(
            listOf(
                GameEvent.AbilityUsed(
                    actorId = EntityId("knight"),
                    abilityId = AbilityId.GUARD,
                    targetId = EntityId("knight"),
                ),
                GameEvent.BlockGained(EntityId("knight"), 12),
            ),
            result.events,
        )
    }

    @Test
    fun `rejects an unknown target predictably`() {
        val exception =
            assertFailsWith<InvalidActionException> {
                AbilityExecutor.execute(
                    combatState(),
                    GameAction.UseAbility(AbilityId.SLASH, EntityId("missing")),
                )
            }

        assertEquals(InvalidActionReason.TARGET_NOT_FOUND, exception.reason)
    }

    @Test
    fun `rejects an invalid ability target predictably`() {
        val exception =
            assertFailsWith<InvalidActionException> {
                AbilityExecutor.execute(
                    combatState(),
                    GameAction.UseAbility(AbilityId.GUARD, EntityId("goblin-1")),
                )
            }

        assertEquals(InvalidActionReason.INVALID_TARGET, exception.reason)
    }

    @Test
    fun `rejects targeting a defeated enemy`() {
        val exception =
            assertFailsWith<InvalidActionException> {
                AbilityExecutor.execute(
                    combatState(enemy = enemy(currentHp = 0)),
                    GameAction.UseAbility(AbilityId.SLASH, EntityId("goblin-1")),
                )
            }

        assertEquals(InvalidActionReason.TARGET_DEFEATED, exception.reason)
    }

    @Test
    fun `rejects ability use by a defeated player`() {
        val exception =
            assertFailsWith<InvalidActionException> {
                AbilityExecutor.execute(
                    combatState(playerHp = 0),
                    GameAction.UseAbility(AbilityId.GUARD, EntityId("knight")),
                )
            }

        assertEquals(InvalidActionReason.ACTOR_DEFEATED, exception.reason)
    }

    @Test
    fun `combat state rejects duplicate entity identifiers`() {
        assertFailsWith<IllegalArgumentException> {
            CombatState(
                player = player(),
                enemies = listOf(enemy(entityId = "knight")),
            )
        }
    }

    private fun combatState(
        playerHp: Int = 100,
        playerBlock: Int = 0,
        enemy: EnemyCombatState = enemy(),
    ) = CombatState(
        player = player(currentHp = playerHp, block = playerBlock),
        enemies = listOf(enemy),
    )

    private fun player(
        currentHp: Int = 100,
        block: Int = 0,
    ) = PlayerCombatState(
        entityId = EntityId("knight"),
        currentHp = HitPoints(currentHp),
        maxHp = HitPoints(100),
        block = Block(block),
    )

    private fun enemy(
        entityId: String = "goblin-1",
        currentHp: Int = 40,
    ) = EnemyCombatState(
        entityId = EntityId(entityId),
        enemyContentId = EnemyContentId("goblin"),
        currentHp = HitPoints(currentHp),
        maxHp = HitPoints(40),
        currentIntentionId = IntentionId("stab"),
    )
}
