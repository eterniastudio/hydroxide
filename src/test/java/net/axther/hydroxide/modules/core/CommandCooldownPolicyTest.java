package net.axther.hydroxide.modules.core;

import org.junit.jupiter.api.Test;

import org.bukkit.configuration.file.YamlConfiguration;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandCooldownPolicyTest {

    @Test
    void matchesCommandLabelIgnoringSlashAndCase() {
        CommandCooldownPolicy policy = new CommandCooldownPolicy(Map.of("Spawn", Duration.ofSeconds(30)));

        assertEquals(Duration.ofSeconds(30), policy.cooldown("/spawn hub", noPermissions()).orElseThrow().duration());
        assertEquals(Duration.ofSeconds(30), policy.cooldown("SPAWN hub", noPermissions()).orElseThrow().duration());
    }

    @Test
    void prefersMostSpecificSubcommandCooldown() {
        CommandCooldownPolicy policy = new CommandCooldownPolicy(Map.of(
                "kit", Duration.ofSeconds(5),
                "kit-tools", Duration.ofSeconds(45)
        ));

        assertEquals(Duration.ofSeconds(45), policy.cooldown("/kit tools", noPermissions()).orElseThrow().duration());
        assertEquals(Duration.ofSeconds(5), policy.cooldown("/kit food", noPermissions()).orElseThrow().duration());
    }

    @Test
    void bypassPermissionSkipsCooldown() {
        CommandCooldownPolicy policy = new CommandCooldownPolicy(Map.of("home", Duration.ofSeconds(10)));

        assertTrue(policy.cooldown("/home base", permission -> permission.equals("hydroxide.command.cooldown.bypass")).isEmpty());
    }

    @Test
    void configValueMinusOneCreatesOneUseCooldown() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("starter", -1);

        CommandCooldownPolicy.Cooldown cooldown = CommandCooldownPolicy.from(yaml).cooldown("/starter", noPermissions()).orElseThrow();

        assertTrue(cooldown.oneUse());
    }

    @Test
    void trackerReturnsRemainingTimeUntilReady() {
        UUID playerId = UUID.randomUUID();
        CommandCooldownTracker tracker = new CommandCooldownTracker();
        CommandCooldownPolicy.Cooldown cooldown = new CommandCooldownPolicy.Cooldown("spawn", Duration.ofSeconds(10));

        assertTrue(tracker.checkAndMark(playerId, cooldown, 1_000L).isEmpty());

        CommandCooldownTracker.ActiveCooldown active = tracker.checkAndMark(playerId, cooldown, 6_000L).orElseThrow();
        assertEquals("spawn", active.key());
        assertEquals(Duration.ofSeconds(5), active.remaining());
        assertTrue(!active.oneUse());

        assertTrue(tracker.checkAndMark(playerId, cooldown, 11_000L).isEmpty());
    }

    @Test
    void trackerMarksOneUseCooldownPermanently() {
        UUID playerId = UUID.randomUUID();
        CommandCooldownTracker tracker = new CommandCooldownTracker();
        CommandCooldownPolicy.Cooldown cooldown = CommandCooldownPolicy.Cooldown.oneUse("starter");

        assertTrue(tracker.checkAndMark(playerId, cooldown, 1_000L).isEmpty());

        CommandCooldownTracker.ActiveCooldown active = tracker.checkAndMark(playerId, cooldown, 1_000_000L).orElseThrow();
        assertEquals("starter", active.key());
        assertTrue(active.oneUse());
        assertEquals(Duration.ZERO, active.remaining());
        assertEquals(java.util.List.of(new CommandCooldownTracker.Entry(playerId, "starter", Long.MAX_VALUE)),
                tracker.snapshot(1_000_000L));
    }

    @Test
    void trackerSnapshotsAndRestoresActiveCooldownsOnly() {
        UUID playerId = UUID.randomUUID();
        CommandCooldownTracker tracker = new CommandCooldownTracker();
        tracker.restore(java.util.List.of(
                new CommandCooldownTracker.Entry(playerId, "spawn", 12_000L),
                new CommandCooldownTracker.Entry(playerId, "home", 2_000L)
        ), 5_000L);

        assertEquals(java.util.List.of(new CommandCooldownTracker.Entry(playerId, "spawn", 12_000L)),
                tracker.snapshot(5_000L));

        CommandCooldownPolicy.Cooldown cooldown = new CommandCooldownPolicy.Cooldown("spawn", Duration.ofSeconds(10));
        CommandCooldownTracker.ActiveCooldown active = tracker.checkAndMark(playerId, cooldown, 7_000L).orElseThrow();
        assertEquals(Duration.ofSeconds(5), active.remaining());
    }

    @Test
    void trackerClearsCooldownsForOnePlayerAndOptionalCommand() {
        UUID alex = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID steve = UUID.fromString("00000000-0000-0000-0000-000000000002");
        CommandCooldownTracker tracker = new CommandCooldownTracker();
        tracker.restore(java.util.List.of(
                new CommandCooldownTracker.Entry(alex, "spawn", 12_000L),
                new CommandCooldownTracker.Entry(alex, "home", Long.MAX_VALUE),
                new CommandCooldownTracker.Entry(steve, "spawn", 12_000L)
        ), 5_000L);

        assertEquals(1, tracker.clear(alex, "spawn"));
        assertEquals(java.util.List.of(
                new CommandCooldownTracker.Entry(alex, "home", Long.MAX_VALUE),
                new CommandCooldownTracker.Entry(steve, "spawn", 12_000L)
        ), tracker.snapshot(5_000L));

        assertEquals(1, tracker.clear(alex, ""));
        assertEquals(java.util.List.of(new CommandCooldownTracker.Entry(steve, "spawn", 12_000L)),
                tracker.snapshot(5_000L));
    }

    @Test
    void trackerClearsAllCooldownsAndCountsOnlyActiveEntries() {
        UUID alex = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID steve = UUID.fromString("00000000-0000-0000-0000-000000000002");
        CommandCooldownTracker tracker = new CommandCooldownTracker();
        tracker.restore(java.util.List.of(
                new CommandCooldownTracker.Entry(alex, "spawn", 12_000L),
                new CommandCooldownTracker.Entry(alex, "home", Long.MAX_VALUE),
                new CommandCooldownTracker.Entry(steve, "spawn", 3_000L),
                new CommandCooldownTracker.Entry(steve, "home", 15_000L)
        ), 5_000L);

        assertEquals(3, tracker.activeCount(5_000L));
        assertEquals(1, tracker.clearAll("spawn"));
        assertEquals(2, tracker.activeCount(5_000L));
        assertEquals(2, tracker.clearAll(""));
        assertEquals(0, tracker.activeCount(5_000L));
    }

    private Predicate<String> noPermissions() {
        return permission -> false;
    }
}
