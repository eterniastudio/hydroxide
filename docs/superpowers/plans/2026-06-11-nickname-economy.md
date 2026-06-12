# Nickname And Economy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a modern nickname module and Vault-compatible economy module to Hydroxide.

**Architecture:** Refactor modules to the requested `onEnable(HydroxideContext)` / `onDisable(HydroxideContext)` lifecycle, then add two feature modules. Nicknames use `PlayerDataStore` persistence, in-memory caches, Paper component display APIs, and scoreboard team prefixes for above-head formatted names. Economy uses a `HydroEconomy` Vault provider backed by `PlayerDataStore` balances and an `EconomyModule` for commands and service registration.

**Tech Stack:** Java 21, Paper API 1.21.11, Adventure Components, VaultAPI from JitPack, JUnit Jupiter.

---

### Task 1: Behavior Tests

**Files:**
- Create: `src/test/java/net/axther/hydroxide/modules/nickname/NicknameServiceTest.java`
- Create: `src/test/java/net/axther/hydroxide/modules/economy/HydroEconomyTest.java`

- [ ] Write tests for nickname sanitization, formatting policy gates, cache lookup, and economy transactions.
- [ ] Run `.\gradlew.bat test --tests *NicknameServiceTest --tests *HydroEconomyTest` and verify RED.

### Task 2: Context-Based Module Lifecycle

**Files:**
- Modify: `src/main/java/net/axther/hydroxide/modules/HydroModule.java`
- Modify: `src/main/java/net/axther/hydroxide/modules/ModuleManager.java`
- Modify existing modules under `src/main/java/net/axther/hydroxide/modules`
- Modify: `src/main/java/net/axther/hydroxide/Hydroxide.java`
- Modify: `src/test/java/net/axther/hydroxide/modules/ModuleManagerTest.java`

- [ ] Replace `enable()` / `disable()` with `onEnable(HydroxideContext)` / `onDisable(HydroxideContext)`.
- [ ] Keep dependency ordering and reload behavior intact.

### Task 3: Player Data Store Extensions

**Files:**
- Modify: `src/main/java/net/axther/hydroxide/storage/PlayerDataStore.java`

- [ ] Add nickname `set/get/remove` methods.
- [ ] Add balance `get/set/deposit/withdraw` methods with non-negative validation.

### Task 4: Nickname Module

**Files:**
- Create: `src/main/java/net/axther/hydroxide/modules/nickname/NicknameService.java`
- Create: `src/main/java/net/axther/hydroxide/modules/nickname/NicknameModule.java`

- [ ] Cache nicknames on enable and joins.
- [ ] Apply display name, player list name, custom name, and scoreboard team prefix.
- [ ] Add `/nickname` and `/realname` with color, hex, gradient, and rainbow permission gates.

### Task 5: Vault Economy Module

**Files:**
- Create: `src/main/java/net/axther/hydroxide/modules/economy/HydroEconomy.java`
- Create: `src/main/java/net/axther/hydroxide/modules/economy/EconomyModule.java`

- [ ] Implement Vault `Economy` through `AbstractEconomy`.
- [ ] Register the provider at `ServicePriority.Highest` when Vault is present.
- [ ] Add `/balance`, `/pay`, and `/eco`.

### Task 6: Metadata And Verification

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/plugin.yml`
- Modify: `src/main/resources/config.yml`
- Modify: `README.md`

- [ ] Add JitPack and VaultAPI compile-only dependency.
- [ ] Add `softdepend: [Vault]`, commands, permissions, config defaults, and docs.
- [ ] Run `.\gradlew.bat clean build`.
