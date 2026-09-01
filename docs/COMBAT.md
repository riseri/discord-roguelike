# Combat

## Goal

Combat is turn-based, readable, deterministic, and designed around building synergistic combinations during a run.

Players should usually understand what an enemy intends to do before choosing their action.

## Initial Combat Model

The MVP uses:

- One player character
- One or more enemies
- Player turns
- Enemy turns
- Telegraphable enemy intentions

Real-time combat is out of scope.

## Combat Terminology

A `round` begins when control is given to the player and ends after enemy actions and end-of-round processing complete.

A `player turn` is the portion of the round in which the player chooses and resolves an action.

An `enemy turn` is the portion in which enemies resolve their previously generated intentions.

An `intention` is the action an enemy has committed to performing during its next enemy turn.

Once shown to the player, an intention must remain stable until it resolves or becomes invalid because combat has already ended.

Use these terms consistently in implementation, tests, issues, and documentation.

## Player

Initial class:

    Knight

Initial stats:

    Max HP: 100
    HP: 100
    Block: 0

Additional stats should only be introduced when required by actual gameplay mechanics.

## Knight Abilities

### Slash

Basic attack.

Initial behavior:

    Damage: 15
    Cooldown: none
    Target: one enemy

### Guard

Defensive action.

Initial behavior:

    Gain: 12 Block
    Cooldown: none
    Target: self

### Shield Bash

Damage plus control.

Initial behavior:

    Damage: 8
    Apply Stun: 1 turn
    Cooldown: 3 turns
    Target: one enemy

### Execute

Conditional damage ability.

Initial behavior:

    Base Damage: 20

    If target HP is below 25%:
    Damage: 40

    Cooldown: TBD
    Target: one enemy

Exact balance values may change during testing.

## Turn Flow

The initial combat loop is:

1. Enemy intentions are visible.
2. Player selects an ability.
3. Validate the player action.
4. Resolve the player action.
5. Resolve resulting effects required by the current implementation.
6. Check enemy deaths.
7. Check combat victory.
8. If combat continues, enemies resolve their intentions.
9. Resolve resulting enemy effects.
10. Check player death.
11. If combat continues, advance statuses and cooldowns.
12. Expire or update Block according to its rules.
13. Generate new enemy intentions.
14. Return control to the player.

Do not continue normal combat resolution after combat has reached a terminal state.

This order may be refined later when additional mechanics require it.

## Action Resolution

A submitted player action should resolve atomically from the perspective of callers.

The caller should not manually perform individual combat steps such as:

- applying damage
- checking deaths
- executing enemy actions
- advancing statuses
- generating new intentions

The authoritative combat engine performs the required sequence and returns the resulting state and generated game events.

Do not expose partially resolved combat state unless a future mechanic explicitly requires it.

## Block

Block absorbs incoming damage before HP.

Example:

    Player HP: 100
    Player Block: 12
    Incoming Damage: 20

Result:

    Block: 0
    HP: 92

Block is removed before HP damage is applied.

For the MVP, remaining Block expires at the start of the player's next turn unless explicitly changed later.

## Damage

Initial damage pipeline:

    Base Damage
        |
        v
    Attacker Modifiers
        |
        v
    Defender Modifiers
        |
        v
    Block
        |
        v
    HP Damage

Avoid prematurely creating an overly generic modifier engine.

The pipeline may evolve as actual mechanics require it.

## Enemy Intentions

Enemies choose an action before the player makes their next decision.

Example:

    Goblin Brute

    Intent:
    Heavy Swing
    20 damage

Once shown to the player, an intention must not randomly change before it resolves.

This allows the player to make decisions based on enemy behavior.

Intent generation must use deterministic seeded randomness when randomness is involved.

## Stun

A stunned entity skips its next action.

Initial behavior:

    Stun duration: 1 action

When the affected entity would act:

1. Skip the action.
2. Reduce or remove Stun.
3. Continue combat if combat remains active.

## Death

An entity is defeated when:

    HP <= 0

Externally exposed HP must be clamped to zero rather than represented as a negative value.

## Combat Victory

Combat ends successfully when all enemies are defeated.

Victory should produce a meaningful game event such as:

    CombatWon

The dungeon or run system can later use this event to transition to rewards.

## Combat Failure

The run ends when the player reaches zero HP.

For the MVP there is no revive system.

Multiplayer downed or revive mechanics are explicitly out of scope.

## Initial Enemy Examples

### Goblin

    HP: 40

    Stab:
    10 damage

### Goblin Brute

    HP: 70

    Punch:
    8 damage

    Heavy Swing:
    20 damage

Heavy Swing should be clearly telegraphed.

### Orc Warlord

Initial boss.

    HP: 180

Actions:

    Slash
    12 damage

    Crushing Blow
    30 damage
    Telegraphed

    War Cry
    Increase future damage

Exact boss behavior can be refined after normal combat works.

## Relics

Relics should create interactions between combat mechanics.

Initial examples:

### Bloodied Blade

    Critical hits apply Bleed.

### Serrated Edge

    Bleed deals 50% more damage.

### Vampiric Fang

    Heal for a percentage of Bleed damage dealt.

### Iron Bulwark

    Guard grants additional Block.

### Spiked Armor

    When Block absorbs damage, deal some damage back to the attacker.

### Executioner's Mark

    Increase Execute's bonus-damage threshold.

### Berserker's Ring

    Increase damage while below a health threshold.

### Heavy Gauntlets

    Increase Shield Bash damage.

Exact values are subject to balancing.

## Relic Interaction Architecture

Relics may react to meaningful combat events.

Conceptually:

    Critical Hit
         |
         v
    CriticalHit event
         |
         v
    Bloodied Blade reacts
         |
         v
    Apply Bleed

Avoid placing knowledge of every relic directly inside unrelated combat code.

Prefer emitting meaningful domain events that appropriate systems can react to.

Do not build a highly generic event or effect scripting engine during the MVP unless actual implementation requirements justify it.

## Determinism

Combat must be deterministic given:

- Starting game state
- Action sequence
- RNG seed

A bug should ideally be reproducible using the same state, seed, and action sequence.

## Random Number Generation

Game logic must not directly use unseeded global randomness.

Random values should come from deterministic seeded RNG.

Potential uses include:

- Critical hits
- Enemy action selection
- Reward generation
- Event outcomes

## Rule Precedence

When multiple combat effects apply:

1. Validate the action.
2. Resolve the acting entity's ability.
3. Apply resulting damage, Block, and statuses.
4. Resolve required triggered effects.
5. Check deaths.
6. Check terminal combat state.
7. Continue normal turn flow only if combat remains active.

Do not create a universal proc-priority framework until actual mechanics require one.

## Testing Requirements

At minimum, combat tests should eventually cover:

- Normal damage
- Lethal damage
- HP clamping at zero
- Block absorbing all incoming damage
- Damage exceeding Block
- Guard
- Shield Bash
- Stun
- Execute above threshold
- Execute below threshold
- Enemy intention generation
- Enemy intention stability
- Enemy turn execution
- Player death
- Enemy death
- Combat victory

Randomized tests must use fixed seeds.

Tests should be added incrementally as the corresponding mechanics are implemented.

## Current Priority

Implement the smallest correct combat model first.

Do not implement:

- Multiplayer combat
- Elemental reactions
- Mana
- Complex buffs and debuffs
- Dozens of status effects
- Equipment
- Advanced damage formulas
- Deep proc chains
- Boss phase systems

Those can be added after the basic Knight combat loop works.