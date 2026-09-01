package dev.riseri.core.combat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnemyTurnExecutorTest {
    @Test
    fun `fixed seed generates deterministic enemy intentions`() {
        val first = EnemyTurnExecutor.generateIntentions(state(seed = 1_234, intention = null))
        val second = EnemyTurnExecutor.generateIntentions(state(seed = 1_234, intention = null))

        assertEquals(first, second)
        assertTrue(
            first.state.enemies
                .single()
                .currentIntention != null,
        )
        assertEquals(1, first.events.size)
    }

    @Test
    fun `visible intention remains stable without consuming randomness`() {
        val intention = EnemyIntention(IntentionId("heavy-swing"), damage = 20)
        val state = state(seed = 99, intention = intention)

        val result = EnemyTurnExecutor.generateIntentions(state)

        assertEquals(
            intention,
            result.state.enemies
                .single()
                .currentIntention,
        )
        assertEquals(CombatRngState(99), result.state.rngState)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `enemy action uses visible intention and Block absorbs all damage first`() {
        val result =
            EnemyTurnExecutor.execute(
                state(
                    playerBlock = 12,
                    intention = EnemyIntention(IntentionId("punch"), damage = 8),
                ),
            )

        assertEquals(HitPoints(100), result.state.player.currentHp)
        assertEquals(Block(4), result.state.player.block)
        assertEquals(CombatPhase.PLAYER, result.state.phase)
        assertEquals(
            GameEvent.EnemyActionUsed(
                enemyId = EntityId("goblin-brute-1"),
                intentionId = IntentionId("punch"),
                targetId = EntityId("knight"),
            ),
            result.events[0],
        )
        assertEquals(GameEvent.BlockAbsorbed(EntityId("knight"), 8), result.events[1])
    }

    @Test
    fun `damage exceeding Block reduces HP by the remainder`() {
        val result =
            EnemyTurnExecutor.execute(
                state(
                    playerBlock = 12,
                    intention = EnemyIntention(IntentionId("heavy-swing"), damage = 20),
                ),
            )

        assertEquals(Block(0), result.state.player.block)
        assertEquals(HitPoints(92), result.state.player.currentHp)
        assertEquals(GameEvent.BlockAbsorbed(EntityId("knight"), 12), result.events[1])
        assertEquals(
            GameEvent.DamageDealt(
                sourceId = EntityId("goblin-brute-1"),
                targetId = EntityId("knight"),
                amount = 8,
            ),
            result.events[2],
        )
    }

    @Test
    fun `enemy phase resolves enemies in state order and generates next intentions`() {
        val firstEnemy =
            enemy(
                entityId = "goblin-brute-1",
                intention = EnemyIntention(IntentionId("punch"), damage = 8),
            )
        val secondEnemy =
            EnemyCombatState(
                entityId = EntityId("goblin-1"),
                enemyContentId = EnemyContentId("goblin"),
                currentHp = HitPoints(40),
                maxHp = HitPoints(40),
                currentIntention = EnemyIntention(IntentionId("stab"), damage = 10),
            )
        val state =
            CombatState(
                player = player(block = 0),
                enemies = listOf(firstEnemy, secondEnemy),
                phase = CombatPhase.ENEMY,
                rngState = CombatRngState(42),
            )

        val result = EnemyTurnExecutor.execute(state)

        assertEquals(HitPoints(82), result.state.player.currentHp)
        assertEquals(
            listOf(EntityId("goblin-brute-1"), EntityId("goblin-1")),
            result.events
                .filterIsInstance<GameEvent.EnemyActionUsed>()
                .map { it.enemyId },
        )
        assertTrue(result.state.enemies.all { it.currentIntention != null })
        assertEquals(CombatPhase.PLAYER, result.state.phase)
    }

    private fun state(
        seed: Long = 42,
        playerBlock: Int = 0,
        intention: EnemyIntention? = EnemyIntention(IntentionId("punch"), damage = 8),
    ) = CombatState(
        player = player(block = playerBlock),
        enemies = listOf(enemy(intention = intention)),
        phase = CombatPhase.ENEMY,
        rngState = CombatRngState(seed),
    )

    private fun player(block: Int) =
        PlayerCombatState(
            entityId = EntityId("knight"),
            currentHp = HitPoints(100),
            maxHp = HitPoints(100),
            block = Block(block),
        )

    private fun enemy(
        entityId: String = "goblin-brute-1",
        intention: EnemyIntention?,
    ) = EnemyCombatState(
        entityId = EntityId(entityId),
        enemyContentId = EnemyContentId("goblin-brute"),
        currentHp = HitPoints(70),
        maxHp = HitPoints(70),
        currentIntention = intention,
    )
}
