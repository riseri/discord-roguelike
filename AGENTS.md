# Discord Roguelike

## Goal

Build a short-session turn-based roguelite intended to eventually run as a Discord Activity.

The current priority is the single-player MVP.

## Documentation Guide

Before implementing a task, read the documentation relevant to the area being changed.

Use:

- `docs/MVP.md` for MVP scope and the core gameplay loop
- `docs/ARCHITECTURE.md` for module boundaries and dependency direction
- `docs/COMBAT.md` for combat rules and behavior
- `docs/DEVELOPMENT.md` for repository workflow
- GitHub Issues for task-specific requirements and acceptance criteria

If documentation and existing implementation conflict, do not silently choose one.

Call out the conflict before expanding the task.

## Decision Priority

When deciding how to implement something, use this priority:

1. GitHub issue acceptance criteria
2. `AGENTS.md`
3. Relevant documentation under `docs/`
4. Existing architecture and established project patterns
5. Smallest reasonable implementation

Do not introduce a new architectural pattern when the existing architecture can satisfy the requirement.

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

## Dependency Direction

The primary dependency direction is:

    activity-client
        |
        v
    game-server
        |
        v
    game-core

Never introduce dependencies in the opposite direction.

`game-core` must remain usable without `game-server` or `activity-client`.

`game-data` contains content definitions and must not become a home for runtime application logic.

## State Ownership

`game-core` is authoritative for gameplay state and rules.

`game-server` may store, retrieve, and orchestrate game state, but must not duplicate gameplay rules.

`activity-client` displays state and sends player intent.

The client must not independently calculate authoritative gameplay outcomes.

## game-core

Pure Kotlin game simulation.

Must not depend on:

- Spring
- HTTP
- WebSockets
- databases
- persistence frameworks
- Discord
- React
- Phaser

Game rules belong here.

Gameplay state transitions should be deterministic and explicit.

## game-server

Kotlin + Spring Boot.

Responsible for:

- APIs
- persistence
- authentication
- run lifecycle
- loading game data
- connecting clients to `game-core`

May depend on `game-core`.

## activity-client

TypeScript + React.

Phaser may be introduced later for combat presentation.

Responsible for:

- UI
- rendering
- animation
- input
- Discord integration

The client must not contain authoritative game rules.

## game-data

Contains static content definitions for:

- enemies
- abilities
- relics
- events

Do not place runtime services or application logic in `game-data`.

## Java

This project uses JDK 25.

All backend development, testing, CI, and deployment target Java 25.

Do not downgrade the Java toolchain unless explicitly requested.

## Architecture

Prefer:

    GameState + GameAction -> ActionResult

`ActionResult` should contain:

- updated state
- generated `GameEvent` values

Prefer deterministic and explicit state transitions.

Avoid hidden global state.

## Randomness

Gameplay randomness must be seeded and reproducible.

Do not use unseeded global randomness inside `game-core`.

The same starting state, action sequence, and RNG seed should produce the same result.

## Kotlin

Prefer:

- immutable data classes
- sealed interfaces
- explicit domain types
- small deterministic functions
- descriptive names

Do not over-engineer abstractions before they are required.

## Before Adding New Types

Before creating a new abstraction, service, interface, manager, factory, or generic framework:

1. Check whether an existing type already owns the responsibility.
2. Prefer extending the existing design when appropriate.
3. Add a new abstraction only when the current issue actually requires it.

Do not design infrastructure for hypothetical future features.

## Code Comments

Add comments for non-obvious behavior, constraints, design decisions, and domain rules.

For new non-trivial classes and functions, include a brief comment when the purpose or responsibility is not immediately obvious from the name alone.

When implementing gameplay logic, prefer documenting:

- why a rule exists
- when a state transition occurs
- invariants that must remain true
- determinism or RNG constraints
- edge cases
- ordering requirements
- architectural boundaries
- behavior that would be easy for a future maintainer to accidentally change

When modifying existing non-trivial logic that has no explanation, add a comment if future maintainers would benefit from understanding the intent.

Do not add comments that merely restate the code.

Prefer clear naming and small functions, but use comments when naming alone does not explain intent.

## Testing

All game mechanics must have deterministic unit tests.

Bug fixes should include regression tests when practical.

Place tests with the module that owns the behavior.

Examples:

- Combat rules -> `game-core`
- API/application orchestration -> `game-server`
- UI behavior -> `activity-client`

Prefer focused deterministic unit tests for `game-core`.

Use integration tests when behavior crosses a meaningful application or module boundary.

Do not replace focused unit tests with broad integration tests when the behavior can be tested directly.

## Scope Control

Do not silently expand a task.

The GitHub issue defines the current implementation scope.

Satisfy every acceptance criterion.

Do not implement behavior listed under Out of Scope.

Prefer the smallest implementation that satisfies the issue and the current MVP.

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

## Visual Review Gate

For any task that materially changes `activity-client` presentation, visual review is required before opening a pull request.

Examples include changes to:

- layout
- styling
- combat presentation
- responsive behavior
- animations or transitions
- new user-facing screens or components

Before opening a pull request for a UI-affecting task:

1. Start the required backend and frontend processes locally.
2. Open the affected flow in a browser.
3. Exercise the changed user interaction from start to finish.
4. Capture at least one screenshot of each materially changed state needed to review the work.
5. Inspect the screenshots for obvious problems in layout, spacing, hierarchy, readability, overflow, contrast, and consistency with the issue's design direction.
6. Fix obvious visual problems before presenting the work as complete.
7. Show the final screenshots to the user for visual approval.
8. Do not open the pull request until the user explicitly approves the visual result.

Functional tests, linting, and builds do not replace visual review for presentation changes.

For browser-reviewed UI work, prefer deterministic/reproducible application state when practical so screenshots can be compared meaningfully across iterations.

Do not treat placeholder art or temporary assets as justification for ignoring layout and presentation quality.

## Generated Project State

Do not manually modify generated project-state documentation if such files are introduced.

GitHub Issues and milestones are the authoritative source for task completion state.

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