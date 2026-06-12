# Hydroxide Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first usable Hydroxide server-core plugin slice: modular runtime, modern text formatting, persistent location-backed utilities, and initial Essentials-style command modules.

**Architecture:** Hydroxide will be a Paper plugin with a small kernel (`HydroxideContext`, `HydroModule`, `ModuleManager`) and optional modules registered from the main plugin. Shared services handle Adventure text formatting, config access, command binding, player data, warps, spawn, teleport requests, and cooldown/back state.

**Tech Stack:** Java 21, Gradle, Paper API `1.21.11-R0.1-SNAPSHOT`, Adventure/MiniMessage from Paper, JUnit Jupiter for pure unit tests.

---

### Task 1: Test Harness

**Files:**
- Modify: `build.gradle`
- Create: `src/test/java/net/axther/hydroxide/text/TextFormatterTest.java`
- Create: `src/test/java/net/axther/hydroxide/modules/ModuleManagerTest.java`
- Create: `src/test/java/net/axther/hydroxide/storage/StoredLocationTest.java`

- [ ] **Step 1: Add JUnit test dependencies**

Add JUnit Jupiter and configure `useJUnitPlatform()` in Gradle.

- [ ] **Step 2: Write failing tests**

Tests assert that legacy ampersand codes, hex codes, MiniMessage tags, module enablement defaults, dependency ordering, and stored-location round trips behave correctly.

- [ ] **Step 3: Run tests to verify RED**

Run: `.\gradlew.bat test`

Expected: FAIL because implementation classes are missing.

### Task 2: Modern Text Service

**Files:**
- Create: `src/main/java/net/axther/hydroxide/text/TextFormatter.java`

- [ ] **Step 1: Implement formatting**

Support `&` legacy styles, `&#RRGGBB`, `<#RRGGBB>`, named MiniMessage tags, escaped null/blank handling, and plain-text serialization for tests.

- [ ] **Step 2: Run focused tests**

Run: `.\gradlew.bat test --tests net.axther.hydroxide.text.TextFormatterTest`

Expected: PASS.

### Task 3: Modular Runtime

**Files:**
- Create: `src/main/java/net/axther/hydroxide/modules/HydroModule.java`
- Create: `src/main/java/net/axther/hydroxide/modules/ModuleConfig.java`
- Create: `src/main/java/net/axther/hydroxide/modules/ModuleManager.java`

- [ ] **Step 1: Implement module interfaces and enablement**

Modules expose id, display name, description, dependency ids, default enabled state, lifecycle methods, and status.

- [ ] **Step 2: Run focused tests**

Run: `.\gradlew.bat test --tests net.axther.hydroxide.modules.ModuleManagerTest`

Expected: PASS.

### Task 4: Location Storage

**Files:**
- Create: `src/main/java/net/axther/hydroxide/storage/StoredLocation.java`
- Create: `src/main/java/net/axther/hydroxide/storage/YamlStore.java`
- Create: `src/main/java/net/axther/hydroxide/storage/PlayerDataStore.java`
- Create: `src/main/java/net/axther/hydroxide/storage/NamedLocationStore.java`

- [ ] **Step 1: Implement serializable locations**

Store world name, coordinates, yaw, and pitch in Bukkit configuration sections.

- [ ] **Step 2: Run focused tests**

Run: `.\gradlew.bat test --tests net.axther.hydroxide.storage.StoredLocationTest`

Expected: PASS.

### Task 5: Plugin Services And Modules

**Files:**
- Replace: `src/main/java/net/axther/hydroxide/Hydroxide.java`
- Create: `src/main/java/net/axther/hydroxide/HydroxideContext.java`
- Create: `src/main/java/net/axther/hydroxide/commands/*`
- Create: `src/main/java/net/axther/hydroxide/modules/core/*`
- Create: `src/main/java/net/axther/hydroxide/modules/chat/*`
- Create: `src/main/java/net/axther/hydroxide/modules/teleport/*`
- Create: `src/main/java/net/axther/hydroxide/modules/moderation/*`
- Modify: `src/main/resources/plugin.yml`
- Create: `src/main/resources/config.yml`

- [ ] **Step 1: Wire runtime services**

Initialize formatter, stores, command registrar, module manager, and default module list in `Hydroxide`.

- [ ] **Step 2: Add first modules**

Add core admin commands (`/hydroxide`), chat formatting, messaging/broadcasting, teleport homes/spawn/warps/tpa/back, and moderation utility commands.

- [ ] **Step 3: Verify build**

Run: `.\gradlew.bat build`

Expected: BUILD SUCCESSFUL.
