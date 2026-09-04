# Discord Roguelike

## Goal

Build a short-session turn-based roguelite intended to eventually run as a Discord Activity.

The current priority is the single-player MVP.

## Sources of Truth

Before implementing a task, read the documentation relevant to the area being changed.

Use:

- GitHub Issues for task-specific requirements and acceptance criteria
- `docs/MVP.md` for MVP scope and the core gameplay loop
- `docs/ARCHITECTURE.md` for module boundaries and dependency direction
- `docs/COMBAT.md` for combat rules and behavior
- `docs/DEVELOPMENT.md` for the human development workflow
- this file for Codex implementation rules

When deciding how to implement something, use this priority:

1. GitHub issue acceptance criteria
2. `AGENTS.md`
3. Relevant documentation under `docs/`
4. Existing architecture and established project patterns
5. Smallest reasonable implementation

If documentation and existing implementation conflict, do not silently choose one. Call out the conflict before expanding the task.

## Scope Control

The GitHub Issue defines the current implementation scope.

- satisfy every acceptance criterion
- do not implement behavior listed under Out of Scope
- do not silently expand the task
- prefer the smallest implementation that satisfies the issue and current MVP
- do not implement post-MVP features unless explicitly requested

Current MVP includes one Knight class, turn-based combat, four abilities, a small enemy set, relics, a branching dungeon, one boss, and basic persistent progression.

Post-MVP examples include multiplayer, PvP, guild progression, world bosses, daily dungeons, multiple classes, trading, crafting, and ascension.

## Architecture and Ownership

Primary dependency direction:

    activity-client -> game-server -> game-core

Never introduce dependencies in the opposite direction.

### game-core

Pure Kotlin game simulation.

- authoritative for gameplay state and rules
- deterministic and explicit state transitions
- no Spring, HTTP, WebSockets, databases, persistence frameworks, Discord, React, or Phaser
- must remain usable without `game-server` or `activity-client`

Prefer:

    GameState + GameAction -> ActionResult

`ActionResult` should contain updated state and generated `GameEvent` values.

### game-server

Kotlin + Spring Boot.

Responsible for APIs, persistence, authentication, run lifecycle, loading game data, and connecting clients to `game-core`.

It may store, retrieve, and orchestrate game state, but must not duplicate authoritative gameplay rules.

### activity-client

TypeScript + React.

Responsible for UI, rendering, animation, input, and Discord integration.

The client displays authoritative state and sends player intent. It must not independently calculate authoritative gameplay outcomes.

### game-data

Contains static content definitions such as enemies, abilities, relics, and events.

Do not place runtime services or application logic in `game-data`.

## Java and Kotlin

This project uses JDK 25 for backend development, testing, CI, and deployment. Do not downgrade the Java toolchain unless explicitly requested.

Prefer in Kotlin:

- immutable data classes
- sealed interfaces
- explicit domain types
- small deterministic functions
- descriptive names

Do not over-engineer abstractions before they are required.

Before creating a new service, interface, manager, factory, abstraction, or generic framework:

1. Check whether an existing type already owns the responsibility.
2. Prefer extending the existing design when appropriate.
3. Add a new abstraction only when the current issue requires it.

Do not design infrastructure for hypothetical future features.

## Determinism and Randomness

Gameplay randomness must be seeded and reproducible.

Do not use unseeded global randomness inside `game-core`.

The same starting state, action sequence, and RNG seed should produce the same result.

## Code Comments

Add comments for non-obvious behavior, constraints, design decisions, and domain rules.

For new non-trivial classes and functions, include a brief comment when purpose or responsibility is not immediately obvious from the name alone.

Gameplay comments should explain why a rule exists, transition timing, invariants, determinism constraints, edge cases, ordering requirements, architectural boundaries, or behavior that would be easy to accidentally change.

Do not add comments that merely restate the code. Prefer clear naming and small functions.

## Testing

All game mechanics must have deterministic unit tests.

Bug fixes should include regression tests when practical.

Place tests with the module that owns the behavior:

- combat/game rules -> `game-core`
- API/application orchestration -> `game-server`
- UI behavior -> `activity-client`

