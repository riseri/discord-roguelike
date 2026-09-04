# Development

## Purpose

This document describes how work moves from an idea to merged code.

Detailed implementation requirements belong in GitHub Issues.

Permanent Codex implementation rules belong in `AGENTS.md`.

## Sources of Truth

Use:

- `docs/MVP.md` for product and MVP scope
- `docs/ARCHITECTURE.md` for architecture and module ownership
- `docs/COMBAT.md` for combat rules
- `AGENTS.md` for Codex implementation, validation, review, branch, and PR rules
- GitHub Issues for task requirements
- GitHub Issues and milestones for task status
- tests and implementation for actual code behavior

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

If implementation reveals that an issue is incomplete or incorrectly scoped, update the issue rather than silently expanding the pull request.

If a newly discovered requirement is independent, prefer creating a new issue.

## Milestones

Milestones group related issues into meaningful development outcomes.

GitHub Issues and milestones are the authoritative project-status tracker.

Do not maintain a parallel checkbox backlog in Markdown.

## Development Flow

The expected workflow is:

    GitHub Issue
        |
        v
    Codex implementation
        |
        v
    Focused validation while iterating
        |
        v
    Final diff review and required validation
        |
        v
    Interactive user review when required
        |
        v
    Pull Request
        |
        v
    CI
        |
        v
    Human review
        |
        v
    Merge
        |
        v
    Issue closed

Codex implementation behavior for this flow is defined in `AGENTS.md`.

## Iteration vs Final Validation

During implementation, Codex should use narrow, relevant checks for fast feedback.

Examples include:

- one `game-core` test class
- one `game-server` integration test
- tests for an affected Gradle module
- targeted frontend checks when frontend code is affected

Full repository validation is a completion gate, not the default feedback loop after every code edit.

The authoritative validation requirements and review-efficiency rules are defined in `AGENTS.md`.

## Interactive Review

Presentation and gameplay-flow work may require the user to play the running application before a PR is opened.

The interactive review gate, deterministic review-state requirements, and iteration rules are defined in `AGENTS.md`.

Interactive review is intended to validate the actual experience. Screenshots may supplement it but are not the primary approval mechanism when the flow can reasonably be played locally.

## Standard Codex Prompt Template

Use this as the default prompt for GitHub Issue work:

    Implement GitHub issue #<ISSUE_NUMBER>.

    Read and follow `AGENTS.md`, `docs/DEVELOPMENT.md`, and all documentation referenced by the issue.

    Stay within the issue scope and satisfy all acceptance criteria.

    Use focused validation while iterating, then complete the required final validation and review the final diff.

    If interactive review is required, stop before PR creation and let me review the running application.

    Otherwise, push the branch and open a pull request against `main` that closes the issue.

Task details should not be duplicated in the prompt because the GitHub Issue remains the authoritative task specification.

For a very small, well-scoped task, this shorter form is acceptable:

    Implement GitHub issue #<ISSUE_NUMBER>.

    Read and follow `AGENTS.md`, `docs/DEVELOPMENT.md`, and all documentation referenced by the issue.

    Stay within the issue scope and satisfy all acceptance criteria.

    Follow the validation and review workflow in `AGENTS.md`, then open a pull request against `main` that closes the issue.

## Pull Requests and CI

`AGENTS.md` defines Codex rules for branches, PR creation, validation, interactive approval, and issue linkage.

CI must pass before merging.

Human review remains the final approval step.

Do not automatically merge Codex-generated pull requests solely because CI passes.

## Documentation Changes

Update documentation when implementation intentionally changes architecture, game rules, MVP scope, or development workflow.

Do not modify documentation solely to make an implementation appear compliant.