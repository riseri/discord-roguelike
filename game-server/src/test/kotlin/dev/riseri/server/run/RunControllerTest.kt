package dev.riseri.server.run

import dev.riseri.core.run.DungeonGenerator
import dev.riseri.core.run.RunSeed
import org.hamcrest.Matchers.empty
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RunControllerTest {
    private lateinit var runService: RunService
    private lateinit var mockMvc: MockMvc

    @BeforeTest
    fun setUp() {
        runService = RunService()
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(RunController(runService))
                .setControllerAdvice(RunExceptionHandler())
                .build()
    }

    @Test
    fun `starts and reads the current authoritative run`() {
        val expectedRngState = DungeonGenerator.generate(RunSeed(42)).nextRngState.value

        mockMvc
            .perform(
                post("/api/runs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"seed":42}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seed").value(42))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.playerHp").value(100))
            .andExpect(jsonPath("$.playerMaxHp").value(100))
            .andExpect(jsonPath("$.currentRoomId").value("start"))
            .andExpect(jsonPath("$.completedRoomIds", empty<Any>()))
            .andExpect(jsonPath("$.ownedRelicIds", empty<Any>()))
            .andExpect(jsonPath("$.rngState").value(expectedRngState))

        mockMvc
            .perform(get("/api/runs/current"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seed").value(42))
            .andExpect(jsonPath("$.rngState").value(expectedRngState))
    }

    @Test
    fun `returns not found when there is no active run`() {
        mockMvc
            .perform(get("/api/runs/current"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NO_ACTIVE_RUN"))
            .andExpect(jsonPath("$.message").value("No active run"))
    }

    @Test
    fun `same explicit seed produces the same initial authoritative state`() {
        val first = RunService().start(7_321)
        val second = RunService().start(7_321)

        assertEquals(first, second)
    }

    @Test
    fun `generates and exposes a seed when the request omits one`() {
        mockMvc
            .perform(post("/api/runs"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.seed").isNumber)
            .andExpect(jsonPath("$.rngState").isNumber)
    }

    @Test
    fun `rejects another start without replacing the active run`() {
        runService.start(42)

        mockMvc
            .perform(
                post("/api/runs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"seed":99}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("RUN_ALREADY_STARTED"))

        assertEquals(42, runService.current().seed)
    }
}
