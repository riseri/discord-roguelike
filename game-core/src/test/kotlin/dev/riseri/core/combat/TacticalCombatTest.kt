package dev.riseri.core.combat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TacticalCombatTest {
    private val definition =
        EnemyDefinition(
            EnemyContentId("goblin"),
            HitPoints(40),
            listOf(EnemyIntention(IntentionId("stab"), 10)),
        )

    @Test
    fun `movement range is deterministic and routes around occupied tiles`() {
        val state = state(enemyPosition = GridPosition(2, 3))
        val first = state.reachablePlayerPositions()
        val second = state.reachablePlayerPositions()

        assertEquals(first, second)
        assertFalse(GridPosition(2, 3) in first)
        assertTrue(GridPosition(1, 0) in first)
        assertFalse(GridPosition(7, 5) in first)
    }

    @Test
    fun `legal move preserves player phase and rejects occupied or repeated moves`() {
        val state = state(enemyPosition = GridPosition(2, 3))
        val moved = MovementExecutor.execute(state, GameAction.MoveUnit(GridPosition(1, 1))).state

        assertEquals(GridPosition(1, 1), moved.player.position)
        assertEquals(CombatPhase.PLAYER, moved.phase)
        assertTrue(moved.player.movedThisPhase)
        assertEquals(InvalidActionReason.DESTINATION_OCCUPIED, failure(state, GridPosition(2, 3)).reason)
        assertEquals(InvalidActionReason.ALREADY_MOVED, failure(moved, GridPosition(1, 2)).reason)
    }

    @Test
    fun `melee attacks require an adjacent living enemy`() {
        val distant = state(enemyPosition = GridPosition(4, 3))
        val failure =
            assertFailsWith<InvalidActionException> {
                AbilityExecutor.execute(distant, GameAction.UseAbility(AbilityId.SLASH, EntityId("goblin-1")))
            }
        assertEquals(InvalidActionReason.TARGET_OUT_OF_RANGE, failure.reason)

        val adjacent = state(enemyPosition = GridPosition(2, 3))
        val result = AbilityExecutor.execute(adjacent, GameAction.UseAbility(AbilityId.SHIELD_BASH, EntityId("goblin-1")))
        assertEquals(
            HitPoints(32),
            result.state.enemies
                .single()
                .currentHp,
        )
        assertEquals(
            1,
            result.state.enemies
                .single()
                .stunnedTurns,
        )
    }

    @Test
    fun `enemy phase follows a stable shortest path then returns control`() {
        val initial = state(enemyPosition = GridPosition(6, 3)).copy(phase = CombatPhase.ENEMY)
        val first = EnemyTurnExecutor.execute(initial, mapOf(definition.id to definition))
        val second = EnemyTurnExecutor.execute(initial, mapOf(definition.id to definition))

        assertEquals(first, second)
        assertEquals(
            GridPosition(4, 3),
            first.state.enemies
                .single()
                .position,
        )
        assertEquals(HitPoints(100), first.state.player.currentHp)
        assertEquals(CombatPhase.PLAYER, first.state.phase)
    }

    private fun state(enemyPosition: GridPosition) =
        CombatState(
            player = PlayerCombatState(EntityId("knight"), HitPoints(100), HitPoints(100), Block(0)),
            enemies =
                listOf(
                    definition
                        .createCombatState(
                            EntityId("goblin-1"),
                        ).copy(position = enemyPosition, currentIntention = definition.intentions.single()),
                ),
            rngState = CombatRngState(42),
        )

    private fun failure(
        state: CombatState,
        destination: GridPosition,
    ) = assertFailsWith<InvalidActionException> { MovementExecutor.execute(state, GameAction.MoveUnit(destination)) }
}
