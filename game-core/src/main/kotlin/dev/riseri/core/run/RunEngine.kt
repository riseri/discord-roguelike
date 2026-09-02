package dev.riseri.core.run

enum class InvalidRunActionReason {
    RUN_NOT_STARTED,
    RUN_ALREADY_STARTED,
    RUN_ENDED,
    ROOM_ALREADY_COMPLETED,
}

class InvalidRunActionException(
    val reason: InvalidRunActionReason,
) : IllegalArgumentException(reason.name)

object RunEngine {
    /**
     * Applies every run lifecycle action through one authoritative transition path. A null state
     * represents the pre-run state and is valid only for [RunAction.StartRun].
     */
    fun execute(
        state: RunState?,
        action: RunAction,
    ): RunActionResult {
        // Terminal status takes precedence so every action against an ended run fails consistently.
        if (state != null && state.status != RunStatus.ACTIVE) {
            throw InvalidRunActionException(InvalidRunActionReason.RUN_ENDED)
        }

        return when (action) {
            is RunAction.StartRun -> startRun(state, action)
            RunAction.CompleteCurrentRoom -> completeCurrentRoom(requireActiveRun(state))
            RunAction.WinRun -> endRun(requireActiveRun(state), RunStatus.WON)
            RunAction.LoseRun -> endRun(requireActiveRun(state), RunStatus.LOST)
        }
    }

    private fun startRun(
        state: RunState?,
        action: RunAction.StartRun,
    ): RunActionResult {
        if (state != null) {
            throw InvalidRunActionException(InvalidRunActionReason.RUN_ALREADY_STARTED)
        }

        return RunActionResult(
            state = RunState.initial(action.seed),
            events = listOf(RunEvent.RunStarted(action.seed)),
        )
    }

    private fun completeCurrentRoom(state: RunState): RunActionResult {
        if (state.currentRoomId in state.completedRoomIds) {
            throw InvalidRunActionException(InvalidRunActionReason.ROOM_ALREADY_COMPLETED)
        }

        return RunActionResult(
            state = state.copy(completedRoomIds = state.completedRoomIds + state.currentRoomId),
            events = listOf(RunEvent.RoomCompleted(state.currentRoomId)),
        )
    }

    private fun endRun(
        state: RunState,
        status: RunStatus,
    ) = RunActionResult(
        state = state.copy(status = status),
        events =
            listOf(
                when (status) {
                    RunStatus.WON -> RunEvent.RunWon
                    RunStatus.LOST -> RunEvent.RunLost
                    RunStatus.ACTIVE -> error("Ending a run requires a terminal status")
                },
            ),
    )

    private fun requireActiveRun(state: RunState?): RunState =
        state ?: throw InvalidRunActionException(InvalidRunActionReason.RUN_NOT_STARTED)
}
