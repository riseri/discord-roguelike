# Discord Roguelike

A short-session turn-based roguelite designed to eventually run as a Discord Activity.

The current goal is a single-player MVP that proves the core gameplay loop before larger systems such as multiplayer, guilds, or multiple classes are introduced.

## MVP

The MVP includes:

- One playable class: Knight
- Turn-based combat
- Four Knight abilities
- A small enemy roster
- Relics
- A branching dungeon
- One boss
- Basic persistent progression
- A React-based Activity client

A typical run follows this loop:

1. Start a Knight run.
2. Choose a room.
3. Fight enemies or resolve an event.
4. Receive a reward.
5. Improve the current build.
6. Continue through the dungeon.
7. Fight the boss.
8. Win or die.
9. Receive persistent rewards.
10. Return to the hub.
11. Unlock content for future runs.
12. Start another run.

## Repository Structure

    discord-roguelike/
    ├── game-core/        Pure Kotlin game logic
    ├── game-server/      Kotlin + Spring Boot backend
    ├── activity-client/  React + TypeScript client
    ├── game-data/        Static game content definitions
    └── docs/             Design and architecture documentation

## Requirements

- JDK 25
- Node.js 24+
- npm

## Backend

Build:

    ./gradlew build

Run tests:

    ./gradlew test

Check formatting:

    ./gradlew spotlessCheck

Run the server:

    ./gradlew :game-server:bootRun

## Frontend

    cd activity-client
    npm install
    npm run dev

Validation:

    npm run lint
    npm run build

## Documentation

- `docs/MVP.md` - MVP scope and gameplay loop
- `docs/ARCHITECTURE.md` - Architecture and module ownership
- `docs/COMBAT.md` - Combat rules and mechanics
- `docs/DEVELOPMENT.md` - Development workflow and sources of truth
- `AGENTS.md` - Repository instructions for coding agents

## Development

Implementation work is tracked through GitHub Issues and milestones.

GitHub Issues define task-specific requirements and acceptance criteria.

## Status

Project foundation is complete.

Current focus: playable combat encounter.