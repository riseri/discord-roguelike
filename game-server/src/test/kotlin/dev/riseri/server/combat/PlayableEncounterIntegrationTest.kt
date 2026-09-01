package dev.riseri.server.combat

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Proves the M1 encounter through the HTTP/application boundary used by the Activity client.
 * MockMvc keeps this CI test fast while the full Spring context still wires production content,
 * server orchestration, and the authoritative game-core engine.
 */
@SpringBootTest
class PlayableEncounterIntegrationTest {
    @Autowired
    private lateinit var applicationContext: WebApplicationContext

    private val objectMapper = ObjectMapper()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build()
    }

    @Test
    fun `plays a deterministic encounter from creation through victory`() {
        var combat =
            postJson("/api/combat", """{"seed":42}""", expectedStatus = 201)

        assertEquals("knight", combat.path("player").path("entityId").stringValue())
        assertEquals(100, combat.path("player").path("currentHp").asInt())
        assertEquals("PLAYER", combat.path("phase").stringValue())
        assertEquals("ACTIVE", combat.path("status").stringValue())
        assertEquals(1, combat.path("enemies").size())
        combat.path("enemies").forEach { enemy ->
            assertEquals("goblin", enemy.path("contentId").stringValue())
            assertEquals(40, enemy.path("currentHp").asInt())
            assertEquals(40, enemy.path("maxHp").asInt())
            assertEquals("stab", enemy.path("intention").path("id").stringValue())
            assertEquals(10, enemy.path("intention").path("damage").asInt())
        }

        val expectedRounds =
            listOf(
                ExpectedRound(playerHp = 90, enemyHp = listOf(25), status = "ACTIVE"),
                ExpectedRound(playerHp = 80, enemyHp = listOf(10), status = "ACTIVE"),
                ExpectedRound(playerHp = 80, enemyHp = listOf(0), status = "WON"),
            )

        expectedRounds.forEach { expected ->
            val target = combat.path("enemies").firstOrNull { it.path("currentHp").asInt() > 0 }
            assertNotNull(target)
            combat =
                postJson(
                    "/api/combat/actions",
                    """{"abilityId":"SLASH","targetId":"${target.path("entityId").stringValue()}"}""",
                )

            assertEquals(expected.playerHp, combat.path("player").path("currentHp").asInt())
            val enemyHp = combat.path("enemies").toList().map { it.path("currentHp").asInt() }
            assertEquals(expected.enemyHp, enemyHp)
            assertEquals("PLAYER", combat.path("phase").stringValue())
            assertEquals(expected.status, combat.path("status").stringValue())

            if (expected.status == "ACTIVE") {
                combat
                    .path("enemies")
                    .filter { it.path("currentHp").asInt() > 0 }
                    .forEach { enemy -> assertNotNull(enemy.get("intention")) }
            }
        }
    }

    private fun postJson(
        path: String,
        body: String,
        expectedStatus: Int = 200,
    ): JsonNode {
        val response =
            mockMvc
                .perform(
                    post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().`is`(expectedStatus))
                .andReturn()
                .response.contentAsString

        return objectMapper.readTree(response)
    }

    private data class ExpectedRound(
        val playerHp: Int,
        val enemyHp: List<Int>,
        val status: String,
    )
}
