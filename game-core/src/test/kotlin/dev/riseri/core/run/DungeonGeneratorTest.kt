package dev.riseri.core.run

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DungeonGeneratorTest {
    @Test
    fun `same seed generates the same dungeon and next random state`() {
        val first = DungeonGenerator.generate(RunSeed(7_321))
        val second = DungeonGenerator.generate(RunSeed(7_321))

        assertEquals(first, second)
        assertNotEquals(RunRngState(7_321), first.nextRngState)
    }

    @Test
    fun `different fixed seeds generate different valid routes`() {
        val converging = DungeonGenerator.generate(RunSeed(1)).graph
        val shortcut = DungeonGenerator.generate(RunSeed(0)).graph

        assertNotEquals(converging, shortcut)
        assertEquals(listOf(RoomId("combat")), converging.rooms.getValue(RoomId("treasure")).nextRoomIds)
        assertEquals(listOf(RoomId("boss")), shortcut.rooms.getValue(RoomId("treasure")).nextRoomIds)
    }

    @Test
    fun `generated routes contain five rooms and a meaningful branch`() {
        listOf(0L, 1L, 42L, Long.MIN_VALUE, Long.MAX_VALUE).forEach { seed ->
            val graph = DungeonGenerator.generate(RunSeed(seed)).graph
            val start = graph.rooms.getValue(graph.startRoomId)

            assertEquals(5, graph.rooms.size)
            assertEquals(2, start.nextRoomIds.size)
            assertNotEquals(
                graph.rooms.getValue(start.nextRoomIds[0]).type,
                graph.rooms.getValue(start.nextRoomIds[1]).type,
            )
        }
    }

    @Test
    fun `every generated room is reachable and can reach the terminal boss`() {
        (-100L..100L).forEach { seed ->
            val graph = DungeonGenerator.generate(RunSeed(seed)).graph
            val boss = graph.rooms.values.single { it.type == RoomType.BOSS }

            assertEquals(emptyList(), boss.nextRoomIds)
            assertEquals(graph.rooms.keys, reachableFrom(graph.startRoomId, graph))
            graph.rooms.keys.forEach { roomId ->
                assertTrue(boss.id in reachableFrom(roomId, graph), "$roomId cannot reach the boss for seed $seed")
            }
        }
    }

    private fun reachableFrom(
        startRoomId: RoomId,
        graph: DungeonGraph,
    ): Set<RoomId> {
        val reachable = mutableSetOf<RoomId>()
        val pending = ArrayDeque<RoomId>()
        pending += startRoomId

        while (pending.isNotEmpty()) {
            val roomId = pending.removeFirst()
            if (reachable.add(roomId)) {
                pending.addAll(graph.rooms.getValue(roomId).nextRoomIds)
            }
        }

        return reachable
    }
}
