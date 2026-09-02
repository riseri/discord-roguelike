package dev.riseri.server.combat

import dev.riseri.server.content.EnemyDataLoader
import dev.riseri.server.run.RunService
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
    private lateinit var runService: RunService
    private lateinit var mockMvc: MockMvc

    @BeforeTest
    fun setUp() {
        runService = RunService()
        runService.start(42)
        val service = CombatService(EnemyDataLoader(), runService)
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
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.player.entityId").value("knight"))
            .andExpect(jsonPath("$.player.currentHp").value(100))
            .andExpect(jsonPath("$.enemies", hasSize<Any>(1)))
            .andExpect(jsonPath("$.enemies[0].contentId").value("goblin"))
            .andExpect(jsonPath("$.enemies[0].intention.id").isString)
            .andExpect(jsonPath("$.abilities", hasSize<Any>(2)))
            .andExpect(jsonPath("$.abilities[0].id").value("SLASH"))
            .andExpect(jsonPath("$.abilities[0].description").value("Deal 15 damage to one enemy."))
            .andExpect(jsonPath("$.abilities[0].target").value("ENEMY"))
            .andExpect(jsonPath("$.abilities[1].id").value("GUARD"))
            .andExpect(jsonPath("$.abilities[1].description").value("Gain 12 Block."))
            .andExpect(jsonPath("$.abilities[1].target").value("SELF"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        mockMvc
            .perform(get("/api/combat"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enemies[0].intention.damage").isNumber)
    }

    @Test
    fun `requires an active run before combat can start`() {
        val service = CombatService(EnemyDataLoader(), RunService())
        val controller =
            MockMvcBuilders
                .standaloneSetup(CombatController(service))
                .setControllerAdvice(CombatExceptionHandler())
                .build()

        controller
            .perform(post("/api/combat"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NO_ACTIVE_RUN"))
    }

    @Test
    fun `valid ability action returns fully resolved authoritative state`() {
        val startResponse =
            mockMvc
                .perform(
                    post("/api/combat")
                        .contentType(MediaType.APPLICATION_JSON),
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
            .andExpect(jsonPath("$.state.enemies[0].currentHp").value(startingHp - 15))
            .andExpect(jsonPath("$.state.player.currentHp").value(expectedPlayerHp))
            .andExpect(jsonPath("$.state.phase").value("PLAYER"))
            .andExpect(jsonPath("$.state.enemies[0].intention.id").isString)
            .andExpect(jsonPath("$.events[0].type").value("ABILITY_USED"))
            .andExpect(jsonPath("$.events[0].abilityId").value("SLASH"))
            .andExpect(jsonPath("$.events[1].type").value("DAMAGE_DEALT"))
            .andExpect(jsonPath("$.events[1].targetId").value(targetId))
            .andExpect(jsonPath("$.events[1].amount").value(15))
            .andExpect(jsonPath("$.events[2].type").value("ENEMY_ACTION_USED"))
            .andExpect(jsonPath("$.events[3].type").value("DAMAGE_DEALT"))
            .andExpect(jsonPath("$.events[4].type").value("ENEMY_INTENTION_GENERATED"))

        assertEquals(expectedPlayerHp, runService.current().playerHp)
    }

    @Test
    fun `invalid action returns client error without changing encounter`() {
        mockMvc.perform(
            post("/api/combat")
                .contentType(MediaType.APPLICATION_JSON),
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
