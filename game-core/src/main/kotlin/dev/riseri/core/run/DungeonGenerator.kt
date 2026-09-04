package dev.riseri.core.run

data class DungeonGenerationResult(
    val graph: DungeonGraph,
    val nextRngState: RunRngState,
)

/** Generates the small M2.1 branching route from the run's reproducible random stream. */
object DungeonGenerator {
    private val startRoomId = RoomId("start")
    private val eventRoomId = RoomId("event")
    private val treasureRoomId = RoomId("treasure")
    private val combatRoomId = RoomId("combat")
    private val bossRoomId = RoomId("boss")

    fun generate(seed: RunSeed): DungeonGenerationResult {
        val shapeSelection = RunRngState(seed.value).nextInt(RouteShape.entries.size)
        val shape = RouteShape.entries[shapeSelection.value]
        val rooms =
            listOf(
                RoomNode(startRoomId, RoomType.COMBAT, listOf(eventRoomId, treasureRoomId)),
                RoomNode(eventRoomId, RoomType.EVENT, listOf(combatRoomId)),
                RoomNode(treasureRoomId, RoomType.TREASURE, shape.treasureRoomChoices),
                RoomNode(combatRoomId, RoomType.COMBAT, listOf(bossRoomId)),
                RoomNode(bossRoomId, RoomType.BOSS, emptyList()),
            )

        return DungeonGenerationResult(
            graph = DungeonGraph(startRoomId, rooms.associateBy { it.id }),
            nextRngState = shapeSelection.nextState,
        )
    }

    /** The shortcut shape varies route length while both choices still lead to the boss. */
    private enum class RouteShape(
        val treasureRoomChoices: List<RoomId>,
    ) {
        CONVERGING(listOf(combatRoomId)),
        TREASURE_SHORTCUT(listOf(bossRoomId)),
    }
}
