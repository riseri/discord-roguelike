# Discord Roguelike

A short-session turn-based roguelite designed to eventually run as a Discord Activity.

The current goal is a single-player MVP that proves the core gameplay loop before adding larger systems such as multiplayer, guilds, or multiple classes.

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
- A React-based activity client

A typical run follows this loop:

1. Start a Knight run
2. Choose a room
3. Fight enemies or resolve an event
4. Receive a reward
5. Improve the current build
6. Continue through the dungeon
7. Fight the boss
8. Win or die
9. Receive persistent rewards
10. Start another run

## Repository Structure

    discord-roguelike/
    ├── game-core/        Pure Kotlin game logic
    ├── game-server/      Kotlin + Spring Boot backend
    ├── activity-client/  React + TypeScript client
    ├── game-data/        Static game content definitions
    └── docs/             Design and architecture documentation

### game-core

Contains deterministic game simulation such as:

- Combat
- Abilities
- Enemies
- Relic behavior
- Dungeon state
- Run state
- Seeded randomness

`game-core` must not depend on Spring, HTTP, persistence, Discord, React, or Phaser.

### game-server

Spring Boot backend responsible for:

- APIs
- Persistence
- Authentication
- Run lifecycle
- Connecting the client to `game-core`

### activity-client

React + TypeScript frontend responsible for:

- UI
- Player input
- Combat presentation
- Route selection
- Discord Activity integration

The client does not contain authoritative game rules.

### game-data

Contains content definitions for:

- Enemies
- Abilities
- Relics
- Events

## Requirements

- JDK 25
- Node.js 24+
- npm

## Build and Test

### Backend

From the repository root:

    ./gradlew build

Run tests:

    ./gradlew test

Check formatting:

    ./gradlew spotlessCheck

### Frontend

    cd activity-client
    npm install
    npm run dev

Validation:

    npm run lint
    npm run build

## Running the Server

    ./gradlew :game-server:bootRun

## Documentation

Detailed project documentation is available under `docs/`.

- `docs/MVP.md` - MVP scope and core gameplay loop
- `docs/ARCHITECTURE.md` - Module boundaries and architecture
- `docs/COMBAT.md` - Combat design

## Development

This project uses GitHub Issues and milestones to track implementation work.

Repository-specific agent instructions are defined in `AGENTS.md`.

## Status

Project foundation is complete.

Current development focus: playable combat encounter.
