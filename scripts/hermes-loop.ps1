param(
    [int]$MaxTasksPerRun = 0,
    [int]$MaxMinutesPerRun = 0,
    [string]$AllowCommits = "",
    [string]$AllowPush = "",
    [string]$FullBuildEveryRun = ""
)

$ErrorActionPreference = "Stop"

if ($MaxTasksPerRun -le 0) { if ($env:HERMES_MAX_TASKS_PER_RUN) { $MaxTasksPerRun = [int]$env:HERMES_MAX_TASKS_PER_RUN } else { $MaxTasksPerRun = 1 } }
if ($MaxMinutesPerRun -le 0) { if ($env:HERMES_MAX_MINUTES_PER_RUN) { $MaxMinutesPerRun = [int]$env:HERMES_MAX_MINUTES_PER_RUN } else { $MaxMinutesPerRun = 45 } }
if ([string]::IsNullOrWhiteSpace($AllowCommits)) { if ($env:HERMES_ALLOW_COMMITS) { $AllowCommits = $env:HERMES_ALLOW_COMMITS } else { $AllowCommits = "true" } }
if ([string]::IsNullOrWhiteSpace($AllowPush)) { if ($env:HERMES_ALLOW_PUSH) { $AllowPush = $env:HERMES_ALLOW_PUSH } else { $AllowPush = "false" } }
if ([string]::IsNullOrWhiteSpace($FullBuildEveryRun)) { if ($env:HERMES_FULL_BUILD_EVERY_RUN) { $FullBuildEveryRun = $env:HERMES_FULL_BUILD_EVERY_RUN } else { $FullBuildEveryRun = "true" } }

$AllowCommitsBool = [System.Convert]::ToBoolean($AllowCommits)
$AllowPushBool = [System.Convert]::ToBoolean($AllowPush)
$FullBuildEveryRunBool = [System.Convert]::ToBoolean($FullBuildEveryRun)

$Repo = Split-Path -Parent $PSScriptRoot
Set-Location $Repo
$Deadline = (Get-Date).AddMinutes($MaxMinutesPerRun)
$RunLog = Join-Path $Repo "docs/hermes/RUN_LOG.md"

function Write-RunLog([string]$Message) {
    Add-Content -Path $RunLog -Value $Message
}

function Assert-CleanOrStop([string]$Reason) {
    $status = git status --porcelain
    if ($status) {
        Write-RunLog ""
        Write-RunLog ("- STOP: " + $Reason + "; repo has uncommitted changes.")
        Write-RunLog '```'
        Write-RunLog ($status -join "`n")
        Write-RunLog '```'
        exit 1
    }
}

Write-RunLog ""
Write-RunLog ("## " + (Get-Date -Format s) + " - bounded loop start")
Write-RunLog ("- Config: maxTasks=" + $MaxTasksPerRun + " maxMinutes=" + $MaxMinutesPerRun + " allowCommits=" + $AllowCommitsBool + " allowPush=" + $AllowPushBool + " fullBuild=" + $FullBuildEveryRunBool)

Assert-CleanOrStop "pre-pull safety check"
git fetch --all --prune
$upstream = git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>$null
if ($LASTEXITCODE -eq 0 -and $upstream) {
    $behind = git rev-list --count HEAD..'@{u}'
    if ([int]$behind -gt 0) {
        git pull --ff-only
    }
}
Assert-CleanOrStop "post-pull safety check"

$tasksCompleted = 0
while ($tasksCompleted -lt $MaxTasksPerRun -and (Get-Date) -lt $Deadline) {
    $backlog = Get-Content "docs/hermes/BACKLOG.md"
    $taskLine = $backlog | Select-String -Pattern '^### HX-[0-9]+' | Select-Object -First 1
    if (-not $taskLine) {
        Write-RunLog "- STOP: no backlog task found."
        break
    }
    $taskId = ($taskLine.Matches[0].Value -replace '^### ', '').Trim()
    $slug = $taskId.ToLowerInvariant()
    $branch = "hermes/" + $slug

    git switch main
    git switch -c $branch 2>$null
    if ($LASTEXITCODE -ne 0) { git switch $branch }

    $prompt = "You are Hermes running a bounded Hydroxide autonomous task. Repo: " + $Repo + ". Read docs/hermes/BACKLOG.md and implement exactly one small safe slice for " + $taskId + ". Follow TDD where practical. Preserve existing architecture. Do not push. Run focused tests and full verification if production code changes. Update docs/hermes/RUN_LOG.md. Commit only if tests pass and HERMES_ALLOW_COMMITS=" + $AllowCommitsBool + "."
    hermes chat -q $prompt

    if ($FullBuildEveryRunBool) {
        $VerifyScript = Join-Path $PSScriptRoot "hermes-verify.ps1"
        & $VerifyScript
    }

    $hasWorkingChanges = $false
    git diff --quiet
    if ($LASTEXITCODE -ne 0) { $hasWorkingChanges = $true }
    git diff --cached --quiet
    if ($LASTEXITCODE -ne 0) { $hasWorkingChanges = $true }

    if ($AllowCommitsBool -and $hasWorkingChanges) {
        git add -A
        git diff --cached --check
        git commit -m ("chore: autonomous " + $taskId)
    }
    if ($AllowPushBool) {
        git push -u origin $branch
    }
    $tasksCompleted++
}

Write-RunLog ("- Completed tasks this run: " + $tasksCompleted)
