package net.axther.hydroxide.modules.welcome;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.registry.ModernRegistryKeys;
import net.axther.hydroxide.storage.StoredLocation;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class WelcomeModule implements HydroModule, Listener, PlayerVisualStateService {

    private static final String FIREWORK_TAG = "hydroxide_welcome_firework";

    private HydroxideContext context;
    private YamlStore store;
    private final Map<UUID, FreezeState> frozen = new HashMap<>();
    private final Map<UUID, List<BukkitTask>> introTasks = new HashMap<>();

    @Override
    public String id() {
        return "welcome";
    }

    @Override
    public String displayName() {
        return "Cinematic Welcome";
    }

    @Override
    public String description() {
        return "Animated join titles, sound cues, cinematic camera lock, and safe welcome fireworks.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "welcome.yml"));
        seedDefaults();
        context.services().playerVisualStateService(this);
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        restoreAllIntroStates();
        frozen.clear();
        introTasks.clear();
        context.services().clearPlayerVisualStateService(this);
    }

    @Override
    public void onReload(HydroxideContext context) {
        restoreAllIntroStates();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        YamlConfiguration yaml = store.load();
        Player player = event.getPlayer();
        if (!isIntroEnabled(yaml)) {
            debug("intro disabled skip for " + player.getName());
            restoreIntroState(player);
            return;
        }
        trackTask(player, Bukkit.getScheduler().runTaskLater(context.plugin(), guarded(player, () -> play(player, yaml)), 10L));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        restoreIntroState(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        FreezeState state = frozen.get(event.getPlayer().getUniqueId());
        if (state != null && event.getTo() != null && !sameBlock(event.getFrom(), event.getTo())) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework firework && firework.getScoreboardTags().contains(FIREWORK_TAG)) {
            event.setCancelled(true);
        }
    }

    private void play(Player player, YamlConfiguration yaml) {
        if (!player.isOnline() || !isIntroEnabled(yaml)) {
            debug("intro disabled skip for " + player.getName());
            restoreIntroState(player);
            return;
        }
        debug("intro start for " + player.getName());
        long durationTicks = yaml.getLong("welcome-screen.cinematic-anchor.duration-ticks", 100L);
        if (yaml.getBoolean("welcome-screen.cinematic-anchor.enabled", true)) {
            freezeAtAnchor(player, yaml);
        }

        WelcomeFrameSequence sequence = loadSequence(yaml);
        for (int index = 0; index < sequence.frameCount(); index++) {
            WelcomeFrameSequence.Frame frame = sequence.frames().get(index);
            trackTask(player, Bukkit.getScheduler().runTaskLater(context.plugin(), guarded(player, () -> {
                if (!player.isOnline() || !isIntroEnabled(store.load())) {
                    debug("intro disabled skip for " + player.getName());
                    restoreIntroState(player);
                    return;
                }
                player.showTitle(Title.title(
                    context.text().format(frame.title().replace("{player}", player.getName())),
                    context.text().format(frame.subtitle().replace("{player}", player.getName())),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(sequence.frameIntervalTicks() * 50L + 250L), Duration.ZERO)
                ));
            }), (long) index * sequence.frameIntervalTicks()));
        }
        playSounds(player, yaml);
        playFireworks(player, yaml);

        long restoreAfter = Math.max(durationTicks, sequence.durationTicks() + sequence.frameIntervalTicks());
        trackTask(player, Bukkit.getScheduler().runTaskLater(context.plugin(), guarded(player, () -> {
            restoreIntroState(player);
            context.spawns().get("spawn").flatMap(this::toLocation).ifPresent(player::teleportAsync);
        }), restoreAfter));
    }

    private void freezeAtAnchor(Player player, YamlConfiguration yaml) {
        FreezeState state = new FreezeState(
                player.getLocation(),
                player.isInvulnerable(),
                player.isInvisible(),
                player.getWalkSpeed(),
                player.getFlySpeed(),
                new HashSet<>()
        );
        frozen.put(player.getUniqueId(), state);
        player.setInvulnerable(true);
        player.setInvisible(true);
        addTrackedPotionEffect(player, state, PotionEffectType.BLINDNESS, 120);
        parseLocation(yaml.getString("welcome-screen.cinematic-anchor.anchor-coords"))
                .ifPresent(location -> player.teleportAsync(location));
    }

    @Override
    public void restoreIntroState(Player player) {
        boolean hadIntroTasks = cancelIntroTasks(player.getUniqueId());
        FreezeState state = frozen.remove(player.getUniqueId());
        if (state != null) {
            player.setInvulnerable(state.invulnerable());
            player.setInvisible(state.invisible());
            player.setWalkSpeed(state.walkSpeed());
            player.setFlySpeed(state.flySpeed());
            for (PotionEffectType effectType : state.appliedEffects()) {
                player.removePotionEffect(effectType);
            }
        }
        if (state != null || hadIntroTasks) {
            debug("intro cleanup for " + player.getName());
            player.clearTitle();
            player.sendActionBar(Component.empty());
        }
        ensureVisibleUnlessVanished(player);
    }

    private WelcomeFrameSequence loadSequence(YamlConfiguration yaml) {
        int interval = yaml.getInt("welcome-screen.animation.frame-interval-ticks", 5);
        boolean loop = yaml.getBoolean("welcome-screen.animation.loop", false);
        List<WelcomeFrameSequence.Frame> frames = new ArrayList<>();
        for (Map<?, ?> item : yaml.getMapList("welcome-screen.animation.frames")) {
            frames.add(new WelcomeFrameSequence.Frame(
                    String.valueOf(item.get("title") == null ? "<#44CCFF>Welcome" : item.get("title")),
                    String.valueOf(item.get("subtitle") == null ? "<gray>Enjoy your stay." : item.get("subtitle"))
            ));
        }
        if (frames.isEmpty()) {
            frames.add(new WelcomeFrameSequence.Frame("<#44CCFF><bold>Welcome</bold>", "<gray>Enjoy your stay, <white>{player}<gray>!"));
        }
        return new WelcomeFrameSequence(frames, interval, loop);
    }

    private void playSounds(Player player, YamlConfiguration yaml) {
        for (Map<?, ?> item : yaml.getMapList("welcome-screen.sounds")) {
            Sound sound = parseSound(String.valueOf(item.get("sound") == null ? "BLOCK_NOTE_BLOCK_CHIME" : item.get("sound"))).orElse(null);
            if (sound == null) {
                continue;
            }
            float pitch = (float) parseDouble(String.valueOf(item.get("pitch") == null ? "1.0" : item.get("pitch")), 1.0D);
            long delay = (long) parseDouble(String.valueOf(item.get("delay-ticks") == null ? "0" : item.get("delay-ticks")), 0.0D);
            trackTask(player, Bukkit.getScheduler().runTaskLater(context.plugin(), guarded(player, () -> {
                if (player.isOnline() && isIntroEnabled(store.load())) {
                    player.playSound(player.getLocation(), sound, 0.8f, pitch);
                }
            }), Math.max(0L, delay)));
        }
    }

    private void playFireworks(Player player, YamlConfiguration yaml) {
        if (!isIntroEnabled(yaml) || !yaml.getBoolean("welcome-screen.fireworks.enabled", true)) {
            return;
        }
        int amount = Math.max(1, yaml.getInt("welcome-screen.fireworks.amount", 3));
        long delay = Math.max(1L, yaml.getLong("welcome-screen.fireworks.delay-ticks", 15L));
        for (int index = 0; index < amount; index++) {
            trackTask(player, Bukkit.getScheduler().runTaskLater(context.plugin(), guarded(player, () -> {
                if (player.isOnline() && isIntroEnabled(store.load())) {
                    spawnFirework(player, yaml);
                }
            }), delay * index));
        }
    }

    private void spawnFirework(Player player, YamlConfiguration yaml) {
        if (!player.isOnline()) {
            return;
        }
        Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation().add(0.0D, 2.5D, 0.0D), EntityType.FIREWORK_ROCKET);
        firework.addScoreboardTag(FIREWORK_TAG);
        FireworkMeta meta = firework.getFireworkMeta();
        ConfigurationSection properties = yaml.getConfigurationSection("welcome-screen.fireworks.properties");
        FireworkEffect.Type type = parseFireworkType(properties == null ? "BALL" : properties.getString("type", "BALL"));
        meta.addEffect(FireworkEffect.builder()
                .with(type)
                .flicker(properties != null && properties.getBoolean("flicker", true))
                .trail(properties != null && properties.getBoolean("trail", true))
                .withColor(colors(properties == null ? List.of("#44CCFF") : properties.getStringList("colors")))
                .withFade(colors(properties == null ? List.of("#FFFFFF") : properties.getStringList("fade-colors")))
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
        trackTask(player, Bukkit.getScheduler().runTaskLater(context.plugin(), guarded(player, firework::detonate), 2L));
    }

    private List<Color> colors(List<String> values) {
        List<Color> colors = new ArrayList<>();
        for (String value : values) {
            try {
                colors.add(Color.fromRGB(Integer.parseInt(value.replace("#", ""), 16)));
            } catch (NumberFormatException ignored) {
                // Ignore malformed colors and keep the rest of the configured effect.
            }
        }
        return colors.isEmpty() ? List.of(Color.AQUA) : colors;
    }

    private Optional<Location> parseLocation(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String[] parts = input.split(",");
        if (parts.length < 4) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(parts[0].trim());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(
                world,
                parseDouble(parts[1], 0.0D),
                parseDouble(parts[2], world.getSpawnLocation().getY()),
                parseDouble(parts[3], 0.0D),
                parts.length > 4 ? (float) parseDouble(parts[4], 0.0D) : 0.0f,
                parts.length > 5 ? (float) parseDouble(parts[5], 0.0D) : 0.0f
        ));
    }

    private Optional<Location> toLocation(StoredLocation storedLocation) {
        World world = Bukkit.getWorld(storedLocation.worldName());
        return world == null ? Optional.empty() : Optional.of(storedLocation.toLocation(world));
    }

    private Optional<Sound> parseSound(String input) {
        for (String key : ModernRegistryKeys.soundKeys(input)) {
            Sound sound = Registry.SOUND_EVENT.get(NamespacedKey.minecraft(key));
            if (sound != null) {
                return Optional.of(sound);
            }
        }
        return Optional.empty();
    }

    private FireworkEffect.Type parseFireworkType(String input) {
        String normalized = input == null ? "BALL" : input.toUpperCase(Locale.ROOT);
        if (normalized.equals("LARGE_BALL")) {
            normalized = "BALL_LARGE";
        }
        try {
            return FireworkEffect.Type.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return FireworkEffect.Type.BALL;
        }
    }

    private double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input.trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() == second.getWorld()
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private void seedDefaults() {
        YamlConfiguration yaml = store.load();
        if (yaml.contains("welcome-screen")) {
            return;
        }
        yaml.set("welcome.enabled", true);
        yaml.set("welcome.intro.enabled", true);
        yaml.set("welcome-screen.enabled", true);
        yaml.set("welcome-screen.cinematic-anchor.enabled", true);
        yaml.set("welcome-screen.cinematic-anchor.duration-ticks", 100);
        yaml.set("welcome-screen.cinematic-anchor.anchor-coords", "world, 0.5, 85.0, 0.5, 90.0, 45.0");
        yaml.set("welcome-screen.animation.frame-interval-ticks", 5);
        yaml.set("welcome-screen.animation.loop", false);
        List<Map<String, String>> frames = List.of(
                Map.of("title", "<gradient:#FF5555:#FF55FF>W</gradient>", "subtitle", "<gray>Welcome to the server</gray>"),
                Map.of("title", "<gradient:#FF5555:#FF55FF>We</gradient>", "subtitle", "<gray>Welcome to the server</gray>"),
                Map.of("title", "<gradient:#FF5555:#FF55FF>Wel</gradient>", "subtitle", "<gray>Welcome to the server</gray>"),
                Map.of("title", "<gradient:#FF5555:#FF55FF>Welc</gradient>", "subtitle", "<gray>Enjoy your stay!</gray>"),
                Map.of("title", "<gradient:#FF5555:#FF55FF>Welcome!</gradient>", "subtitle", "<green>Enjoy your stay!</green>")
        );
        yaml.set("welcome-screen.animation.frames", frames);
        yaml.set("welcome-screen.sounds", List.of(
                Map.of("sound", "BLOCK_NOTE_BLOCK_CHIME", "pitch", 0.8D, "delay-ticks", 10),
                Map.of("sound", "BLOCK_NOTE_BLOCK_CHIME", "pitch", 1.2D, "delay-ticks", 20)
        ));
        yaml.set("welcome-screen.fireworks.enabled", true);
        yaml.set("welcome-screen.fireworks.amount", 3);
        yaml.set("welcome-screen.fireworks.delay-ticks", 15);
        yaml.set("welcome-screen.fireworks.properties.type", "CREEPER");
        yaml.set("welcome-screen.fireworks.properties.flicker", true);
        yaml.set("welcome-screen.fireworks.properties.trail", true);
        yaml.set("welcome-screen.fireworks.properties.colors", List.of("#FF5555", "#55FF55"));
        yaml.set("welcome-screen.fireworks.properties.fade-colors", List.of("#FFFFFF"));
        store.save(yaml);
    }

    private boolean isIntroEnabled(YamlConfiguration yaml) {
        return WelcomeIntroSettings.from(yaml).introEnabled();
    }

    private void addTrackedPotionEffect(Player player, FreezeState state, PotionEffectType effectType, int durationTicks) {
        if (player.hasPotionEffect(effectType)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(effectType, durationTicks, 0, false, false, false));
        state.appliedEffects().add(effectType);
    }

    private void trackTask(Player player, BukkitTask task) {
        introTasks.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayList<>()).add(task);
    }

    private boolean cancelIntroTasks(UUID playerId) {
        List<BukkitTask> tasks = introTasks.remove(playerId);
        if (tasks == null) {
            return false;
        }
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        return true;
    }

    private void restoreAllIntroStates() {
        java.util.HashSet<UUID> playerIds = new java.util.HashSet<>();
        playerIds.addAll(frozen.keySet());
        playerIds.addAll(introTasks.keySet());
        playerIds.stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null)
                .forEach(this::restoreIntroState);
    }

    private void ensureVisibleUnlessVanished(Player player) {
        boolean vanished = context.services().vanishService()
                .map(service -> service.isVanished(player.getUniqueId()))
                .orElse(false);
        if (vanished) {
            context.services().vanishService().ifPresent(service -> service.reconcileVisibility(player));
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(context.plugin(), player);
        }
    }

    private Runnable guarded(Player player, Runnable action) {
        return () -> {
            try {
                action.run();
            } catch (RuntimeException exception) {
                restoreIntroState(player);
                context.plugin().getLogger().warning("Welcome intro task failed for " + player.getName() + ": " + exception.getMessage());
            }
        };
    }

    private void debug(String message) {
        if (context != null && context.plugin().getConfig().getBoolean("debug.visibility", false)) {
            context.plugin().getLogger().info("[visibility] " + message);
        }
    }

    private record FreezeState(
            Location original,
            boolean invulnerable,
            boolean invisible,
            float walkSpeed,
            float flySpeed,
            java.util.Set<PotionEffectType> appliedEffects
    ) {
    }
}
