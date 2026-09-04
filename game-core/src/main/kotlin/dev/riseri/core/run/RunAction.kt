package dev.riseri.core.run

/** Closed set of authoritative run transitions. */
sealed interface RunAction {
    data class StartRun(
        val seed: RunSeed,
    ) : RunAction

    data object CompleteCurrentRoom : RunAction

    data class ChooseRoom(
        val roomId: RoomId,
    ) : RunAction

    data object WinRun : RunAction

    data object LoseRun : RunAction
}
