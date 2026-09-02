package dev.riseri.core.run

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
) {
    init {
        require(playerMaxHp.value > 0) { "Player maximum hit points must be positive" }
        require(playerHp.value <= playerMaxHp.value) {
            "Player hit points must not exceed maximum hit points"
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
