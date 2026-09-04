package dev.riseri.core.run

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

class RunEngineTest {
    @Test
    fun `available room choices follow authoritative navigation gates`() {
        val initial = RunState.initial(RunSeed(42))

        assertEquals(emptyList(), RunEngine.availableNextRoomIds(initial))

        val completed = RunEngine.execute(initial, RunAction.CompleteCurrentRoom).state

        assertEquals(
            completed.dungeonGraph.rooms
                .getValue(completed.currentRoomId)
                .nextRoomIds,
            RunEngine.availableNextRoomIds(completed),
        )
        assertEquals(
            emptyList(),
            RunEngine.availableNextRoomIds(completed.copy(status = RunStatus.WON)),
        )
    }

    @Test
    fun `starts a run through the authoritative transition path`() {
        val result = RunEngine.execute(null, RunAction.StartRun(RunSeed(42)))

        assertEquals(RunState.initial(RunSeed(42)), result.state)
        assertEquals(listOf(RunEvent.RunStarted(RunSeed(42))), result.events)
    }

    @Test
    fun `same state and action produce the same transition`() {
        val state = RunState.initial(RunSeed(7_321))
        val completed = RunEngine.execute(state, RunAction.CompleteCurrentRoom).state
        val action =
            RunAction.ChooseRoom(
                completed.dungeonGraph.rooms
                    .getValue(completed.currentRoomId)
                    .nextRoomIds
                    .first(),
            )

        val first = RunEngine.execute(completed, action)
        val second = RunEngine.execute(completed, action)

        assertEquals(first, second)
        assertEquals(completed.rngState, first.state.rngState)
    }

    @Test
    fun `completes the current room without mutating the prior state`() {
        val state = RunState.initial(RunSeed(42))

        val result = RunEngine.execute(state, RunAction.CompleteCurrentRoom)

        assertNotSame(state, result.state)
        assertEquals(emptySet(), state.completedRoomIds)
        assertEquals(setOf(RoomId("start")), result.state.completedRoomIds)
        assertEquals(listOf(RunEvent.RoomCompleted(RoomId("start"))), result.events)
    }

    @Test
    fun `chooses a reachable room through an immutable transition`() {
        val initial = RunState.initial(RunSeed(42))
        val completed = RunEngine.execute(initial, RunAction.CompleteCurrentRoom).state
        val destination =
            completed.dungeonGraph.rooms
                .getValue(completed.currentRoomId)
                .nextRoomIds
                .first()

        val result = RunEngine.execute(completed, RunAction.ChooseRoom(destination))

        assertNotSame(completed, result.state)
        assertEquals(RoomId("start"), completed.currentRoomId)
        assertEquals(destination, result.state.currentRoomId)
        assertEquals(completed.completedRoomIds, result.state.completedRoomIds)
        assertEquals(listOf(RunEvent.RoomChosen(destination)), result.events)
    }

    @Test
    fun `rejects room choices until the current room is completed`() {
        val state = RunState.initial(RunSeed(42))
        val destination =
            state.dungeonGraph.rooms
                .getValue(state.currentRoomId)
                .nextRoomIds
                .first()

        val exception =
            assertFailsWith<InvalidRunActionException> {
                RunEngine.execute(state, RunAction.ChooseRoom(destination))
            }

        assertEquals(InvalidRunActionReason.CURRENT_ROOM_NOT_COMPLETED, exception.reason)
    }

    @Test
    fun `rejects unreachable and previously completed room choices`() {
        val initial = RunState.initial(RunSeed(42))
        val completedStart = RunEngine.execute(initial, RunAction.CompleteCurrentRoom).state

        val unreachableException =
            assertFailsWith<InvalidRunActionException> {
                RunEngine.execute(completedStart, RunAction.ChooseRoom(RoomId("boss")))
            }
        assertEquals(InvalidRunActionReason.ROOM_NOT_REACHABLE, unreachableException.reason)

        val nextRoom =
            completedStart.dungeonGraph.rooms
                .getValue(completedStart.currentRoomId)
                .nextRoomIds
                .first()
        val completedNext =
            RunEngine
                .execute(completedStart, RunAction.ChooseRoom(nextRoom))
                .state
                .let { RunEngine.execute(it, RunAction.CompleteCurrentRoom).state }
        val revisitedException =
            assertFailsWith<InvalidRunActionException> {
                RunEngine.execute(completedNext, RunAction.ChooseRoom(RoomId("start")))
            }
        assertEquals(InvalidRunActionReason.ROOM_ALREADY_COMPLETED, revisitedException.reason)
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
        val state = RunState.initial(RunSeed(42))
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
                RunAction.ChooseRoom(RoomId("event")),
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
