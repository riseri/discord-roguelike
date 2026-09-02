package dev.riseri.server.run

import dev.riseri.core.run.InvalidRunActionException
import dev.riseri.core.run.RunState
import dev.riseri.core.run.RunStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

data class StartRunRequest(
    val seed: Long? = null,
)

/** Client-facing projection of authoritative run state. */
data class RunResponse(
    val seed: Long,
    val status: RunStatus,
    val playerHp: Int,
    val playerMaxHp: Int,
    val currentRoomId: String,
    val completedRoomIds: List<String>,
    val ownedRelicIds: List<String>,
    val rngState: Long,
) {
    companion object {
        fun from(state: RunState) =
            RunResponse(
                seed = state.seed.value,
                status = state.status,
                playerHp = state.playerHp.value,
                playerMaxHp = state.playerMaxHp.value,
                currentRoomId = state.currentRoomId.value,
                completedRoomIds = state.completedRoomIds.map { it.value }.sorted(),
                ownedRelicIds = state.ownedRelicIds.map { it.value }.sorted(),
                rngState = state.rngState.value,
            )
    }
}

data class RunApiErrorResponse(
    val code: String,
    val message: String,
)

@RestController
@RequestMapping("/api/runs")
class RunController(
    private val runService: RunService,
) {
    @PostMapping
    fun start(
        @RequestBody(required = false) request: StartRunRequest?,
    ): ResponseEntity<RunResponse> = ResponseEntity.status(HttpStatus.CREATED).body(runService.start(request?.seed))

    @GetMapping("/current")
    fun current(): RunResponse = runService.current()
}

@RestControllerAdvice
class RunExceptionHandler {
    @ExceptionHandler(NoActiveRunException::class)
    fun noActiveRun(exception: NoActiveRunException): ResponseEntity<RunApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(RunApiErrorResponse("NO_ACTIVE_RUN", exception.message.orEmpty()))

    @ExceptionHandler(InvalidRunActionException::class)
    fun invalidAction(exception: InvalidRunActionException): ResponseEntity<RunApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(RunApiErrorResponse(exception.reason.name, exception.message.orEmpty()))
}
