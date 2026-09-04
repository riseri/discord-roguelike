package dev.riseri.server.combat

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    fun `base Knight deterministically defeats the starter encounter`() {
        postJson("/api/runs", """{"seed":42}""", expectedStatus = 201)
        var combat =
            postJson("/api/combat", expectedStatus = 201)

        assertEquals("knight", combat.path("player").path("entityId").stringValue())
        assertEquals(
            combat.path("player").path("maxHp").asInt(),
            combat.path("player").path("currentHp").asInt(),
        )
        assertEquals(0, combat.path("player").path("block").asInt())
        assertEquals("PLAYER", combat.path("phase").stringValue())
        assertEquals("ACTIVE", combat.path("status").stringValue())
        assertEquals(2, combat.path("enemies").size())
        assertTrue(combat.path("enemies").all { it.path("contentId").stringValue() == "goblin" })

        // Follow authoritative reachable tiles, then use an adjacent attack through the same API
        // sequence as the playable client.
        var actionsTaken = 0
        while (combat.path("status").stringValue() == "ACTIVE" && actionsTaken < 100) {
            val target = combat.path("enemies").firstOrNull { it.path("currentHp").asInt() > 0 }
            assertNotNull(target)

            fun distance(): Int =
                kotlin.math.abs(
                    combat
                        .path("player")
                        .path("position")
                        .path("x")
                        .asInt() - target.path("position").path("x").asInt(),
                ) +
                    kotlin.math.abs(
                        combat
                            .path("player")
                            .path("position")
                            .path("y")
                            .asInt() - target.path("position").path("y").asInt(),
                    )
            if (distance() > 1 && combat.path("reachablePositions").size() > 0) {
                val destination =
                    combat.path("reachablePositions").minBy { position ->
                        kotlin.math.abs(position.path("x").asInt() - target.path("position").path("x").asInt()) +
                            kotlin.math.abs(position.path("y").asInt() - target.path("position").path("y").asInt())
                    }
                combat =
                    postJson(
                        "/api/combat/actions",
                        """{"destination":{"x":${destination.path("x").asInt()},"y":${destination.path("y").asInt()}}}""",
                    ).path("state")
            }
            val adjacent = distance() == 1
            combat =
                postJson(
                    "/api/combat/actions",
                    if (adjacent) {
                        """{"abilityId":"SLASH","targetId":"${target.path("entityId").stringValue()}"}"""
                    } else {
                        """{"abilityId":"GUARD","targetId":"knight"}"""
                    },
                ).path("state")
            actionsTaken++
        }

        assertEquals("WON", combat.path("status").stringValue())
        assertTrue(combat.path("player").path("currentHp").asInt() > 0)

        val run = getJson("/api/runs/current")
        assertEquals("ACTIVE", run.path("status").stringValue())
        assertEquals(combat.path("player").path("currentHp").asInt(), run.path("playerHp").asInt())
        assertEquals(1, run.path("completedRoomIds").size())
        assertEquals("start", run.path("completedRoomIds").get(0).stringValue())
        assertEquals(2, run.path("availableNextRooms").size())
        assertEquals(
            "event",
            run
                .path("availableNextRooms")
                .get(0)
                .path("id")
                .stringValue(),
        )
        assertEquals(
            "treasure",
            run
                .path("availableNextRooms")
                .get(1)
                .path("id")
                .stringValue(),
        )

        mockMvc
            .perform(
                post("/api/runs/current/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"roomId":"boss"}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("ROOM_NOT_REACHABLE"))

        assertEquals("start", getJson("/api/runs/current").path("currentRoom").path("id").stringValue())

        val advanced = postJson("/api/runs/current/rooms", """{"roomId":"event"}""")
        assertEquals("event", advanced.path("currentRoom").path("id").stringValue())
        assertEquals("EVENT", advanced.path("currentRoom").path("type").stringValue())
        assertEquals(0, advanced.path("availableNextRooms").size())
    }

    private fun postJson(
        path: String,
        body: String? = null,
        expectedStatus: Int = 200,
    ): JsonNode {
        val response =
            mockMvc
                .perform(
                    post(path)
                        .apply {
                            if (body != null) {
                                contentType(MediaType.APPLICATION_JSON)
                                content(body)
                            }
                        },
                ).andExpect(status().`is`(expectedStatus))
                .andReturn()
                .response.contentAsString

        return objectMapper.readTree(response)
    }

    private fun getJson(path: String): JsonNode {
        val response =
            mockMvc
                .perform(get(path))
                .andExpect(status().isOk)
                .andReturn()
                .response.contentAsString

        return objectMapper.readTree(response)
    }
}
