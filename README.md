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

Gradle does not need to be installed separately. Use the Gradle wrapper included in the repository.

## Running Locally

The game currently runs as two local processes:

1. the Spring Boot game server
2. the Vite development server for the React client

The Vite client proxies `/api` requests to the backend at `http://localhost:8080`.

### 1. Clone the repository

    git clone https://github.com/riseri/discord-roguelike.git
    cd discord-roguelike

### 2. Start the backend

On macOS or Linux:

    ./gradlew :game-server:bootRun

On Windows PowerShell or Command Prompt:

    .\gradlew.bat :game-server:bootRun

The Spring Boot server starts on:

    http://localhost:8080

Leave this terminal running.

### 3. Install frontend dependencies

Open a second terminal from the repository root:

    cd activity-client
    npm install

For repeatable installs after `package-lock.json` already exists, `npm ci` can be used instead.

### 4. Start the frontend

    npm run dev

Vite will print the local development URL in the terminal. By default, it is typically:

    http://localhost:5173

Open that URL in a browser and select **Begin encounter** to play the current combat vertical slice.

### Quick Start

Terminal 1:

    # macOS / Linux
    ./gradlew :game-server:bootRun

    # Windows
    .\gradlew.bat :game-server:bootRun

Terminal 2:

    cd activity-client
    npm install
    npm run dev

Then open the URL shown by Vite in your browser.

## Backend Development

Build:

    ./gradlew build

Run tests:

    ./gradlew test

Check formatting:

    ./gradlew spotlessCheck

Run the server:

    ./gradlew :game-server:bootRun

On Windows, replace `./gradlew` with `.\gradlew.bat`.

## Frontend Development

    cd activity-client
    npm install
    npm run dev

Validation:

    npm run lint
    npm run build

## Verify Your Local Setup

Before starting development, verify both backend and frontend builds succeed.

From the repository root:

    ./gradlew test
    ./gradlew spotlessCheck

On Windows:

    .\gradlew.bat test
    .\gradlew.bat spotlessCheck

Then:

    cd activity-client
    npm ci
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

The project foundation and M1 playable combat encounter are complete.

Current focus: expanding the playable encounter into the first roguelite run loop.
