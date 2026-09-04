package dev.riseri.core.run

import dev.riseri.core.combat.CombatState
import dev.riseri.core.combat.CombatStatus
import dev.riseri.core.combat.HitPoints
import dev.riseri.core.random.nextDeterministicInt
import dev.riseri.core.relic.RelicContentId

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
value class RunRngState(
    val value: Long,
) {
    /** Advances run-owned randomness without consulting global or platform RNG state. */
    fun nextInt(bound: Int): RunRandomIntResult {
        val random = nextDeterministicInt(value, bound)
        return RunRandomIntResult(random.value, RunRngState(random.nextState))
    }
}

data class RunRandomIntResult(
    val value: Int,
    val nextState: RunRngState,
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
    val dungeonGraph: DungeonGraph,
    val currentRoomId: RoomId,
    val completedRoomIds: Set<RoomId>,
    val ownedRelicIds: Set<RelicContentId>,
    val pendingRewardRelicIds: List<RelicContentId>,
    val rngState: RunRngState,
    val activeCombat: CombatState? = null,
) {
    init {
        require(playerMaxHp.value > 0) { "Player maximum hit points must be positive" }
        require(playerHp.value <= playerMaxHp.value) {
            "Player hit points must not exceed maximum hit points"
        }
        require(currentRoomId in dungeonGraph.rooms) {
            "Current room must exist in the dungeon graph"
        }
        require(completedRoomIds.all { it in dungeonGraph.rooms }) {
            "Completed rooms must exist in the dungeon graph"
        }
        require(pendingRewardRelicIds.distinct().size == pendingRewardRelicIds.size) {
            "Pending reward choices must not contain duplicate relics"
        }
        require(pendingRewardRelicIds.none(ownedRelicIds::contains)) {
            "Pending reward choices must not contain owned relics"
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

        /** Creates the authoritative starting state and consumes RNG only through route generation. */
        fun initial(seed: RunSeed): RunState {
            val generatedDungeon = DungeonGenerator.generate(seed)
            return RunState(
                seed = seed,
                status = RunStatus.ACTIVE,
                playerHp = HitPoints(INITIAL_PLAYER_HP),
                playerMaxHp = HitPoints(INITIAL_PLAYER_HP),
                dungeonGraph = generatedDungeon.graph,
                currentRoomId = generatedDungeon.graph.startRoomId,
                completedRoomIds = emptySet(),
                ownedRelicIds = emptySet(),
                pendingRewardRelicIds = emptyList(),
                rngState = generatedDungeon.nextRngState,
            )
        }
    }
}
