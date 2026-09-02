package dev.riseri.core.run

import dev.riseri.core.combat.CombatState
import dev.riseri.core.combat.CombatStatus
import dev.riseri.core.combat.HitPoints

@JvmInline
value class RunSeed(
    val value: Long,
)

@JvmInline
value class RoomId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Room identifier must not be blank" }
    }
}

@JvmInline
value class RelicId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Relic identifier must not be blank" }
    }
}

@JvmInline
value class RunRngState(
    val value: Long,
)

enum class RunStatus {
    ACTIVE,
    WON,
    LOST,
}

data class RunState(
    val seed: RunSeed,
    val status: RunStatus,
    val playerHp: HitPoints,
    val playerMaxHp: HitPoints,
    val currentRoomId: RoomId,
    val completedRoomIds: Set<RoomId>,
    val ownedRelicIds: Set<RelicId>,
    val rngState: RunRngState,
    val activeCombat: CombatState? = null,
) {
    init {
        require(playerMaxHp.value > 0) { "Player maximum hit points must be positive" }
        require(playerHp.value <= playerMaxHp.value) {
            "Player hit points must not exceed maximum hit points"
        }
        activeCombat?.let { combat ->
            require(combat.player.currentHp == playerHp && combat.player.maxHp == playerMaxHp) {
                "Active combat player hit points must match the owning run"
            }
            require(combat.rngState.value == rngState.value) {
                "Active combat RNG state must match the owning run"
            }
            require(
                when (combat.status) {
                    CombatStatus.ACTIVE -> status == RunStatus.ACTIVE
                    CombatStatus.WON -> status != RunStatus.LOST
                    CombatStatus.LOST -> status == RunStatus.LOST
                },
            ) {
                "Active combat terminal status must agree with the owning run"
            }
        }
    }

    companion object {
        private const val INITIAL_PLAYER_HP = 100
        private const val INITIAL_ROOM_ID = "start"

        /** Creates the authoritative starting state without generating a dungeon route. */
        fun initial(
            seed: RunSeed,
            currentRoomId: RoomId = RoomId(INITIAL_ROOM_ID),
        ) = RunState(
            seed = seed,
            status = RunStatus.ACTIVE,
            playerHp = HitPoints(INITIAL_PLAYER_HP),
            playerMaxHp = HitPoints(INITIAL_PLAYER_HP),
            currentRoomId = currentRoomId,
            completedRoomIds = emptySet(),
            ownedRelicIds = emptySet(),
            rngState = RunRngState(seed.value),
        )
    }
}
