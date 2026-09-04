package dev.riseri.core.combat

import dev.riseri.core.random.nextDeterministicInt

@JvmInline
value class CombatRngState(
    val value: Long,
) {
    /** Advances an explicit linear-congruential state so random choices can be replayed exactly. */
    fun nextInt(bound: Int): RandomIntResult {
        val random = nextDeterministicInt(value, bound)
        return RandomIntResult(random.value, CombatRngState(random.nextState))
    }
}

data class RandomIntResult(
    val value: Int,
    val nextState: CombatRngState,
)
