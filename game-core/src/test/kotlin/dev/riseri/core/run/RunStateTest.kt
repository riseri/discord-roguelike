package dev.riseri.core.run

import dev.riseri.core.combat.Block
import dev.riseri.core.combat.CombatRngState
import dev.riseri.core.combat.CombatState
import dev.riseri.core.combat.CombatStatus
import dev.riseri.core.combat.EntityId
import dev.riseri.core.combat.HitPoints
import dev.riseri.core.combat.PlayerCombatState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RunStateTest {
    @Test
    fun `same seed produces identical initial state`() {
        val seed = RunSeed(7_321)
        val generatedDungeon = DungeonGenerator.generate(seed)
        val first = RunState.initial(seed)
        val second = RunState.initial(seed)

        assertEquals(first, second)
        assertEquals(generatedDungeon.graph, first.dungeonGraph)
        assertEquals(generatedDungeon.nextRngState, first.rngState)
    }

    @Test
    fun `initial state contains the Knight defaults and no run progress`() {
        val state = RunState.initial(RunSeed(42))

        assertEquals(RunSeed(42), state.seed)
        assertEquals(RunStatus.ACTIVE, state.status)
        assertEquals(HitPoints(100), state.playerHp)
        assertEquals(HitPoints(100), state.playerMaxHp)
        assertEquals(DungeonGenerator.generate(RunSeed(42)).graph, state.dungeonGraph)
        assertEquals(RoomId("start"), state.currentRoomId)
        assertTrue(state.completedRoomIds.isEmpty())
        assertTrue(state.ownedRelicIds.isEmpty())
    }

    @Test
    fun `represents every run status`() {
        val active = runState(status = RunStatus.ACTIVE)
        val won = runState(status = RunStatus.WON)
        val lost = runState(status = RunStatus.LOST, playerHp = HitPoints(0))

        assertEquals(RunStatus.ACTIVE, active.status)
        assertEquals(RunStatus.WON, won.status)
        assertEquals(RunStatus.LOST, lost.status)
    }

    @Test
    fun `tracks completed rooms and owned relics`() {
        val state =
            runState(
                completedRoomIds = setOf(RoomId("start"), RoomId("event")),
                ownedRelicIds = setOf(RelicId("iron-bulwark")),
            )

        assertEquals(setOf(RoomId("start"), RoomId("event")), state.completedRoomIds)
        assertEquals(setOf(RelicId("iron-bulwark")), state.ownedRelicIds)
    }

    @Test
    fun `rejects zero maximum hit points`() {
        assertFailsWith<IllegalArgumentException> {
            runState(playerHp = HitPoints(0), playerMaxHp = HitPoints(0))
        }
    }

    @Test
    fun `rejects hit points above maximum hit points`() {
        assertFailsWith<IllegalArgumentException> {
            runState(playerHp = HitPoints(101))
        }
    }

    @Test
    fun `rejects blank room and relic identifiers`() {
        assertFailsWith<IllegalArgumentException> { RoomId(" ") }
        assertFailsWith<IllegalArgumentException> { RelicId("") }
    }

    @Test
    fun `rejects run progress outside its dungeon graph`() {
        val state = RunState.initial(RunSeed(42))

        assertFailsWith<IllegalArgumentException> {
            state.copy(currentRoomId = RoomId("missing"))
        }
        assertFailsWith<IllegalArgumentException> {
            state.copy(completedRoomIds = setOf(RoomId("missing")))
        }
    }

    @Test
    fun `rejects active combat state that diverges from its run`() {
        val combat =
            CombatState(
                player =
                    PlayerCombatState(
                        entityId = EntityId("knight"),
                        currentHp = HitPoints(90),
                        maxHp = HitPoints(100),
                        block = Block(0),
                    ),
                enemies = emptyList(),
                rngState = CombatRngState(42),
            )

        assertFailsWith<IllegalArgumentException> {
            RunState.initial(RunSeed(42)).copy(activeCombat = combat)
        }

        val lostCombat =
            combat.copy(
                player = combat.player.copy(currentHp = HitPoints(0)),
                status = CombatStatus.LOST,
            )
        assertFailsWith<IllegalArgumentException> {
            RunState.initial(RunSeed(42)).copy(playerHp = HitPoints(0), activeCombat = lostCombat)
        }
    }

    private fun runState(
        status: RunStatus = RunStatus.ACTIVE,
        playerHp: HitPoints = HitPoints(100),
        playerMaxHp: HitPoints = HitPoints(100),
        completedRoomIds: Set<RoomId> = emptySet(),
        ownedRelicIds: Set<RelicId> = emptySet(),
    ): RunState {
        val graph = DungeonGenerator.generate(RunSeed(42)).graph
        return RunState(
            seed = RunSeed(42),
            status = status,
            playerHp = playerHp,
            playerMaxHp = playerMaxHp,
            dungeonGraph = graph,
            currentRoomId = graph.startRoomId,
            completedRoomIds = completedRoomIds,
            ownedRelicIds = ownedRelicIds,
            rngState = RunRngState(99),
        )
    }
}
