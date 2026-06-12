#!/usr/bin/env bash
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO"

HERMES_MAX_TASKS_PER_RUN="${HERMES_MAX_TASKS_PER_RUN:-1}"
HERMES_MAX_MINUTES_PER_RUN="${HERMES_MAX_MINUTES_PER_RUN:-45}"
HERMES_ALLOW_COMMITS="${HERMES_ALLOW_COMMITS:-true}"
HERMES_ALLOW_PUSH="${HERMES_ALLOW_PUSH:-true}"
HERMES_FULL_BUILD_EVERY_RUN="${HERMES_FULL_BUILD_EVERY_RUN:-true}"
RUN_LOG="$REPO/docs/hermes/RUN_LOG.md"
START_EPOCH="$(date +%s)"
DEADLINE_EPOCH="$((START_EPOCH + HERMES_MAX_MINUTES_PER_RUN * 60))"

log() { printf '%s\n' "$*" >> "$RUN_LOG"; }

assert_clean() {
  local reason="$1"
  if [ -n "$(git status --porcelain)" ]; then
    log ""
    log "- STOP: $reason; repo has uncommitted changes."
    log '```'
    git status --porcelain >> "$RUN_LOG"
    log '```'
    exit 1
  fi
}

log ""
log "## $(date -u +%Y-%m-%dT%H:%M:%SZ) — bounded loop start"
log "- Config: maxTasks=$HERMES_MAX_TASKS_PER_RUN maxMinutes=$HERMES_MAX_MINUTES_PER_RUN allowCommits=$HERMES_ALLOW_COMMITS allowPush=$HERMES_ALLOW_PUSH fullBuild=$HERMES_FULL_BUILD_EVERY_RUN"

assert_clean "pre-pull safety check"
git fetch --all --prune
if git rev-parse --abbrev-ref --symbolic-full-name '@{u}' >/dev/null 2>&1; then
  behind="$(git rev-list --count HEAD..@{u})"
  if [ "$behind" -gt 0 ]; then
    git pull --ff-only
  fi
fi
assert_clean "post-pull safety check"

tasks_completed=0
while [ "$tasks_completed" -lt "$HERMES_MAX_TASKS_PER_RUN" ] && [ "$(date +%s)" -lt "$DEADLINE_EPOCH" ]; do
  task_id="$(grep -m1 -E '^### HX-[0-9]+' docs/hermes/BACKLOG.md | sed 's/^### //')"
  if [ -z "$task_id" ]; then
    log "- STOP: no backlog task found."
    break
  fi
  slug="$(printf '%s' "$task_id" | tr '[:upper:]' '[:lower:]')"
  branch="hermes/$slug"

  git switch main
  git switch -c "$branch" 2>/dev/null || git switch "$branch"

  prompt="You are Hermes running a bounded Hydroxide autonomous task. Repo: $REPO. Read docs/hermes/BACKLOG.md and implement exactly one small safe slice for $task_id. Follow TDD where practical. Preserve existing architecture. Push only if HERMES_ALLOW_PUSH=true. Run focused tests and full verification if production code changes. Update docs/hermes/RUN_LOG.md. Commit only if tests pass and HERMES_ALLOW_COMMITS=$HERMES_ALLOW_COMMITS."
  hermes chat -q "$prompt"

  if [ "$HERMES_FULL_BUILD_EVERY_RUN" = "true" ]; then
    "$REPO/scripts/hermes-verify.sh"
  fi

  has_changes=false
  git diff --quiet || has_changes=true
  git diff --cached --quiet || has_changes=true
  if [ "$HERMES_ALLOW_COMMITS" = "true" ] && [ "$has_changes" = "true" ]; then
    git add -A
    git diff --cached --check
    git commit -m "chore: autonomous $task_id"
  fi
  if [ "$HERMES_ALLOW_PUSH" = "true" ]; then
    git push -u origin "$branch"
  fi
  tasks_completed="$((tasks_completed + 1))"
done

log "- Completed tasks this run: $tasks_completed"
