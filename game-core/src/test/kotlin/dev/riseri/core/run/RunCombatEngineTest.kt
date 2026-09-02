package dev.riseri.core.run

import dev.riseri.core.combat.AbilityId
import dev.riseri.core.combat.CombatStatus
import dev.riseri.core.combat.EnemyContentId
import dev.riseri.core.combat.EnemyDefinition
import dev.riseri.core.combat.EnemyIntention
import dev.riseri.core.combat.EntityId
import dev.riseri.core.combat.GameAction
import dev.riseri.core.combat.HitPoints
import dev.riseri.core.combat.IntentionId
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
        var state = RunCombatEngine.start(RunState.initial(RunSeed(42)), enemyDefinitions)
        repeat(2) {
            state = RunCombatEngine.execute(state, slash(), enemyDefinitions).state
        }
        val result = RunCombatEngine.execute(state, slash(), enemyDefinitions)
        state = result.state

        val combat = state.activeCombat!!
        assertEquals(CombatStatus.WON, combat.status)
        assertEquals(RunStatus.ACTIVE, state.status)
        assertEquals(combat.player.currentHp, state.playerHp)
        assertEquals(setOf(RoomId("start")), state.completedRoomIds)
        assertEquals(listOf(RunEvent.RoomCompleted(RoomId("start"))), result.runEvents)
    }

    @Test
    fun `combat defeat marks the owning run lost without divergent terminal state`() {
        val lowHealthRun = RunState.initial(RunSeed(42)).copy(playerHp = HitPoints(5))
        val started = RunCombatEngine.start(lowHealthRun, enemyDefinitions)

        val result = RunCombatEngine.execute(started, slash(), enemyDefinitions)
        val combat = result.state.activeCombat!!

        assertEquals(CombatStatus.LOST, combat.status)
        assertEquals(RunStatus.LOST, result.state.status)
        assertEquals(HitPoints(0), result.state.playerHp)
        assertEquals(combat.player.currentHp, result.state.playerHp)
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

    private fun playOneAction(state: RunState): RunCombatActionResult =
        RunCombatEngine.execute(
            RunCombatEngine.start(state, enemyDefinitions),
            slash(),
            enemyDefinitions,
        )

    private fun slash() = GameAction.UseAbility(AbilityId.SLASH, EntityId("goblin-1"))
}
