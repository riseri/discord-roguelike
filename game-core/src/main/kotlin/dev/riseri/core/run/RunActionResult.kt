package dev.riseri.core.run

data class RunActionResult(
    val state: RunState,
    // Events remain in transition order so callers never need to infer lifecycle changes.
    val events: List<RunEvent>,
)

sealed interface RunEvent {
    data class RunStarted(
        val seed: RunSeed,
    ) : RunEvent

    data class RoomCompleted(
        val roomId: RoomId,
    ) : RunEvent

    data object RunWon : RunEvent

    data object RunLost : RunEvent
}
