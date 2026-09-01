# Combat

## Goal

Combat is turn-based, readable, deterministic, and designed around building synergistic combinations during a run.

Players should usually understand what an enemy intends to do before choosing their action.

## Initial Combat Model

The MVP uses:

- One player character
- One or more enemies
- Player turn
- Enemy turn
- Telegraphable enemy intentions

Real-time combat is out of scope.

## Player

Initial class:

Knight

Initial stats:

```text
Max HP: 100
HP: 100
Block: 0
```

Additional stats such as critical-hit chance may be introduced as needed.

## Knight Abilities

### Slash

Basic attack.

Initial behavior:

```text
Damage: 15
Cooldown: none
Target: one enemy
```

### Guard

Defensive action.

Initial behavior:

```text
Gain: 12 Block
Cooldown: none
Target: self
```

### Shield Bash

Damage plus control.

Initial behavior:

```text
Damage: 8
Apply Stun: 1 turn
Cooldown: 3 turns
Target: one enemy
```

### Execute

Conditional damage ability.

Initial behavior:

```text
Base Damage: 20

If target HP is below 25%:
Damage: 40

Cooldown: TBD
Target: one enemy
```

Exact balance values may change during testing.

## Turn Flow

The initial combat loop is:

1. Enemy intentions are visible.
2. Player selects an ability.
3. Player action resolves.
4. Resulting effects resolve.
5. Enemy deaths are checked.
6. If combat continues, enemies take their actions.
7. Resulting effects resolve.
8. Player death is checked.
9. Turn-based statuses and cooldowns advance.
10. New enemy intentions are generated.
11. Control returns to the player.

This order may be refined as mechanics become more complex.

## Block

Block absorbs incoming damage before HP.

Example:

```text
Player HP: 100
Player Block: 12
Incoming Damage: 20
```

Result:

```text
Block: 0
HP: 92
```

Block is removed before HP damage is applied.

For the MVP, remaining Block expires at the start of the player's next turn unless changed later.

## Damage

Initial damage pipeline:

```text
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
```

Avoid prematurely creating an overly generic modifier engine.

The pipeline may evolve as additional mechanics are implemented.

## Enemy Intentions

Enemies choose an action before the player makes their next decision.

Example:

```text
Goblin Brute

Intent:
Heavy Swing
20 damage
```

Once shown to the player, an intention must not randomly change before it resolves.

This allows the player to make decisions based on enemy behavior.

## Stun

A stunned entity skips its next action.

Initial behavior:

```text
Stun duration: 1 turn
```

When the affected entity attempts to act:

1. Skip the action.
2. Reduce or remove Stun.
3. Continue combat.

## Death

An entity is defeated when:

```text
HP <= 0
```

Externally exposed HP should be clamped to zero rather than represented as a negative value.

## Combat Victory

Combat ends successfully when all enemies are defeated.

The result should generate a combat victory event.

Example:

```text
CombatWon
```

The dungeon system can use that event to transition to rewards.

## Combat Failure

The run ends when the player reaches zero HP.

For the MVP there is no revive system.

Multiplayer downed/revive mechanics are explicitly out of scope.

## Initial Enemy Examples

### Goblin

```text
HP: 40

Stab:
10 damage
```

### Goblin Brute

```text
HP: 70

Punch:
8 damage

Heavy Swing:
20 damage
```

Heavy Swing should be clearly telegraphed.

### Orc Warlord

Initial boss.

```text
HP: 180
```

Actions:

```text
Slash
12 damage

Crushing Blow
30 damage
Telegraphed

War Cry
Increase future damage
```

Exact boss behavior can be refined after normal combat works.

## Relics

Relics should create interactions between combat mechanics.

Initial examples:

### Bloodied Blade

```text
Critical hits apply Bleed.
```

### Serrated Edge

```text
Bleed deals 50% more damage.
```

### Vampiric Fang

```text
Heal for a percentage of Bleed damage dealt.
```

### Iron Bulwark

```text
Guard grants additional Block.
```

### Spiked Armor

```text
When Block absorbs damage, deal some damage back to the attacker.
```

### Executioner's Mark

```text
Increase Execute's bonus-damage threshold.
```

### Berserker's Ring

```text
Increase damage while below a health threshold.
```

### Heavy Gauntlets

```text
Increase Shield Bash damage.
```

Exact values are subject to balancing.

## Relic Interaction Architecture

Relics may react to combat events.

Conceptually:

```text
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
```

Avoid placing knowledge of every relic directly inside the combat engine.

Prefer emitting meaningful game events that relic systems can react to.

Do not build a highly generic event/effect scripting engine during the MVP unless actual implementation needs justify it.

## Determinism

Combat must be deterministic given:

- Starting GameState
- Action sequence
- RNG seed

A bug should ideally be reproducible using the same seed and action sequence.

## Random Number Generation

Game logic must not directly use unseeded global randomness.

Random values should come from the run's deterministic RNG.

Potential uses include:

- Critical hits
- Enemy action selection
- Reward generation
- Event outcomes

## Testing Requirements

At minimum, combat tests should cover:

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

Random tests must use fixed seeds.

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