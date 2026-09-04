package dev.riseri.core.random

internal data class DeterministicRandomInt(
    val value: Int,
    val nextState: Long,
)

/** Advances the shared explicit RNG state used by authoritative gameplay systems. */
internal fun nextDeterministicInt(
    state: Long,
    bound: Int,
): DeterministicRandomInt {
    require(bound > 0) { "Random bound must be positive" }

    // Long overflow is part of the generator definition. The unsigned shift keeps modulo input
    // non-negative without relying on platform or global randomness.
    val nextState = state * MULTIPLIER + INCREMENT
    val value = ((nextState ushr 1) % bound.toLong()).toInt()
    return DeterministicRandomInt(value, nextState)
}

private const val MULTIPLIER = 6_364_136_223_846_793_005L
private const val INCREMENT = 1_442_695_040_888_963_407L
