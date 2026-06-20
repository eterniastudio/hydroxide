package net.axther.hydroxide.modules.world;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.registry.ModernRegistryKeys;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class WorldManagerModule implements HydroModule {

    private static final List<String> SUBCOMMANDS = List.of("create", "delete", "list", "load", "setrule", "unload");
    private static final List<String> ENVIRONMENTS = List.of("normal", "nether", "end");

    private HydroxideContext context;
    private YamlStore store;
    private BukkitTask unloadChunksTask;

    @Override
    public String id() {
        return "worlds";
    }

    @Override
    public String displayName() {
        return "World Manager";
    }

    @Override
    public String description() {
        return "Native world create/load/unload/delete and YAML-backed per-world settings.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "worlds.yml"));
        context.commands().register("hydroworld", hydroworldCommand());
        context.commands().register("gamerule", gameruleCommand());
        context.commands().register("unloadchunks", unloadChunksCommand());
        loadConfiguredWorlds();
    }

    @Override
    public void onDisable(HydroxideContext context) {
        if (unloadChunksTask != null) {
            unloadChunksTask.cancel();
            unloadChunksTask = null;
        }
    }

    private CommandService hydroworldCommand() {
        return new CommandService(HydroCommand.builder("hydroworld")
                .permission("hydroxide.command.hydroworld")
                .usage("/{label} <create|load|unload|delete|setrule|list> ...")
                .executor(ctx -> handleCommand(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(ctx -> completions(ctx.arguments().toArray(String[]::new)))
                .build(), context.messages());
    }

    private CommandService unloadChunksCommand() {
        return new CommandService(HydroCommand.builder("unloadchunks")
                .permission("hydroxide.command.unloadchunks")
                .usage("/{label} [-f]")
                .executor(ctx -> unloadChunks(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), List.of("-f")) : List.of())
                .build(), context.messages());
    }

    private CommandService gameruleCommand() {
        return new CommandService(HydroCommand.builder("gamerule")
                .permission("hydroxide.command.gamerule")
                .usage("/{label} [world] <gamerule|difficulty|pvp> <value>")
                .executor(ctx -> gamerule(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::gameruleCompletions)
                .build(), context.messages());
    }

    private void handleCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "worlds.usage", Map.of("label", label));
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, label, args);
            case "load" -> load(sender, label, args);
            case "unload" -> unload(sender, label, args, false);
            case "delete" -> delete(sender, label, args);
            case "setrule" -> setRule(sender, label, args);
            case "list" -> context.message(sender, "worlds.list", Map.of(
                    "worlds", String.join(", ", Bukkit.getWorlds().stream().map(World::getName).toList())
            ));
            default -> context.message(sender, "worlds.usage", Map.of("label", label));
        }
    }

    private List<String> completions(String[] args) {
        if (args.length == 1) {
            return CommandUtils.matching(args[0], SUBCOMMANDS);
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("create")) {
            if (args.length == 3) {
                return CommandUtils.matching(args[2], ENVIRONMENTS);
            }
            if (args.length == 4) {
                return CommandUtils.matching(args[3], List.of("random", "0"));
            }
        }
        if ((subcommand.equals("load") || subcommand.equals("delete")) && args.length == 2) {
            return CommandUtils.matching(args[1], knownWorldNames());
        }
        if ((subcommand.equals("unload") || subcommand.equals("setrule")) && args.length == 2) {
            return CommandUtils.matching(args[1], Bukkit.getWorlds().stream().map(World::getName).toList());
        }
        if (subcommand.equals("setrule") && args.length == 3) {
            return CommandUtils.matching(args[2], worldSettingNames());
        }
        if (subcommand.equals("setrule") && args.length == 4) {
            if (args[2].equalsIgnoreCase("difficulty")) {
                return CommandUtils.matching(args[3], java.util.Arrays.stream(Difficulty.values())
                        .map(value -> value.name().toLowerCase(Locale.ROOT))
                        .toList());
            }
            return CommandUtils.matching(args[3], List.of("true", "false", "0", "1", "10", "100"));
        }
        return List.of();
    }

    private List<String> gameruleCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            List<String> values = new ArrayList<>(Bukkit.getWorlds().stream().map(World::getName).toList());
            values.addAll(worldSettingNames());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            if (Bukkit.getWorld(ctx.argument(0)) != null) {
                return CommandUtils.matching(ctx.argument(1), worldSettingNames());
            }
            return CommandUtils.matching(ctx.argument(1), List.of("true", "false", "0", "1", "10", "100"));
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("true", "false", "0", "1", "10", "100"));
        }
        return List.of();
    }

    private void gamerule(CommandSender sender, String label, List<String> args) {
        Optional<WorldGameruleCommandParser.Request> parsed = WorldGameruleCommandParser.parse(args, sender instanceof Player);
        if (parsed.isEmpty()) {
            context.message(sender, "worlds.gamerule.usage", Map.of("label", label));
            return;
        }

        WorldGameruleCommandParser.Request request = parsed.orElseThrow();
        World world = request.worldName()
                .map(Bukkit::getWorld)
                .orElseGet(() -> sender instanceof Player player ? player.getWorld() : null);
        if (world == null) {
            context.message(sender, "worlds.gamerule.not-loaded", Map.of("world", request.worldName().orElse("")));
            return;
        }

        String key = request.setting().toLowerCase(Locale.ROOT);
        if (!applyWorldSetting(world, key, request.value())) {
            context.message(sender, "worlds.gamerule.invalid", Map.of("setting", key, "value", request.value()));
            return;
        }
        persistWorldSetting(world, key, request.value());
        context.message(sender, "worlds.gamerule.saved", Map.of("world", world.getName(), "setting", key, "value", request.value()));
    }

    private void create(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.message(sender, "worlds.create.usage", Map.of("label", label));
            return;
        }
        WorldDefinition definition = WorldDefinition.fromCommand(
                args[1],
                args.length > 2 ? args[2] : "normal",
                args.length > 3 ? args[3] : "random",
                args.length > 4 ? args[4] : ""
        );
        World world = createWorld(definition);
        persist(definition);
        context.message(sender, world == null ? "worlds.create.failed" : "worlds.create.success",
                Map.of("world", world == null ? definition.name() : world.getName()));
    }

    private void load(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.message(sender, "worlds.load.usage", Map.of("label", label));
            return;
        }
        YamlConfiguration yaml = store.load();
        ConfigurationSection section = yaml.getConfigurationSection("worlds." + args[1].toLowerCase(Locale.ROOT));
        WorldDefinition definition = section == null
                ? WorldDefinition.fromCommand(args[1], "normal", "random", "")
                : new WorldDefinition(args[1].toLowerCase(Locale.ROOT), section.getString("environment", "NORMAL"), section.getLong("seed"), section.getString("generator", ""));
        World world = createWorld(definition);
        applySettings(world, section);
        context.message(sender, world == null ? "worlds.load.failed" : "worlds.load.success",
                Map.of("world", args[1]));
    }

    private void unload(CommandSender sender, String label, String[] args, boolean quiet) {
        if (args.length < 2) {
            context.message(sender, "worlds.unload.usage", Map.of("label", label));
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        boolean unloaded = world != null && Bukkit.unloadWorld(world, true);
        if (!quiet) {
            context.message(sender, unloaded ? "worlds.unload.success" : "worlds.unload.failed",
                    Map.of("world", args[1]));
        }
    }

    private void unloadChunks(CommandSender sender, String label, List<String> args) {
        Optional<UnloadChunksCommandParser.Request> parsed = UnloadChunksCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "worlds.unloadchunks.usage", Map.of("label", label));
            return;
        }
        UnloadChunksCommandParser.Request request = parsed.orElseThrow();
        if (unloadChunksTask != null && !unloadChunksTask.isCancelled()) {
            if (!request.forced()) {
                context.message(sender, "worlds.unloadchunks.busy", Map.of());
                return;
            }
            unloadChunksTask.cancel();
            unloadChunksTask = null;
        }

        List<Chunk> chunks = loadedChunks();
        if (chunks.isEmpty()) {
            context.message(sender, "worlds.unloadchunks.done", new ChunkUnloadResult(0, 0).placeholders(0));
            return;
        }
        if (request.forced()) {
            ChunkUnloadResult result = unloadChunkBatch(chunks, ChunkUnloadBatchPolicy.limit(chunks.size(), true, unloadChunksBatchSize()));
            context.message(sender, "worlds.unloadchunks.done", result.placeholders(0));
            return;
        }

        int batchSize = unloadChunksBatchSize();
        context.message(sender, "worlds.unloadchunks.started", Map.of(
                "chunks", chunks.size(),
                "batch", ChunkUnloadBatchPolicy.limit(chunks.size(), false, batchSize)
        ));
        startChunkUnloadTask(sender, chunks, batchSize);
    }

    private void startChunkUnloadTask(CommandSender sender, List<Chunk> chunks, int batchSize) {
        unloadChunksTask = Bukkit.getScheduler().runTaskTimer(context.plugin(), new Runnable() {
            private int index;
            private int unloaded;
            private int skipped;

            @Override
            public void run() {
                int limit = ChunkUnloadBatchPolicy.limit(chunks.size() - index, false, batchSize);
                int processed = 0;
                while (processed < limit && index < chunks.size()) {
                    ChunkUnloadResult result = unloadSingleChunk(chunks.get(index++));
                    unloaded += result.unloaded();
                    skipped += result.skipped();
                    processed++;
                }
                if (index >= chunks.size()) {
                    BukkitTask task = unloadChunksTask;
                    unloadChunksTask = null;
                    if (task != null) {
                        task.cancel();
                    }
                    context.message(sender, "worlds.unloadchunks.done", new ChunkUnloadResult(unloaded, skipped).placeholders(0));
                }
            }
        }, 1L, 1L);
    }

    private ChunkUnloadResult unloadChunkBatch(List<Chunk> chunks, int limit) {
        int unloaded = 0;
        int skipped = 0;
        for (int i = 0; i < limit && i < chunks.size(); i++) {
            ChunkUnloadResult result = unloadSingleChunk(chunks.get(i));
            unloaded += result.unloaded();
            skipped += result.skipped();
        }
        int remaining = Math.max(0, chunks.size() - limit);
        return new ChunkUnloadResult(unloaded, skipped + remaining);
    }

    private ChunkUnloadResult unloadSingleChunk(Chunk chunk) {
        if (containsOnlinePlayer(chunk)) {
            return new ChunkUnloadResult(0, 1);
        }
        return chunk.unload(true) ? new ChunkUnloadResult(1, 0) : new ChunkUnloadResult(0, 1);
    }

    private boolean containsOnlinePlayer(Chunk chunk) {
        return chunk.getWorld().getPlayers().stream()
                .anyMatch(player -> player.getLocation().getBlockX() >> 4 == chunk.getX()
                        && player.getLocation().getBlockZ() >> 4 == chunk.getZ());
    }

    private List<Chunk> loadedChunks() {
        List<Chunk> chunks = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            chunks.addAll(List.of(world.getLoadedChunks()));
        }
        return chunks;
    }

    private int unloadChunksBatchSize() {
        return Math.max(1, context.plugin().getConfig().getInt("worlds.unloadchunks.batch-size", 25));
    }

    private void delete(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.message(sender, "worlds.delete.usage", Map.of("label", label));
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        World world = Bukkit.getWorld(name);
        if (world != null && !Bukkit.unloadWorld(world, true)) {
            context.message(sender, "worlds.delete.unload-failed", Map.of("world", name));
            return;
        }
        try {
            File worldDir = new File(Bukkit.getWorldContainer(), name).getCanonicalFile();
            File container = Bukkit.getWorldContainer().getCanonicalFile();
            if (!worldDir.toPath().startsWith(container.toPath()) || worldDir.equals(container)) {
                context.message(sender, "worlds.delete.unsafe", Map.of("world", name));
                return;
            }
            if (worldDir.exists()) {
                try (java.util.stream.Stream<java.nio.file.Path> paths = Files.walk(worldDir.toPath())) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
                }
            }
            YamlConfiguration yaml = store.load();
            yaml.set("worlds." + name, null);
            store.save(yaml);
            context.message(sender, "worlds.delete.success", Map.of("world", name));
        } catch (IOException | IllegalStateException exception) {
            context.message(sender, "worlds.delete.failed", Map.of("world", name, "reason", exception.getMessage()));
        }
    }

    private void setRule(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            context.message(sender, "worlds.setrule.usage", Map.of("label", label));
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            context.message(sender, "worlds.setrule.not-loaded", Map.of("world", args[1]));
            return;
        }
        String key = args[2].toLowerCase(Locale.ROOT);
        String value = CommandUtils.joinArgs(args, 3);
        if (!applyWorldSetting(world, key, value)) {
            context.message(sender, "worlds.setrule.invalid", Map.of("setting", key, "value", value));
            return;
        }
        persistWorldSetting(world, key, value);
        context.message(sender, "worlds.setrule.saved", Map.of("world", world.getName(), "setting", key, "value", value));
    }

    private void loadConfiguredWorlds() {
        YamlConfiguration yaml = store.load();
        ConfigurationSection worlds = yaml.getConfigurationSection("worlds");
        if (worlds == null) {
            return;
        }
        for (String name : worlds.getKeys(false)) {
            ConfigurationSection section = worlds.getConfigurationSection(name);
            WorldDefinition definition = new WorldDefinition(name, section.getString("environment", "NORMAL"), section.getLong("seed"), section.getString("generator", ""));
            World world = createWorld(definition);
            applySettings(world, section);
        }
    }

    private World createWorld(WorldDefinition definition) {
        WorldCreator creator = new WorldCreator(definition.name());
        creator.environment(World.Environment.valueOf(definition.environment()));
        if (definition.seed() != 0L) {
            creator.seed(definition.seed());
        }
        if (!definition.generator().isBlank()) {
            creator.generator(definition.generator());
        }
        return Bukkit.createWorld(creator);
    }

    private void persist(WorldDefinition definition) {
        YamlConfiguration yaml = store.load();
        String path = "worlds." + definition.name();
        yaml.set(path + ".environment", definition.environment());
        yaml.set(path + ".seed", definition.seed());
        yaml.set(path + ".generator", definition.generator());
        store.save(yaml);
    }

    private void applySettings(World world, ConfigurationSection section) {
        if (world == null || section == null) {
            return;
        }
        ConfigurationSection settings = section.getConfigurationSection("settings");
        if (settings == null) {
            return;
        }
        for (String key : settings.getKeys(false)) {
            String value = settings.getString(key, "");
            if (key.equalsIgnoreCase("difficulty")) {
                parseDifficulty(value).ifPresent(world::setDifficulty);
            } else if (key.equalsIgnoreCase("pvp")) {
                parseBoolean(value).ifPresent(enabled -> setWorldPvp(world, enabled));
            } else {
                applyGameRule(world, key, value);
            }
        }
    }

    private boolean applyWorldSetting(World world, String key, String value) {
        if (key.equals("difficulty")) {
            return parseDifficulty(value).map(difficulty -> {
                world.setDifficulty(difficulty);
                return true;
            }).orElse(false);
        }
        if (key.equals("pvp")) {
            return parseBoolean(value).map(enabled -> {
                setWorldPvp(world, enabled);
                return true;
            }).orElse(false);
        }
        return applyGameRule(world, key, value);
    }

    @SuppressWarnings("deprecation")
    private void setWorldPvp(World world, boolean enabled) {
        // Paper 1.21.11 still exposes runtime world PVP changes through this Bukkit API method.
        world.setPVP(enabled);
    }

    private void persistWorldSetting(World world, String key, String value) {
        YamlConfiguration yaml = store.load();
        yaml.set("worlds." + world.getName().toLowerCase(Locale.ROOT) + ".settings." + key, value);
        store.save(yaml);
    }

    private boolean applyGameRule(World world, String key, String value) {
        GameRule<?> rule = findGameRule(key);
        if (rule == null) {
            return false;
        }
        Object defaultValue = world.getGameRuleDefault(rule);
        Optional<Object> parsed = ModernRegistryKeys.parseGameRuleValue(defaultValue, value);
        return parsed.map(parsedValue -> setGameRule(world, rule, parsedValue)).orElse(false);
    }

    private List<String> knownWorldNames() {
        YamlConfiguration yaml = store.load();
        ConfigurationSection section = yaml.getConfigurationSection("worlds");
        java.util.stream.Stream<String> configured = section == null ? java.util.stream.Stream.empty() : section.getKeys(false).stream();
        java.util.stream.Stream<String> loaded = Bukkit.getWorlds().stream().map(World::getName);
        return java.util.stream.Stream.concat(configured, loaded)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<String> worldSettingNames() {
        java.util.stream.Stream<String> gameRules = Registry.GAME_RULE.keyStream().map(NamespacedKey::getKey);
        return java.util.stream.Stream.concat(java.util.stream.Stream.of("difficulty", "pvp"), gameRules)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private GameRule<?> findGameRule(String key) {
        for (String candidate : ModernRegistryKeys.gameRuleKeys(key)) {
            GameRule<?> rule = Registry.GAME_RULE.get(NamespacedKey.minecraft(candidate));
            if (rule != null) {
                return rule;
            }
        }
        return null;
    }

    private Optional<Difficulty> parseDifficulty(String value) {
        try {
            return Optional.of(Difficulty.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<Boolean> parseBoolean(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on", "1" -> Optional.of(true);
            case "false", "no", "off", "0" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean setGameRule(World world, GameRule<?> rule, Object value) {
        return world.setGameRule((GameRule) rule, value);
    }

    private record ChunkUnloadResult(int unloaded, int skipped) {

        Map<String, Object> placeholders(int remaining) {
            return Map.of(
                    "unloaded", unloaded,
                    "skipped", skipped,
                    "remaining", remaining
            );
        }
    }
}
