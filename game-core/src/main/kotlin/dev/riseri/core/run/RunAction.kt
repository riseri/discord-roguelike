package dev.riseri.core.run

import dev.riseri.core.relic.RelicContentId

/** Closed set of authoritative run transitions. */
sealed interface RunAction {
    data class StartRun(
        val seed: RunSeed,
    ) : RunAction

    data object CompleteCurrentRoom : RunAction

    data class ChooseRoom(
        val roomId: RoomId,
    ) : RunAction

    /** System intent to create the next deterministic set of relic choices. */
    data object OfferReward : RunAction

    data class SelectReward(
        val relicId: RelicContentId,
    ) : RunAction

    data object WinRun : RunAction

    data object LoseRun : RunAction
}
