package dev.riseri.core.combat

data class GridPosition(
    val x: Int,
    val y: Int,
)

data class TacticalGrid(
    val width: Int = 8,
    val height: Int = 6,
) {
    init {
        require(width > 0 && height > 0) { "Grid dimensions must be positive" }
    }

    fun contains(position: GridPosition) = position.x in 0 until width && position.y in 0 until height
}

object TacticalMovement {
    private val neighborOffsets = listOf(0 to -1, -1 to 0, 1 to 0, 0 to 1)

    fun distance(
        from: GridPosition,
        to: GridPosition,
    ) = kotlin.math.abs(from.x - to.x) + kotlin.math.abs(from.y - to.y)

    /** Breadth-first traversal uses a fixed neighbor order so paths replay identically. */
    fun reachable(
        grid: TacticalGrid,
        start: GridPosition,
        movement: Int,
        occupied: Set<GridPosition>,
    ): Set<GridPosition> {
        val distances = linkedMapOf(start to 0)
        val queue = ArrayDeque<GridPosition>().apply { add(start) }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val nextDistance = distances.getValue(current) + 1
            if (nextDistance > movement) continue
            for (next in neighbors(current)) {
                if (grid.contains(next) && next !in occupied && next !in distances) {
                    distances[next] = nextDistance
                    queue.add(next)
                }
            }
        }
        return distances.keys - start
    }

    fun path(
        grid: TacticalGrid,
        start: GridPosition,
        goals: Set<GridPosition>,
        occupied: Set<GridPosition>,
    ): List<GridPosition>? {
        if (start in goals) return emptyList()
        val previous = mutableMapOf<GridPosition, GridPosition?>().apply { put(start, null) }
        val queue = ArrayDeque<GridPosition>().apply { add(start) }
        var found: GridPosition? = null
        while (queue.isNotEmpty() && found == null) {
            val current = queue.removeFirst()
            for (next in neighbors(current)) {
                if (!grid.contains(next) || next in occupied || next in previous) continue
                previous[next] = current
                if (next in goals) {
                    found = next
                    break
                }
                queue.add(next)
            }
        }
        val destination = found ?: return null
        val reversed = mutableListOf<GridPosition>()
        var cursor: GridPosition? = destination
        while (cursor != null && cursor != start) {
            reversed += cursor
            cursor = previous[cursor]
        }
        return reversed.asReversed()
    }

    fun adjacent(position: GridPosition) = neighbors(position).toSet()

    private fun neighbors(position: GridPosition) = neighborOffsets.map { (dx, dy) -> GridPosition(position.x + dx, position.y + dy) }
}
