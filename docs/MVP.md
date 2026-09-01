# MVP

## Goal

Build a playable single-player vertical slice of a turn-based roguelite designed to eventually run as a Discord Activity.

The MVP should prove that the core gameplay loop is fun before multiplayer, guild systems, or other larger features are added.

## Core Loop

1. Start a run as the Knight.
2. Choose a room.
3. Fight enemies or resolve an event.
4. Receive a reward.
5. Improve the current run build.
6. Continue through the dungeon.
7. Fight the boss.
8. Win or die.
9. Receive persistent rewards.
10. Return to the hub.
11. Unlock something that expands future runs.
12. Start another run.

## MVP Content

### Class

Knight only.

### Knight Abilities

- Slash
- Guard
- Shield Bash
- Execute

Detailed combat behavior is defined in `COMBAT.md`.

### Enemies

MVP target:

- 4 normal enemies
- 1 elite enemy
- 1 boss

### Relics

Target 8-10 relics.

Relics should create interactions between mechanics rather than only providing flat stat increases.

Example build directions:

- Bleed
- Block
- Thorns
- Low-health / Berserker
- Execute

### Dungeon

A run uses a branching graph rather than a freely explorable map.

Initial room types:

- Combat
- Event
- Treasure
- Boss

Target approximately 6-8 rooms per run.

### Events

Target 3-5 simple choice-based events.

### Persistent Progression

Use one persistent currency for the MVP:

- Gold

Gold is earned at the end of runs.

Gold can unlock additional relics or other content that becomes available in future runs.

Persistent progression should primarily expand available content rather than provide large permanent stat bonuses.

## Content Targets

Numeric content counts in this document are MVP targets, not requirements for early milestones.

Examples include:

- 4 normal enemies
- 1 elite
- 1 boss
- 8-10 relics
- 3-5 events
- 6-8 rooms

Early milestones may use fewer content items while validating the underlying systems.

Do not create placeholder content solely to reach these counts before the relevant system is ready.

## Incremental Development

The MVP describes the target vertical slice, not the scope of every implementation task.

Work is implemented incrementally through GitHub Issues and milestones.

An issue may intentionally implement only a small portion of the MVP.

Do not implement later MVP systems early unless explicitly required by the current GitHub issue.

For example, a combat-state issue should not also introduce:

- relic systems
- dungeon generation
- persistence
- progression
- Discord integration

The GitHub issue defines the current implementation scope.

## Target Run Length

Initial development target:

    5-10 minutes

Run duration can increase later after the core loop has been validated.

## MVP Success Condition

The MVP is successful when a player can:

- Start a Knight run
- Fight through a dungeon
- Build a combination of relics
- Fight a boss
- Win or die
- Receive persistent rewards
- Unlock something
- Start another run

The core product question is:

> After finishing a run, does the player immediately want to start another one?

## Explicitly Out of Scope

Do not implement the following as part of the MVP:

- Multiplayer
- PvP
- Server guild progression
- World bosses
- Daily dungeons
- Weekly modifiers
- Ascension
- Multiple playable classes
- Trading
- Crafting
- Auction house
- Open-world exploration
- Real-time combat
- Player housing
- Pets
- Complex equipment durability
- Large-scale procedural terrain

These systems may be considered only after the core single-player loop has been proven.