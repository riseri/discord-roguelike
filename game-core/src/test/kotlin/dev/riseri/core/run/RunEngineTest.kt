package dev.riseri.core.run

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

class RunEngineTest {
    @Test
    fun `starts a run through the authoritative transition path`() {
        val result = RunEngine.execute(null, RunAction.StartRun(RunSeed(42)))

        assertEquals(RunState.initial(RunSeed(42)), result.state)
        assertEquals(listOf(RunEvent.RunStarted(RunSeed(42))), result.events)
    }

    @Test
    fun `same state and action produce the same transition`() {
        val state = RunState.initial(RunSeed(7_321))
        val action = RunAction.CompleteCurrentRoom

        val first = RunEngine.execute(state, action)
        val second = RunEngine.execute(state, action)

        assertEquals(first, second)
        assertEquals(RunRngState(7_321), first.state.rngState)
    }

    @Test
    fun `completes the current room without mutating the prior state`() {
        val state = RunState.initial(RunSeed(42), RoomId("room-1"))

        val result = RunEngine.execute(state, RunAction.CompleteCurrentRoom)

        assertNotSame(state, result.state)
        assertEquals(emptySet(), state.completedRoomIds)
        assertEquals(setOf(RoomId("room-1")), result.state.completedRoomIds)
        assertEquals(listOf(RunEvent.RoomCompleted(RoomId("room-1"))), result.events)
    }

    @Test
    fun `wins an active run`() {
        val result = RunEngine.execute(RunState.initial(RunSeed(42)), RunAction.WinRun)

        assertEquals(RunStatus.WON, result.state.status)
        assertEquals(listOf(RunEvent.RunWon), result.events)
    }

    @Test
    fun `loses an active run`() {
        val result = RunEngine.execute(RunState.initial(RunSeed(42)), RunAction.LoseRun)

        assertEquals(RunStatus.LOST, result.state.status)
        assertEquals(listOf(RunEvent.RunLost), result.events)
    }

    @Test
    fun `rejects lifecycle actions before a run starts`() {
        val exception =
            assertFailsWith<InvalidRunActionException> {
                RunEngine.execute(null, RunAction.CompleteCurrentRoom)
            }

        assertEquals(InvalidRunActionReason.RUN_NOT_STARTED, exception.reason)
    }

    @Test
    fun `rejects starting another active run`() {
        val exception =
            assertFailsWith<InvalidRunActionException> {
                RunEngine.execute(
                    RunState.initial(RunSeed(42)),
                    RunAction.StartRun(RunSeed(99)),
                )
            }

        assertEquals(InvalidRunActionReason.RUN_ALREADY_STARTED, exception.reason)
    }

    @Test
    fun `rejects completing the same room twice`() {
        val state = RunState.initial(RunSeed(42), RoomId("room-1"))
        val completed = RunEngine.execute(state, RunAction.CompleteCurrentRoom).state

        val exception =
            assertFailsWith<InvalidRunActionException> {
                RunEngine.execute(completed, RunAction.CompleteCurrentRoom)
            }

        assertEquals(InvalidRunActionReason.ROOM_ALREADY_COMPLETED, exception.reason)
    }

    @Test
    fun `terminal runs reject every further action with one stable reason`() {
        val actions =
            listOf(
                RunAction.StartRun(RunSeed(99)),
                RunAction.CompleteCurrentRoom,
                RunAction.WinRun,
                RunAction.LoseRun,
            )

        RunStatus.entries.filter { it != RunStatus.ACTIVE }.forEach { status ->
            val terminalState = RunState.initial(RunSeed(42)).copy(status = status)

            actions.forEach { action ->
                val exception =
                    assertFailsWith<InvalidRunActionException> {
                        RunEngine.execute(terminalState, action)
                    }

                assertEquals(InvalidRunActionReason.RUN_ENDED, exception.reason)
            }
        }
    }
}
