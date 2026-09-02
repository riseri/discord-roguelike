package dev.riseri.core.run

import dev.riseri.core.combat.HitPoints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RunStateTest {
    @Test
    fun `same seed produces identical initial state`() {
        val first = RunState.initial(RunSeed(7_321))
        val second = RunState.initial(RunSeed(7_321))

        assertEquals(first, second)
        assertEquals(RunRngState(7_321), first.rngState)
    }

    @Test
    fun `initial state contains the Knight defaults and no run progress`() {
        val state = RunState.initial(RunSeed(42))

        assertEquals(RunSeed(42), state.seed)
        assertEquals(RunStatus.ACTIVE, state.status)
        assertEquals(HitPoints(100), state.playerHp)
        assertEquals(HitPoints(100), state.playerMaxHp)
        assertEquals(RoomId("start"), state.currentRoomId)
        assertTrue(state.completedRoomIds.isEmpty())
        assertTrue(state.ownedRelicIds.isEmpty())
    }

    @Test
    fun `represents every run status`() {
        val active = runState(status = RunStatus.ACTIVE)
        val won = runState(status = RunStatus.WON)
        val lost = runState(status = RunStatus.LOST, playerHp = HitPoints(0))

        assertEquals(RunStatus.ACTIVE, active.status)
        assertEquals(RunStatus.WON, won.status)
        assertEquals(RunStatus.LOST, lost.status)
    }

    @Test
    fun `tracks completed rooms and owned relics`() {
        val state =
            runState(
                completedRoomIds = setOf(RoomId("room-1"), RoomId("room-2")),
                ownedRelicIds = setOf(RelicId("iron-bulwark")),
            )

        assertEquals(setOf(RoomId("room-1"), RoomId("room-2")), state.completedRoomIds)
        assertEquals(setOf(RelicId("iron-bulwark")), state.ownedRelicIds)
    }

    @Test
    fun `rejects zero maximum hit points`() {
        assertFailsWith<IllegalArgumentException> {
            runState(playerHp = HitPoints(0), playerMaxHp = HitPoints(0))
        }
    }

    @Test
    fun `rejects hit points above maximum hit points`() {
        assertFailsWith<IllegalArgumentException> {
            runState(playerHp = HitPoints(101))
        }
    }

    @Test
    fun `rejects blank room and relic identifiers`() {
        assertFailsWith<IllegalArgumentException> { RoomId(" ") }
        assertFailsWith<IllegalArgumentException> { RelicId("") }
    }

    private fun runState(
        status: RunStatus = RunStatus.ACTIVE,
        playerHp: HitPoints = HitPoints(100),
        playerMaxHp: HitPoints = HitPoints(100),
        completedRoomIds: Set<RoomId> = emptySet(),
        ownedRelicIds: Set<RelicId> = emptySet(),
    ) = RunState(
        seed = RunSeed(42),
        status = status,
        playerHp = playerHp,
        playerMaxHp = playerMaxHp,
        currentRoomId = RoomId("entrance"),
        completedRoomIds = completedRoomIds,
        ownedRelicIds = ownedRelicIds,
        rngState = RunRngState(99),
    )
}
