package dev.riseri.server.combat

import dev.riseri.server.content.EnemyDataLoader
import org.hamcrest.Matchers.hasSize
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.ObjectMapper
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CombatControllerTest {
    private val objectMapper = ObjectMapper()
    private lateinit var mockMvc: MockMvc

    @BeforeTest
    fun setUp() {
        val service = CombatService(EnemyDataLoader())
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(CombatController(service))
                .setControllerAdvice(CombatExceptionHandler())
                .build()
    }

    @Test
    fun `starts and reads a playable encounter with visible intentions`() {
        mockMvc
            .perform(
                post("/api/combat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"seed":42}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.player.entityId").value("knight"))
            .andExpect(jsonPath("$.player.currentHp").value(100))
            .andExpect(jsonPath("$.enemies", hasSize<Any>(2)))
            .andExpect(jsonPath("$.enemies[0].intention.id").isString)
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        mockMvc
            .perform(get("/api/combat"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enemies[0].intention.damage").isNumber)
    }

    @Test
    fun `valid ability action returns fully resolved authoritative state`() {
        val startResponse =
            mockMvc
                .perform(
                    post("/api/combat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"seed":42}"""),
                ).andReturn()
                .response.contentAsString
        val enemies = objectMapper.readTree(startResponse).get("enemies")
        val firstEnemy = enemies.get(0)
        val targetId = firstEnemy.get("entityId").stringValue()
        val startingHp = firstEnemy.get("currentHp").asInt()
        val expectedPlayerHp =
            100 -
                (0 until enemies.size()).sumOf {
                    enemies
                        .get(it)
                        .get("intention")
                        .get("damage")
                        .asInt()
                }

        mockMvc
            .perform(
                post("/api/combat/actions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"abilityId":"SLASH","targetId":"$targetId"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.enemies[0].currentHp").value(startingHp - 15))
            .andExpect(jsonPath("$.player.currentHp").value(expectedPlayerHp))
            .andExpect(jsonPath("$.phase").value("PLAYER"))
            .andExpect(jsonPath("$.enemies[0].intention.id").isString)
    }

    @Test
    fun `invalid action returns client error without changing encounter`() {
        mockMvc.perform(
            post("/api/combat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"seed":42}"""),
        )
        val before =
            mockMvc
                .perform(get("/api/combat"))
                .andReturn()
                .response.contentAsString

        mockMvc
            .perform(
                post("/api/combat/actions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"abilityId":"SLASH","targetId":"missing"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("TARGET_NOT_FOUND"))

        val after =
            mockMvc
                .perform(get("/api/combat"))
                .andReturn()
                .response.contentAsString
        assertEquals(before, after)
    }
}
