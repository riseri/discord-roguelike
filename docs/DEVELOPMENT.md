# Development

## Purpose

This document describes how work moves from an idea to merged code.

Detailed implementation requirements belong in GitHub Issues.

Permanent coding-agent rules belong in `AGENTS.md`.

## Sources of Truth

Use the following sources for different kinds of information:

- Product and MVP scope -> `docs/MVP.md`
- Architecture and module ownership -> `docs/ARCHITECTURE.md`
- Combat rules -> `docs/COMBAT.md`
- Agent behavior and repository rules -> `AGENTS.md`
- Task requirements -> GitHub Issues
- Task status -> GitHub Issues and milestones
- Code behavior -> tests and implementation

Do not maintain duplicate task-completion state in Markdown files.

## GitHub Issues

Every implementation task should normally correspond to one GitHub Issue.

Issues should contain:

- Goal
- Context
- Requirements
- Acceptance Criteria
- Out of Scope

Acceptance criteria define when the issue is complete.

## Milestones

Milestones group related issues into meaningful development goals.

Example:

    M1 - Playable Encounter

A milestone describes a development outcome rather than an individual code change.

## Development Flow

The expected workflow is:

    GitHub Issue
        |
        v
    Codex implementation
        |
        v
    Feature or bugfix branch
        |
        v
    Validation
        |
        v
    Visual review (when UI presentation changes)
        |
        v
    User approval (when visual review is required)
        |
        v
    Pull Request
        |
        v
    CI
        |
        v
    Review
        |
        v
    Merge
        |
        v
    Issue closed

## Codex Tasks

Prefer one GitHub Issue per Codex implementation session.

Start a new Codex session for the next issue when practical.

Codex should:

1. Read `AGENTS.md`.
2. Read documentation referenced by the issue.
3. Review relevant existing implementation.
4. Create a branch following repository naming rules.
5. Implement only the issue scope.
6. Add or update appropriate tests.
7. Run required validation.
8. Review the diff for unrelated changes.
9. If the task materially changes UI presentation, complete the visual review workflow and wait for user approval.
10. Push the branch.
11. Open a pull request against `main`.

## Visual Review Workflow

UI-affecting work is not considered ready for a pull request solely because linting, builds, or automated tests pass.

For tasks that materially change `activity-client` presentation, Codex must:

1. Start the backend and frontend locally as needed for the affected flow.
2. Open the application in a browser.
3. Exercise the changed flow from start to finish.
4. Capture screenshots that show each materially changed state needed for review.
5. Review those screenshots for obvious issues in layout, spacing, hierarchy, readability, overflow, contrast, responsive behavior, and consistency with the issue's design direction.
6. Iterate on obvious visual problems before presenting the work.
7. Show the final screenshots to the user.
8. Wait for explicit user approval.
9. Only after approval, push the final branch state if needed and open the pull request.

Examples of work that normally requires this gate:

- combat-screen redesigns
- status/HUD changes
- new menus or dialogs
- responsive-layout changes
- animation or transition changes
- new user-facing screens
- significant styling/theme changes

Small non-visual frontend changes, such as internal refactors with no presentation impact, do not require screenshot approval unless the issue says otherwise.

## Standard Codex Prompt Template

Use the following as the default prompt for GitHub Issue work:

    Implement GitHub issue #<ISSUE_NUMBER>.

    Read and follow `AGENTS.md`, `docs/DEVELOPMENT.md`, and all documentation referenced by the issue.

    Treat the GitHub Issue as the task specification. Stay within its scope, satisfy every acceptance criterion, and do not implement behavior listed under Out of Scope.

    Before changing code:
    - inspect the relevant existing implementation
    - identify the module that owns the behavior
    - call out any conflict between the issue, AGENTS.md, documentation, and existing architecture instead of silently choosing one

    During implementation:
    - follow the repository architecture and dependency direction
    - keep gameplay rules authoritative in `game-core`
    - avoid unnecessary abstractions or unrelated refactors
    - add or update focused tests for the behavior being changed
    - add comments only for non-obvious intent, constraints, ordering, invariants, or architectural reasoning

    Before considering the task complete:
    - run all validation required by `AGENTS.md` for every module touched
    - review the diff for unrelated changes
    - verify every acceptance criterion explicitly

    If the task materially changes `activity-client` presentation:
    - start the required backend and frontend processes locally
    - open the affected flow in a browser
    - exercise the changed interaction from start to finish
    - capture screenshots of each materially changed state needed for review
    - inspect the screenshots for layout, spacing, hierarchy, readability, overflow, contrast, responsive behavior, and consistency with the issue's design direction
    - iterate on obvious visual problems before presenting the result
    - show me the final screenshots and wait for my explicit approval
    - do not open the pull request before I approve the visual result

    If visual approval is not required, or after I approve the final UI when it is required:
    - push the implementation branch
    - open a pull request against `main`
    - include `Closes #<ISSUE_NUMBER>` in the PR description
    - summarize the implementation, tests, validation performed, and any important design decisions or limitations

Task details should not be duplicated in the prompt because the GitHub Issue remains the authoritative task specification.

For a very small non-UI task, the shortened form is acceptable:

    Implement GitHub issue #<ISSUE_NUMBER>.

    Read and follow `AGENTS.md`, `docs/DEVELOPMENT.md`, and all documentation referenced by the issue.

    Stay within the issue scope and satisfy all acceptance criteria.

    Run the required validation, review the diff, push the branch, and open a pull request against `main` that includes `Closes #<ISSUE_NUMBER>`.

## Branches

Use:

    feature/<issue-number>-<short-description>
    bugfix/<issue-number>-<short-description>
    chore/<issue-number>-<short-description>
    docs/<issue-number>-<short-description>

Do not use `codex/` branches.

## Pull Requests

A pull request should:

- reference the GitHub Issue
- describe what changed
- identify tests added or updated
- state which validation commands were run
- mention important design decisions or limitations
- avoid unrelated changes

For UI-affecting work that requires visual review, the pull request should only be created after the user has approved the final browser screenshots.

Prefer:

    Closes #<ISSUE_NUMBER>

when the PR fully completes the issue.

## CI

CI must pass before merging.

Backend validation includes:

    ./gradlew test
    ./gradlew spotlessCheck

Frontend validation includes:

    npm run lint
    npm run build

## Merge Policy

Human review remains the final approval step.

Do not automatically merge Codex-generated pull requests solely because CI passes.

Review:

- acceptance criteria
- architecture
- unnecessary complexity
- unrelated changes
- tests
- comments and naming
- module boundaries
- final visual result for UI-affecting work

## Scope Changes

If implementation reveals that an issue is incomplete or incorrectly scoped, update the GitHub Issue rather than silently expanding the pull request.

If a newly discovered requirement is independent, prefer creating a new issue.

## Documentation Changes

Update documentation when implementation intentionally changes:

- architecture
- game rules
- MVP scope
- development workflow

Do not modify documentation solely to make an implementation appear compliant.

## Progress Tracking

GitHub Issues and milestones are the authoritative project-status tracker.

Do not maintain a parallel checkbox backlog in Markdown.

## GitHub Task Setup

When starting work on a GitHub Issue:

1. Assign the issue to the current repository owner or developer when GitHub access allows it.
2. Create the implementation branch using the repository branch naming rules.
3. Associate the branch and eventual pull request with the GitHub Issue when supported.
4. Ensure the pull request includes:

       Closes #<ISSUE_NUMBER>

Do not consider GitHub task setup a substitute for implementing the issue.