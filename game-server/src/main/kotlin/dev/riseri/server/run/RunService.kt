package dev.riseri.server.run

import dev.riseri.core.run.RunAction
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
}
