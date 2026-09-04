package dev.riseri.core.run

import dev.riseri.core.relic.RelicContentId
import dev.riseri.core.relic.RelicDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RunRewardTest {
    @Test
    fun `same state and content produce the same three reward choices`() {
        val state = RunState.initial(RunSeed(7_321))

        val first = RunEngine.execute(state, RunAction.OfferReward, relicDefinitions)
        val second = RunEngine.execute(state, RunAction.OfferReward, relicDefinitions.reversed())

        assertEquals(first, second)
        assertEquals(3, first.state.pendingRewardRelicIds.size)
        assertEquals(
            3,
            first.state.pendingRewardRelicIds
                .distinct()
                .size,
        )
        assertEquals(
            listOf(RunEvent.RewardOffered(first.state.pendingRewardRelicIds)),
            first.events,
        )
    }

    @Test
    fun `owned relics are not offered again`() {
        val ownedRelicId = relicDefinitions.first().id
        val state = RunState.initial(RunSeed(42)).copy(ownedRelicIds = setOf(ownedRelicId))

        val result = RunEngine.execute(state, RunAction.OfferReward, relicDefinitions)

        assertEquals(3, result.state.pendingRewardRelicIds.size)
        assertEquals(false, ownedRelicId in result.state.pendingRewardRelicIds)
    }

    @Test
    fun `selecting an offered relic adds it to the run and clears the pending reward`() {
        val offered =
            RunEngine
                .execute(RunState.initial(RunSeed(42)), RunAction.OfferReward, relicDefinitions)
                .state
        val selectedRelicId = offered.pendingRewardRelicIds[1]

        val result = RunEngine.execute(offered, RunAction.SelectReward(selectedRelicId))

        assertEquals(setOf(selectedRelicId), result.state.ownedRelicIds)
        assertEquals(emptyList(), result.state.pendingRewardRelicIds)
        assertEquals(listOf(RunEvent.RewardSelected(selectedRelicId)), result.events)
    }

    @Test
    fun `unoffered and repeated reward selections are rejected`() {
        val offered =
            RunEngine
                .execute(RunState.initial(RunSeed(42)), RunAction.OfferReward, relicDefinitions)
                .state
        val unofferedRelicId = relicDefinitions.map(RelicDefinition::id).first { it !in offered.pendingRewardRelicIds }

        val unofferedException =
            assertFailsWith<InvalidRunActionException> {
                RunEngine.execute(offered, RunAction.SelectReward(unofferedRelicId))
            }
        assertEquals(InvalidRunActionReason.REWARD_NOT_OFFERED, unofferedException.reason)

        val selectedRelicId = offered.pendingRewardRelicIds.first()
        val claimed = RunEngine.execute(offered, RunAction.SelectReward(selectedRelicId)).state
        val repeatedException =
            assertFailsWith<InvalidRunActionException> {
                RunEngine.execute(claimed, RunAction.SelectReward(selectedRelicId))
            }
        assertEquals(InvalidRunActionReason.NO_PENDING_REWARD, repeatedException.reason)
    }

    @Test
    fun `a pending reward cannot be replaced by another offer`() {
        val offered =
            RunEngine
                .execute(RunState.initial(RunSeed(42)), RunAction.OfferReward, relicDefinitions)
                .state

        val exception =
            assertFailsWith<InvalidRunActionException> {
                RunEngine.execute(offered, RunAction.OfferReward, relicDefinitions)
            }

        assertEquals(InvalidRunActionReason.REWARD_ALREADY_PENDING, exception.reason)
    }

    @Test
    fun `an offer requires three eligible unique relics`() {
        val state =
            RunState.initial(RunSeed(42)).copy(
                ownedRelicIds = relicDefinitions.take(3).map(RelicDefinition::id).toSet(),
            )

        val exception =
            assertFailsWith<InvalidRunActionException> {
                RunEngine.execute(state, RunAction.OfferReward, relicDefinitions)
            }

        assertEquals(InvalidRunActionReason.INSUFFICIENT_RELIC_REWARDS_AVAILABLE, exception.reason)
    }

    private val relicDefinitions =
        listOf(
            relic("iron-bulwark"),
            relic("spiked-armor"),
            relic("berserkers-ring"),
            relic("executioners-mark"),
            relic("heavy-gauntlets"),
        )

    private fun relic(id: String) =
        RelicDefinition(
            id = RelicContentId(id),
            name = id,
            description = "$id description",
        )
}
