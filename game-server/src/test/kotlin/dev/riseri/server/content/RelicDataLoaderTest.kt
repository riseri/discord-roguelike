package dev.riseri.server.content

import dev.riseri.core.relic.RelicContentId
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RelicDataLoaderTest {
    private val loader = RelicDataLoader()

    @Test
    fun `loads bundled relic definitions in stable content order`() {
        val definitions = loader.loadFromClasspath()

        assertEquals(
            listOf(
                "iron-bulwark",
                "spiked-armor",
                "berserkers-ring",
                "executioners-mark",
                "heavy-gauntlets",
                "vitality-charm",
            ).map(::RelicContentId),
            definitions.keys.toList(),
        )
        assertEquals("Iron Bulwark", definitions.getValue(RelicContentId("iron-bulwark")).name)
        assertEquals(
            "Guard grants 5 additional Block.",
            definitions.getValue(RelicContentId("iron-bulwark")).description,
        )
    }

    @Test
    fun `rejects duplicate relic ids`() {
        val exception =
            assertFailsWith<RelicDataLoadException> {
                loader.load(
                    jsonInput(
                        """{"relics":[
                            {"id":"iron-bulwark","name":"Iron Bulwark","description":"First."},
                            {"id":"iron-bulwark","name":"Other Bulwark","description":"Second."}
                        ]}""",
                    ),
                )
            }

        assertTrue(exception.message.orEmpty().contains("duplicate id 'iron-bulwark'"))
    }

    @Test
    fun `rejects relic ids outside lowercase kebab-case`() {
        val exception =
            assertFailsWith<RelicDataLoadException> {
                loader.load(
                    jsonInput(
                        """{"relics":[
                            {"id":"Iron Bulwark","name":"Iron Bulwark","description":"Invalid id."}
                        ]}""",
                    ),
                )
            }

        assertTrue(exception.message.orEmpty().contains("Invalid relic[0]"))
        assertTrue(exception.message.orEmpty().contains("lowercase kebab-case"))
    }

    @Test
    fun `reports missing required content fields`() {
        val exception =
            assertFailsWith<RelicDataLoadException> {
                loader.load(jsonInput("""{"relics":[{"id":"iron-bulwark","name":"Iron Bulwark"}]}"""))
            }

        assertTrue(exception.message.orEmpty().contains("relic[0].description must be a non-blank string"))
    }

    private fun jsonInput(json: String) = ByteArrayInputStream(json.toByteArray())
}
