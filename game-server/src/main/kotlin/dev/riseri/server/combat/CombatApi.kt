package dev.riseri.server.combat

import dev.riseri.core.combat.AbilityId
import dev.riseri.core.combat.CombatPhase
import dev.riseri.core.combat.CombatState
import dev.riseri.core.combat.CombatStatus
import dev.riseri.core.combat.EnemyCombatState
import dev.riseri.core.combat.EnemyIntention
import dev.riseri.core.combat.GameEvent
import dev.riseri.core.combat.KnightAbilityValues
import dev.riseri.core.combat.PlayerCombatState
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

data class StartCombatRequest(
    val seed: Long,
)

data class UseAbilityRequest(
    val abilityId: AbilityId,
    val targetId: String,
)

data class CombatActionResponse(
    val state: CombatResponse,
    val events: List<CombatEventResponse>,
) {
    companion object {
        fun from(
            state: CombatState,
            events: List<GameEvent>,
        ) = CombatActionResponse(
            state = CombatResponse.from(state),
            events = events.map(CombatEventResponse::from),
        )
    }
}

/** Explicit transport shape for core events; nullable fields are populated only for their type. */
data class CombatEventResponse(
    val type: String,
    val actorId: String? = null,
    val abilityId: AbilityId? = null,
    val sourceId: String? = null,
    val targetId: String? = null,
    val entityId: String? = null,
    val enemyId: String? = null,
    val intention: IntentionResponse? = null,
    val intentionId: String? = null,
    val amount: Int? = null,
) {
    companion object {
        fun from(event: GameEvent): CombatEventResponse =
            when (event) {
                is GameEvent.AbilityUsed -> {
                    CombatEventResponse(
                        type = "ABILITY_USED",
                        actorId = event.actorId.value,
                        abilityId = event.abilityId,
                        targetId = event.targetId.value,
                    )
                }

                is GameEvent.DamageDealt -> {
                    CombatEventResponse(
                        type = "DAMAGE_DEALT",
                        sourceId = event.sourceId.value,
                        targetId = event.targetId.value,
                        amount = event.amount,
                    )
                }

                is GameEvent.BlockGained -> {
                    CombatEventResponse(
                        type = "BLOCK_GAINED",
                        entityId = event.entityId.value,
                        amount = event.amount,
                    )
                }

                is GameEvent.EnemyIntentionGenerated -> {
                    CombatEventResponse(
                        type = "ENEMY_INTENTION_GENERATED",
                        enemyId = event.enemyId.value,
                        intention = IntentionResponse.from(event.intention),
                    )
                }

                is GameEvent.EnemyActionUsed -> {
                    CombatEventResponse(
                        type = "ENEMY_ACTION_USED",
                        enemyId = event.enemyId.value,
                        intentionId = event.intentionId.value,
                        targetId = event.targetId.value,
                    )
                }

                is GameEvent.BlockAbsorbed -> {
                    CombatEventResponse(
                        type = "BLOCK_ABSORBED",
                        entityId = event.entityId.value,
                        amount = event.amount,
                    )
                }

                is GameEvent.EntityDefeated -> {
                    CombatEventResponse(type = "ENTITY_DEFEATED", entityId = event.entityId.value)
                }

                GameEvent.CombatWon -> {
                    CombatEventResponse(type = "COMBAT_WON")
                }

                GameEvent.CombatLost -> {
                    CombatEventResponse(type = "COMBAT_LOST")
                }
            }
    }
}

data class CombatResponse(
    val player: PlayerResponse,
    val enemies: List<EnemyResponse>,
    val abilities: List<AbilityResponse>,
    val phase: CombatPhase,
    val status: CombatStatus,
) {
    companion object {
        fun from(state: CombatState) =
            CombatResponse(
                player = PlayerResponse.from(state.player),
                enemies = state.enemies.map(EnemyResponse::from),
                abilities = AbilityResponse.KNIGHT_ABILITIES,
                phase = state.phase,
                status = state.status,
            )
    }
}

enum class AbilityTargetResponse {
    ENEMY,
    SELF,
}

data class AbilityResponse(
    val id: AbilityId,
    val name: String,
    val description: String,
    val target: AbilityTargetResponse,
) {
    companion object {
        val KNIGHT_ABILITIES =
            listOf(
                AbilityResponse(
                    id = AbilityId.SLASH,
                    name = "Slash",
                    description = "Deal ${KnightAbilityValues.SLASH_DAMAGE} damage to one enemy.",
                    target = AbilityTargetResponse.ENEMY,
                ),
                AbilityResponse(
                    id = AbilityId.GUARD,
                    name = "Guard",
                    description = "Gain ${KnightAbilityValues.GUARD_BLOCK} Block.",
                    target = AbilityTargetResponse.SELF,
                ),
            )
    }
}

data class PlayerResponse(
    val entityId: String,
    val currentHp: Int,
    val maxHp: Int,
    val block: Int,
) {
    companion object {
        fun from(player: PlayerCombatState) =
            PlayerResponse(
                entityId = player.entityId.value,
                currentHp = player.currentHp.value,
                maxHp = player.maxHp.value,
                block = player.block.value,
            )
    }
}

data class EnemyResponse(
    val entityId: String,
    val contentId: String,
    val currentHp: Int,
    val maxHp: Int,
    val intention: IntentionResponse?,
) {
    companion object {
        fun from(enemy: EnemyCombatState) =
            EnemyResponse(
                entityId = enemy.entityId.value,
                contentId = enemy.enemyContentId.value,
                currentHp = enemy.currentHp.value,
                maxHp = enemy.maxHp.value,
                intention = enemy.currentIntention?.let(IntentionResponse::from),
            )
    }
}

data class IntentionResponse(
    val id: String,
    val damage: Int,
) {
    companion object {
        fun from(intention: EnemyIntention) = IntentionResponse(intention.id.value, intention.damage)
    }
}

data class ApiErrorResponse(
    val code: String,
    val message: String,
)

@RestController
@RequestMapping("/api/combat")
class CombatController(
    private val combatService: CombatService,
) {
    @PostMapping
    fun start(
        @RequestBody request: StartCombatRequest,
    ): ResponseEntity<CombatResponse> = ResponseEntity.status(HttpStatus.CREATED).body(combatService.start(request.seed))

    @GetMapping
    fun get(): CombatResponse = combatService.get()

    @PostMapping("/actions")
    fun useAbility(
        @RequestBody request: UseAbilityRequest,
    ): CombatActionResponse = combatService.useAbility(request)
}

@RestControllerAdvice
class CombatExceptionHandler {
    @ExceptionHandler(InvalidCombatActionException::class)
    fun invalidAction(exception: InvalidCombatActionException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.badRequest().body(ApiErrorResponse(exception.code, exception.message.orEmpty()))

    @ExceptionHandler(NoActiveCombatException::class)
    fun noActiveCombat(exception: NoActiveCombatException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiErrorResponse("NO_ACTIVE_COMBAT", exception.message.orEmpty()))
}
