package dev.riseri.core.run

enum class RoomType {
    COMBAT,
    EVENT,
    TREASURE,
    BOSS,
}

data class RoomNode(
    val id: RoomId,
    val type: RoomType,
    val nextRoomIds: List<RoomId>,
) {
    init {
        require(nextRoomIds.distinct().size == nextRoomIds.size) {
            "Room choices must not contain duplicate room identifiers"
        }
    }
}

/**
 * Authoritative dungeon topology. Construction validates that every encoded choice stays within
 * a finite branching route from [startRoomId] to the single terminal boss.
 */
data class DungeonGraph(
    val startRoomId: RoomId,
    val rooms: Map<RoomId, RoomNode>,
) {
    init {
        require(rooms.isNotEmpty()) { "Dungeon graph must contain at least one room" }
        require(rooms.all { (id, room) -> id == room.id }) {
            "Dungeon room map keys must match their room identifiers"
        }
        require(startRoomId in rooms) { "Dungeon start room must exist in the graph" }

        val unknownChoice =
            rooms.values
                .asSequence()
                .flatMap { room -> room.nextRoomIds.asSequence() }
                .firstOrNull { it !in rooms }
        require(unknownChoice == null) { "Room choices must reference rooms in the graph" }

        val bossRooms = rooms.values.filter { it.type == RoomType.BOSS }
        require(bossRooms.size == 1) { "Dungeon graph must contain exactly one boss room" }
        val bossRoom = bossRooms.single()
        require(bossRoom.nextRoomIds.isEmpty()) { "The boss room must be terminal" }

        val reachableRoomIds = reachableRoomIdsFromStart()
        require(reachableRoomIds == rooms.keys) {
            "Every dungeon room must be reachable from the start room"
        }

        require(isAcyclic()) { "Dungeon graph must not contain cycles" }
        val bossReachability = mutableMapOf<RoomId, Boolean>()
        require(rooms.keys.all { canReachBoss(it, bossRoom.id, bossReachability) }) {
            "Every dungeon room must have a path to the boss room"
        }
    }

    private fun reachableRoomIdsFromStart(): Set<RoomId> {
        val reachable = mutableSetOf<RoomId>()
        val pending = ArrayDeque<RoomId>()
        pending.add(startRoomId)

        while (pending.isNotEmpty()) {
            val roomId = pending.removeFirst()
            if (reachable.add(roomId)) {
                pending.addAll(rooms.getValue(roomId).nextRoomIds)
            }
        }

        return reachable
    }

    private fun isAcyclic(): Boolean {
        val visiting = mutableSetOf<RoomId>()
        val visited = mutableSetOf<RoomId>()

        fun visit(roomId: RoomId): Boolean {
            if (roomId in visiting) return false
            if (roomId in visited) return true

            visiting += roomId
            if (rooms.getValue(roomId).nextRoomIds.any { !visit(it) }) return false
            visiting -= roomId
            visited += roomId
            return true
        }

        return visit(startRoomId)
    }

    private fun canReachBoss(
        roomId: RoomId,
        bossRoomId: RoomId,
        results: MutableMap<RoomId, Boolean>,
    ): Boolean =
        results.getOrPut(roomId) {
            roomId == bossRoomId || rooms.getValue(roomId).nextRoomIds.any { canReachBoss(it, bossRoomId, results) }
        }
}