Prefer focused deterministic unit tests for `game-core`.

Use integration tests when behavior crosses a meaningful application or module boundary. Do not replace focused unit tests with broad integration tests when direct testing is practical.

## Validation Strategy

Use the narrowest useful validation while implementation is still changing.

During implementation:

- run focused tests for the behavior or module being changed
- prefer a specific test class, test package, Gradle module, lint target, or affected build when available
- do not repeatedly run the entire repository test suite after every small edit
- when a focused check fails, fix and rerun that focused check before escalating to broader validation

Before considering the implementation complete, run the required final validation once the implementation has stabilized.

### Backend changes

    ./gradlew test
    ./gradlew spotlessCheck

### Frontend changes

    cd activity-client
    npm run lint
    npm run build

If final validation reveals a defect:

1. fix the defect
2. rerun the smallest relevant validation first
3. rerun full required validation only after the implementation is stable again

Do not mark a task complete when required final validation fails. Do not repeatedly rerun full validation when no relevant code has changed.

## Review Efficiency

Before presenting work as complete:

1. Inspect the final diff.
2. Check for unrelated changes.
3. Verify every acceptance criterion against the implementation.
4. Perform one focused self-review for obvious defects.
5. Fix obvious defects before asking the user to review the result.

Review the task against the GitHub Issue and final diff rather than re-evaluating the entire repository.

Do not reread unrelated repository files solely for final review, repeatedly summarize the architecture, rerun unchanged validation without a reason, or repeat identical browser-review passes when application state has not changed.

After user feedback, focus on the files and behavior affected by that feedback.

## Interactive Review Gate

For any task that materially changes `activity-client` presentation or gameplay flow, interactive user review is required before opening a pull request.

Examples include layout, styling, combat presentation, responsive behavior, animations, new user-facing screens, navigation changes, room/reward flows, or changes to how a player starts, continues, wins, or loses a run.

For interactive-review tasks:

1. Complete required automated validation.
2. Start the required backend and frontend processes locally.
3. Initialize a deterministic, reproducible state appropriate for the feature.
4. Perform one internal browser pass and fix obvious layout, overflow, console, and interaction problems.
5. Provide the local URL to the user.
6. Keep development processes running while the user reviews the application when practical.
7. Let the user play through the affected flow from the appropriate starting state.
8. Fix reported issues and test the affected interaction directly.
9. Reuse the same deterministic review state across iterations when practical.
10. Do not restart servers, regenerate state, or replay unrelated flows unless the change requires it.
11. Wait for explicit user approval before opening the pull request.

When practical, provide deterministic review states for start of run, specific room types, combat, rewards, victory, and defeat.

Review states must not bypass or duplicate authoritative gameplay rules. Prefer seeded game state or development-only setup mechanisms that exercise the real application flow.

Screenshots may supplement interactive review, especially for responsive layouts, but do not replace playable review when the feature can reasonably be exercised locally.

Small non-visual frontend refactors with no presentation or gameplay-flow impact do not require interactive approval unless the issue says otherwise.

## GitHub Task and PR Rules

Prefer one GitHub Issue per Codex implementation session. Start a new session for the next issue when practical.

When starting work:

- assign the issue to the current developer when GitHub access allows it
- create a branch using the repository naming rules
- associate the branch and PR with the issue when supported

Branch naming:

- `feature/<issue-number>-<short-description>`
- `bugfix/<issue-number>-<short-description>`
- `chore/<issue-number>-<short-description>`
- `docs/<issue-number>-<short-description>`

Do not use `codex` as a branch name or prefix.

Before opening a PR:

- required validation must pass
- the final diff must be reviewed
- all acceptance criteria must be verified
- required interactive approval must be complete

The PR should:

- target `main`
- include `Closes #<ISSUE_NUMBER>` when it fully completes the issue
- summarize implementation, tests, validation, and important design decisions or limitations
- avoid unrelated changes

Human review remains the final approval step. Do not automatically merge solely because CI passes.

## Generated Project State

Do not manually modify generated project-state documentation if such files are introduced.

GitHub Issues and milestones are the authoritative source for task completion state.