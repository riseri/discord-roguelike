package dev.riseri.server.combat

import dev.riseri.core.combat.EntityId
import dev.riseri.core.combat.GameEvent
import dev.riseri.core.relic.RelicContentId
import kotlin.test.Test
import kotlin.test.assertEquals

class CombatEventResponseTest {
    @Test
    fun `relic trigger exposes presentation identifiers without calculating its effect`() {
        val response =
            CombatEventResponse.from(
                GameEvent.RelicTriggered(
                    relicId = RelicContentId("spiked-armor"),
                    ownerId = EntityId("knight"),
                ),
            )

        assertEquals("RELIC_TRIGGERED", response.type)
        assertEquals("spiked-armor", response.relicId)
        assertEquals("knight", response.entityId)
    }
}
