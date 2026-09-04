package dev.riseri.core.run

import dev.riseri.core.combat.AbilityId
import dev.riseri.core.combat.CombatStatus
import dev.riseri.core.combat.EnemyContentId
import dev.riseri.core.combat.EnemyDefinition
import dev.riseri.core.combat.EnemyIntention
import dev.riseri.core.combat.EntityId
import dev.riseri.core.combat.GameAction
import dev.riseri.core.combat.GridPosition
import dev.riseri.core.combat.HitPoints
import dev.riseri.core.combat.IntentionId
import dev.riseri.core.combat.TacticalMovement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RunCombatEngineTest {
    private val goblin =
        EnemyDefinition(
            id = EnemyContentId("goblin"),
            maxHp = HitPoints(40),
            intentions =
                listOf(
                    EnemyIntention(IntentionId("stab"), damage = 10),
                    EnemyIntention(IntentionId("lunge"), damage = 5),
                ),
        )
    private val enemyDefinitions = mapOf(goblin.id to goblin)

    @Test
    fun `starts combat from the current run player and rng state`() {
        val run =
            RunState.initial(RunSeed(42)).copy(
                playerHp = HitPoints(37),
                playerMaxHp = HitPoints(80),
                rngState = RunRngState(7_321),
            )

        val started = RunCombatEngine.start(run, enemyDefinitions)

        assertEquals(HitPoints(37), started.activeCombat?.player?.currentHp)
        assertEquals(HitPoints(80), started.activeCombat?.player?.maxHp)
        assertEquals(started.rngState.value, started.activeCombat?.rngState?.value)
    }

    @Test
    fun `same run seed and action path reproduce encounter behavior`() {
        val first = playOneAction(RunState.initial(RunSeed(7_321)))
        val second = playOneAction(RunState.initial(RunSeed(7_321)))

        assertEquals(first, second)
    }

    @Test
    fun `active combat damage and rng remain synchronized with the run`() {
        val result = playOneAction(RunState.initial(RunSeed(42)))
        val combat = result.state.activeCombat!!

        assertEquals(combat.player.currentHp, result.state.playerHp)
        assertEquals(combat.rngState.value, result.state.rngState.value)
        assertEquals(CombatStatus.ACTIVE, combat.status)
        assertEquals(emptyList(), result.runEvents)
    }

    @Test
    fun `combat victory persists hp and completes the current room`() {
        val result = playToTerminal(RunState.initial(RunSeed(42)))
        val state = result.state

        val combat = state.activeCombat!!
        assertEquals(CombatStatus.WON, combat.status)
        assertEquals(RunStatus.ACTIVE, state.status)
        assertEquals(combat.player.currentHp, state.playerHp)
        assertEquals(setOf(RoomId("start")), state.completedRoomIds)
        assertEquals(listOf(RunEvent.RoomCompleted(RoomId("start"))), result.runEvents)
    }

    @Test
    fun `choosing the next room discards the resolved combat`() {
        val completed = playToTerminal(RunState.initial(RunSeed(42))).state
        val destination =
            completed.dungeonGraph.rooms
                .getValue(completed.currentRoomId)
                .nextRoomIds
                .first()

        val chosen = RunEngine.execute(completed, RunAction.ChooseRoom(destination)).state

        assertEquals(destination, chosen.currentRoomId)
        assertEquals(null, chosen.activeCombat)
    }

    @Test
    fun `combat defeat marks the owning run lost without divergent terminal state`() {
        val lowHealthRun = RunState.initial(RunSeed(42)).copy(playerHp = HitPoints(5))
        val started = RunCombatEngine.start(lowHealthRun, enemyDefinitions)
        val combat = started.activeCombat!!
        val positioned =
            started.copy(
                activeCombat =
                    combat.copy(
                        enemies =
                            combat.enemies.mapIndexed { index, enemy ->
                                enemy.copy(
                                    position =
                                        GridPosition(2 + index, 3),
                                )
                            },
                    ),
            )

        val result = RunCombatEngine.execute(positioned, slash(), enemyDefinitions)
        val endedCombat = result.state.activeCombat!!

        assertEquals(CombatStatus.LOST, endedCombat.status)
        assertEquals(RunStatus.LOST, result.state.status)
        assertEquals(HitPoints(0), result.state.playerHp)
        assertEquals(endedCombat.player.currentHp, result.state.playerHp)
        assertEquals(listOf(RunEvent.RunLost), result.runEvents)

        val exception =
            assertFailsWith<InvalidRunCombatException> {
                RunCombatEngine.execute(result.state, slash(), enemyDefinitions)
            }
        assertEquals(InvalidRunCombatReason.RUN_ENDED, exception.reason)
    }

    @Test
    fun `cannot start another combat for an active or completed encounter`() {
        val started = RunCombatEngine.start(RunState.initial(RunSeed(42)), enemyDefinitions)

        val activeException =
            assertFailsWith<InvalidRunCombatException> {
                RunCombatEngine.start(started, enemyDefinitions)
            }
        assertEquals(InvalidRunCombatReason.COMBAT_ALREADY_STARTED, activeException.reason)

        val completed =
            RunState
                .initial(RunSeed(42))
                .copy(completedRoomIds = setOf(RoomId("start")))
        val completedException =
            assertFailsWith<InvalidRunCombatException> {
                RunCombatEngine.start(completed, enemyDefinitions)
            }
        assertEquals(InvalidRunCombatReason.ROOM_ALREADY_COMPLETED, completedException.reason)
    }

    private fun playOneAction(state: RunState): RunCombatActionResult {
        val started = RunCombatEngine.start(state, enemyDefinitions)
        val destination =
            started.activeCombat!!
                .reachablePlayerPositions()
                .sortedWith(compareBy({ it.y }, { it.x }))
                .first()
        return RunCombatEngine.execute(started, GameAction.MoveUnit(destination), enemyDefinitions)
    }

    private fun playToTerminal(initial: RunState): RunCombatActionResult {
        var state = RunCombatEngine.start(initial, enemyDefinitions)
        var result: RunCombatActionResult? = null
        repeat(40) {
            val combat = state.activeCombat!!
            if (combat.status != CombatStatus.ACTIVE) return result!!
            val target = combat.enemies.first { it.currentHp.value > 0 }
            if (TacticalMovement.distance(combat.player.position, target.position) > 1 && !combat.player.movedThisPhase) {
                val destination =
                    combat.reachablePlayerPositions().minWith(
                        compareBy({
                            TacticalMovement.distance(it, target.position)
                        }, { it.y }, { it.x }),
                    )
                state = RunCombatEngine.execute(state, GameAction.MoveUnit(destination), enemyDefinitions).state
            }
            val nextCombat = state.activeCombat!!
            val nextTarget = nextCombat.enemies.first { it.currentHp.value > 0 }
            val action =
                if (TacticalMovement.distance(nextCombat.player.position, nextTarget.position) ==
                    1
                ) {
                    slash(nextTarget.entityId)
                } else {
                    GameAction.UseAbility(AbilityId.GUARD, EntityId("knight"))
                }
            result = RunCombatEngine.execute(state, action, enemyDefinitions)
            state = result!!.state
        }
        error("Encounter did not terminate")
    }

    private fun slash(targetId: EntityId = EntityId("goblin-1")) = GameAction.UseAbility(AbilityId.SLASH, targetId)
}
