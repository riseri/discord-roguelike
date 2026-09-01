# Discord Roguelike

## Goal

Build a short-session turn-based roguelite intended to eventually run as a Discord Activity.

The current priority is the single-player MVP.

## MVP Scope

The MVP contains:

- One playable class: Knight
- Turn-based combat
- Four abilities
- A small set of enemies
- Relics
- A branching dungeon
- One boss
- Basic persistent progression

Do not implement post-MVP features unless explicitly requested.

Post-MVP features include:

- Multiplayer
- PvP
- Guild progression
- World bosses
- Daily dungeons
- Multiple classes
- Trading
- Crafting
- Ascension

## Repository

### game-core

Pure Kotlin game simulation.

Must not depend on:

- Spring
- HTTP
- WebSockets
- databases
- Discord
- React
- Phaser

Game rules belong here.

### game-server

Kotlin + Spring Boot.

Responsible for:

- APIs
- persistence
- authentication
- run lifecycle
- connecting clients to game-core

May depend on game-core.

### activity-client

TypeScript + React.

Phaser may be used for combat rendering.

Responsible for:

- UI
- rendering
- animation
- input
- Discord integration

The client must not contain authoritative game rules.

### game-data

Data definitions for:

- enemies
- abilities
- relics
- events

## Java

This project uses JDK 25.

All backend development, testing, CI, and deployment should target Java 25.

Do not downgrade the Java toolchain unless explicitly requested.

## Architecture

Prefer:

GameState + GameAction -> ActionResult

ActionResult should contain:

- updated state
- generated GameEvents

Prefer deterministic and explicit state transitions.

Avoid hidden global state.

## Randomness

Gameplay randomness must be seeded and reproducible.

Do not use unseeded global randomness inside game-core.

## Kotlin

Prefer:

- immutable data classes
- sealed interfaces
- explicit domain types
- small deterministic functions

Do not over-engineer abstractions before they are required.

## Testing

All game mechanics must have deterministic unit tests.

Bug fixes should include regression tests when practical.

When modifying game-core, run the game-core tests before finishing.

## Scope Control

Do not silently expand a task.

Prefer the smallest implementation that satisfies the current MVP.

## Required Validation

Before completing a task:

### Backend changes

Run:

    ./gradlew test
    ./gradlew spotlessCheck

### Frontend changes

Run:

    cd activity-client
    npm run lint
    npm run build

Do not mark a task complete when required validation fails.

## Generated Project State

Do not manually modify:

- `docs/CURRENT_MILESTONE.md`
- `docs/PROGRESS.md`

These files are generated from GitHub project state.

## Branch Naming

Do not use `codex` as a branch name or branch prefix.

Use:

- `feature/<issue-number>-<short-description>` for feature work
- `bugfix/<issue-number>-<short-description>` for bug fixes
- `chore/<issue-number>-<short-description>` for maintenance or tooling
- `docs/<issue-number>-<short-description>` for documentation-only work

Examples:

    feature/2-enemy-combat-state
    bugfix/24-block-damage-calculation
    chore/30-update-ci
    docs/31-update-combat-rules