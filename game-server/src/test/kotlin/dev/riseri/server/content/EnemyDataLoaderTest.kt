package dev.riseri.server.content

import dev.riseri.core.combat.EnemyContentId
import dev.riseri.core.combat.EnemyIntention
import dev.riseri.core.combat.HitPoints
import dev.riseri.core.combat.IntentionId
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EnemyDataLoaderTest {
    private val loader = EnemyDataLoader()

    @Test
    fun `loads bundled enemy definitions`() {
        val definitions = loader.loadFromClasspath()

        assertEquals(setOf(EnemyContentId("goblin"), EnemyContentId("goblin-brute")), definitions.keys)
        val brute = definitions.getValue(EnemyContentId("goblin-brute"))
        assertEquals(HitPoints(70), brute.maxHp)
        assertEquals(
            listOf(
                EnemyIntention(IntentionId("punch"), damage = 8),
                EnemyIntention(IntentionId("heavy-swing"), damage = 20),
            ),
            brute.intentions,
        )
    }

    @Test
    fun `reports the location of invalid required hp`() {
        val exception =
            assertFailsWith<EnemyDataLoadException> {
                loader.load(
                    ByteArrayInputStream(
                        """{"enemies":[{"id":"goblin","hp":0,"intentions":[{"id":"stab","damage":10}]}]}"""
                            .toByteArray(),
                    ),
                )
            }

        assertTrue(exception.message.orEmpty().contains("enemy[0].hp must be positive"))
    }
}
