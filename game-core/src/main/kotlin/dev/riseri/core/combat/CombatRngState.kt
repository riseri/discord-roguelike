package dev.riseri.core.combat

@JvmInline
value class CombatRngState(
    val value: Long,
) {
    fun nextInt(bound: Int): RandomIntResult {
        require(bound > 0) { "Random bound must be positive" }

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
