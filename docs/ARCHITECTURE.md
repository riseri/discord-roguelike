# Architecture

## Overview

The project is a turn-based roguelite intended to eventually run as a Discord Activity.

Core gameplay logic must remain independent from presentation, networking, persistence, and Discord.

## Repository Structure

    discord-roguelike/
    ├── game-core/
    ├── game-server/
    ├── activity-client/
    ├── game-data/
    └── docs/

## Dependency Direction

The primary dependency direction is:

    activity-client
        |
        v
    game-server
        |
        v
    game-core

Dependencies must not point upward.

`game-core` must never depend on `game-server` or `activity-client`.

## game-core

Pure Kotlin game simulation.

Responsibilities include:

- Combat
- Game state
- Abilities
- Enemies
- Status effects
- Relic behavior
- Dungeon state
- Run state
- Seeded random behavior
- Game events

`game-core` must not depend on:

- Spring
- HTTP
- WebSockets
- Discord SDK
- React
- Phaser
- databases
- persistence frameworks

`game-core` should be deterministic and easy to unit test.

## game-server

Kotlin + Spring Boot.

Responsibilities include:

- REST APIs
- WebSocket communication if later required
- Authentication
- Persistence
- Player accounts
- Run lifecycle
- Meta progression
- Loading game data
- Connecting clients to `game-core`

`game-server` may depend on `game-core`.

`game-server` must not duplicate authoritative gameplay rules.

## activity-client

TypeScript + React.

Phaser may later be used for graphical combat presentation.

Responsibilities include:

- Rendering game state
- User input
- Menus
- Hub UI
- Route selection
- Combat presentation
- Animations
- Sound
- Discord Activity integration

The client must not contain authoritative game rules.

The client sends player intent and renders authoritative results.

## game-data

Contains static content definitions.

Examples:

- Abilities
- Enemies
- Relics
- Events

Content should be data-driven where doing so keeps the implementation simple.

Do not create overly generic systems solely to avoid writing specialized code.

`game-data` should not contain runtime services or application logic.

## State Ownership

`game-core` owns authoritative gameplay state and gameplay rules.

`game-server` may:

- create game sessions
- load game content
- load and persist state
- invoke `game-core`
- translate API requests into game actions
- translate core results into client-facing responses

`game-server` must not independently calculate combat or dungeon outcomes.

`activity-client` owns presentation and user input only.

The client must never independently determine authoritative outcomes such as:

- damage
- Block consumption
- enemy actions
- status application
- combat victory
- rewards

## Module Communication

Prefer explicit data passed across module boundaries.

Avoid:

- shared mutable global state
- static service locators
- hidden cross-module dependencies
- presentation code controlling gameplay rules
- persistence code controlling gameplay rules

Expected flow:

    player input
        |
        v
    activity-client
        |
        v
    game-server
        |
        v
    GameAction
        |
        v
    game-core
        |
        v
    ActionResult
        |
        v
    game-server
        |
        v
    activity-client

## Core Game API

Game state should be modified through explicit actions.

Conceptually:

    GameState + GameAction -> ActionResult

An `ActionResult` contains:

- the updated authoritative state
- the game events produced during resolution

Game events describe what happened without giving presentation concerns control over gameplay.

Examples:

- `DamageDealt`
- `BlockGained`
- `StatusApplied`
- `EntityDefeated`
- `AbilityUsed`
- `RelicTriggered`
- `CombatWon`

## Game Actions

Use sealed interfaces or equivalent closed domain types where appropriate.

Examples:

- `UseAbility`
- `ChooseRoom`
- `SelectReward`
- `ResolveEventChoice`

Actions represent player or system intent.

Actions should not directly contain presentation concerns.

## Randomness

All gameplay randomness must be deterministic.

Every run has a seed.

Do not call global or default random functions directly from authoritative game logic.

Random behavior should use seeded RNG associated with the run or explicitly passed through the relevant logic.

This enables:

- reproducible bugs
- deterministic tests
- replay/debugging support
- seeded runs later

## State

Prefer explicit immutable state transitions where practical.

Prefer:

    old state
    + action
    = new state

Avoid objects silently mutating unrelated shared state.

## Persistence

The core engine must not know whether state is stored in:

- memory
- PostgreSQL
- Redis
- files
- another persistence system

Persistence belongs outside `game-core`.

## Where New Code Belongs

Place behavior in the module that owns the responsibility.

Examples:

- Damage rules -> `game-core`
- Ability behavior -> `game-core`
- Enemy turns -> `game-core`
- Relic gameplay effects -> `game-core`
- Dungeon rules -> `game-core`
- REST endpoints -> `game-server`
- Authentication -> `game-server`
- Persistence -> `game-server`
- Loading files from `game-data` -> `game-server` or an adapter outside `game-core`
- UI state -> `activity-client`
- Rendering -> `activity-client`
- Static enemy definitions -> `game-data`
- Static relic definitions -> `game-data`

Do not move logic to another module simply because it is easier to access there.

## Design Principle

Prefer the simplest implementation that supports the current MVP.

Do not build infrastructure for speculative future systems unless the current issue requires it.