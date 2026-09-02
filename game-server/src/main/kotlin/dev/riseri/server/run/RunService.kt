package dev.riseri.server.run

import dev.riseri.core.combat.EnemyContentId
import dev.riseri.core.combat.EnemyDefinition
import dev.riseri.core.combat.GameAction
import dev.riseri.core.run.RunAction
import dev.riseri.core.run.RunCombatActionResult
import dev.riseri.core.run.RunCombatEngine
import dev.riseri.core.run.RunEngine
import dev.riseri.core.run.RunSeed
import dev.riseri.core.run.RunState
import org.springframework.stereotype.Service
import kotlin.random.Random

class NoActiveRunException : IllegalStateException("No active run")

/** Keeps the single M2.0 run in memory while delegating lifecycle rules to game-core. */
@Service
class RunService {
    private var state: RunState? = null

    @Synchronized
    fun start(seed: Long?): RunResponse {
        val action = RunAction.StartRun(RunSeed(seed ?: Random.nextLong()))
        val result = RunEngine.execute(state, action)

        // Commit only a successful core transition so rejected starts preserve the active run.
        state = result.state
        return RunResponse.from(result.state)
    }

    @Synchronized
    fun current(): RunResponse = RunResponse.from(state ?: throw NoActiveRunException())

    @Synchronized
    internal fun startCombat(enemyDefinitions: Map<EnemyContentId, EnemyDefinition>): RunState {
        val updated = RunCombatEngine.start(state ?: throw NoActiveRunException(), enemyDefinitions)
        state = updated
        return updated
    }

    @Synchronized
    internal fun currentState(): RunState = state ?: throw NoActiveRunException()

    @Synchronized
    internal fun executeCombat(
        action: GameAction.UseAbility,
        enemyDefinitions: Map<EnemyContentId, EnemyDefinition>,
    ): RunCombatActionResult {
        val result = RunCombatEngine.execute(state ?: throw NoActiveRunException(), action, enemyDefinitions)
        state = result.state
        return result
    }
}
