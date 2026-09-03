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
            .andExpect(jsonPath("$.enemies", hasSize<Any>(2)))
            .andExpect(jsonPath("$.enemies[0].contentId").value("goblin"))
            .andExpect(jsonPath("$.enemies[0].intention.id").isString)
            .andExpect(jsonPath("$.abilities", hasSize<Any>(3)))
            .andExpect(jsonPath("$.abilities[0].id").value("SLASH"))
            .andExpect(jsonPath("$.abilities[0].description").value("Deal 15 damage to one enemy."))
            .andExpect(jsonPath("$.abilities[0].target").value("ENEMY"))
            .andExpect(jsonPath("$.abilities[1].id").value("GUARD"))
            .andExpect(jsonPath("$.abilities[1].description").value("Gain 12 Block."))
            .andExpect(jsonPath("$.abilities[1].target").value("SELF"))
            .andExpect(jsonPath("$.abilities[2].id").value("SHIELD_BASH"))
            .andExpect(jsonPath("$.grid.width").value(8))
            .andExpect(jsonPath("$.grid.height").value(6))
            .andExpect(jsonPath("$.reachablePositions").isArray)
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
    fun `valid move action returns authoritative position without ending player phase`() {
        val startResponse =
            mockMvc
                .perform(
                    post("/api/combat")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andReturn()
                .response.contentAsString
        val destination = objectMapper.readTree(startResponse).get("reachablePositions").get(0)
        val x = destination.get("x").asInt()
        val y = destination.get("y").asInt()

        mockMvc
            .perform(
                post("/api/combat/actions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"destination":{"x":$x,"y":$y}}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.state.player.position.x").value(x))
            .andExpect(jsonPath("$.state.player.position.y").value(y))
            .andExpect(jsonPath("$.state.player.currentHp").value(100))
            .andExpect(jsonPath("$.state.phase").value("PLAYER"))
            .andExpect(jsonPath("$.events[0].type").value("ENTITY_MOVED"))
            .andExpect(jsonPath("$.events[0].entityId").value("knight"))

        assertEquals(100, runService.current().playerHp)
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
