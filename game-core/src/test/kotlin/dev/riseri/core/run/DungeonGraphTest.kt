package dev.riseri.core.run

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DungeonGraphTest {
    @Test
    fun `represents every room type and authoritative branching choices`() {
        val graph = validGraph()

        assertEquals(RoomId("combat-start"), graph.startRoomId)
        assertEquals(
            listOf(RoomId("event"), RoomId("treasure")),
            graph.rooms.getValue(graph.startRoomId).nextRoomIds,
        )
        assertEquals(RoomType.COMBAT, graph.rooms.getValue(RoomId("combat-start")).type)
        assertEquals(RoomType.EVENT, graph.rooms.getValue(RoomId("event")).type)
        assertEquals(RoomType.TREASURE, graph.rooms.getValue(RoomId("treasure")).type)
        assertEquals(RoomType.BOSS, graph.rooms.getValue(RoomId("boss")).type)
    }

    @Test
    fun `represents the boss as the single terminal room`() {
        val graph = validGraph()

        val terminalRooms = graph.rooms.values.filter { it.nextRoomIds.isEmpty() }

        assertEquals(listOf(RoomId("boss")), terminalRooms.map { it.id })
        assertEquals(RoomType.BOSS, terminalRooms.single().type)
    }

    @Test
    fun `rejects duplicate choices from one room`() {
        assertFailsWith<IllegalArgumentException> {
            room("start", RoomType.COMBAT, "boss", "boss")
        }
    }

    @Test
    fun `rejects an absent start room or inconsistent room map key`() {
        assertFailsWith<IllegalArgumentException> {
            DungeonGraph(
                startRoomId = RoomId("missing"),
                rooms = validRooms(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DungeonGraph(
                startRoomId = RoomId("combat-start"),
                rooms = validRooms() + (RoomId("wrong-key") to room("extra", RoomType.EVENT, "boss")),
            )
        }
    }

    @Test
    fun `rejects choices outside the graph`() {
        assertFailsWith<IllegalArgumentException> {
            DungeonGraph(
                startRoomId = RoomId("start"),
                rooms =
                    roomsById(
                        room("start", RoomType.COMBAT, "missing"),
                        room("boss", RoomType.BOSS),
                    ),
            )
        }
    }

    @Test
    fun `rejects missing multiple or nonterminal bosses`() {
        assertFailsWith<IllegalArgumentException> {
            DungeonGraph(
                startRoomId = RoomId("start"),
                rooms = roomsById(room("start", RoomType.COMBAT)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DungeonGraph(
                startRoomId = RoomId("boss-one"),
                rooms =
                    roomsById(
                        room("boss-one", RoomType.BOSS),
                        room("boss-two", RoomType.BOSS),
                    ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DungeonGraph(
                startRoomId = RoomId("boss"),
                rooms =
                    roomsById(
                        room("boss", RoomType.BOSS, "event"),
                        room("event", RoomType.EVENT),
                    ),
            )
        }
    }

    @Test
    fun `rejects unreachable rooms`() {
        assertFailsWith<IllegalArgumentException> {
            DungeonGraph(
                startRoomId = RoomId("start"),
                rooms =
                    roomsById(
                        room("start", RoomType.COMBAT, "boss"),
                        room("unreachable", RoomType.EVENT, "boss"),
                        room("boss", RoomType.BOSS),
                    ),
            )
        }
    }

    @Test
    fun `rejects cycles and paths that do not reach the boss`() {
        assertFailsWith<IllegalArgumentException> {
            DungeonGraph(
                startRoomId = RoomId("start"),
                rooms =
                    roomsById(
                        room("start", RoomType.COMBAT, "event", "boss"),
                        room("event", RoomType.EVENT, "start"),
                        room("boss", RoomType.BOSS),
                    ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DungeonGraph(
                startRoomId = RoomId("start"),
                rooms =
                    roomsById(
                        room("start", RoomType.COMBAT, "treasure", "boss"),
                        room("treasure", RoomType.TREASURE),
                        room("boss", RoomType.BOSS),
                    ),
            )
        }
    }

    private fun validGraph() =
        DungeonGraph(
            startRoomId = RoomId("combat-start"),
            rooms = validRooms(),
        )

    private fun validRooms() =
        roomsById(
            room("combat-start", RoomType.COMBAT, "event", "treasure"),
            room("event", RoomType.EVENT, "boss"),
            room("treasure", RoomType.TREASURE, "boss"),
            room("boss", RoomType.BOSS),
        )

    private fun roomsById(vararg rooms: RoomNode) = rooms.associateBy { it.id }

    private fun room(
        id: String,
        type: RoomType,
        vararg nextRoomIds: String,
    ) = RoomNode(
        id = RoomId(id),
        type = type,
        nextRoomIds = nextRoomIds.map(::RoomId),
    )
}
