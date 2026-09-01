package dev.riseri.core.combat

@JvmInline
value class CombatRngState(
    val value: Long,
) {
    /** Advances an explicit linear-congruential state so random choices can be replayed exactly. */
    fun nextInt(bound: Int): RandomIntResult {
        require(bound > 0) { "Random bound must be positive" }

        // Long overflow is part of the generator definition. The unsigned shift keeps modulo input
        // non-negative without relying on platform or global randomness.
        val nextValue = value * MULTIPLIER + INCREMENT
        val randomValue = ((nextValue ushr 1) % bound.toLong()).toInt()
        return RandomIntResult(randomValue, CombatRngState(nextValue))
    }

    private companion object {
        const val MULTIPLIER = 6_364_136_223_846_793_005L
        const val INCREMENT = 1_442_695_040_888_963_407L
    }
}

data class RandomIntResult(
    val value: Int,
    val nextState: CombatRngState,
)
