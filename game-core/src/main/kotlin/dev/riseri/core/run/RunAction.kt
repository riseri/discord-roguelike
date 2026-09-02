package dev.riseri.core.run

/** Closed set of lifecycle transitions supported by the M2.0 run model. */
sealed interface RunAction {
    data class StartRun(
        val seed: RunSeed,
    ) : RunAction

    data object CompleteCurrentRoom : RunAction

    data object WinRun : RunAction

    data object LoseRun : RunAction
}
