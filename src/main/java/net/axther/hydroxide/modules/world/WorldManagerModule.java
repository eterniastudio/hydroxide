package net.axther.hydroxide.modules.world;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.registry.ModernRegistryKeys;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class WorldManagerModule implements HydroModule, CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("create", "delete", "list", "load", "setrule", "unload");
    private static final List<String> ENVIRONMENTS = List.of("normal", "nether", "end");

    private HydroxideContext context;
    private YamlStore store;

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
        context.commands().register("hydroworld", this);
        loadConfiguredWorlds();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.hydroworld")) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <create|load|unload|delete|setrule|list> ...");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, label, args);
            case "load" -> load(sender, label, args);
            case "unload" -> unload(sender, label, args, false);
            case "delete" -> delete(sender, label, args);
            case "setrule" -> setRule(sender, label, args);
            case "list" -> context.send(sender, "<green>Worlds: <white>" + String.join("<gray>, <white>", Bukkit.getWorlds().stream().map(World::getName).toList()));
            default -> context.send(sender, "<red>Usage: /" + label + " <create|load|unload|delete|setrule|list> ...");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
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

    private void create(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " create <name> [normal|nether|end] [seed] [generator]");
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
        context.send(sender, world == null ? "<red>Unable to create world." : "<green>World <white>" + world.getName() + " <green>created.");
    }

    private void load(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " load <name>");
            return;
        }
        YamlConfiguration yaml = store.load();
        ConfigurationSection section = yaml.getConfigurationSection("worlds." + args[1].toLowerCase(Locale.ROOT));
        WorldDefinition definition = section == null
                ? WorldDefinition.fromCommand(args[1], "normal", "random", "")
                : new WorldDefinition(args[1].toLowerCase(Locale.ROOT), section.getString("environment", "NORMAL"), section.getLong("seed"), section.getString("generator", ""));
        World world = createWorld(definition);
        applySettings(world, section);
        context.send(sender, world == null ? "<red>Unable to load world." : "<green>World loaded.");
    }

    private void unload(CommandSender sender, String label, String[] args, boolean quiet) {
        if (args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " unload <name>");
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        boolean unloaded = world != null && Bukkit.unloadWorld(world, true);
        if (!quiet) {
            context.send(sender, unloaded ? "<green>World unloaded." : "<red>Unable to unload world.");
        }
    }

    private void delete(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " delete <name>");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        World world = Bukkit.getWorld(name);
        if (world != null && !Bukkit.unloadWorld(world, true)) {
            context.send(sender, "<red>Unable to unload world first.");
            return;
        }
        try {
            File worldDir = new File(Bukkit.getWorldContainer(), name).getCanonicalFile();
            File container = Bukkit.getWorldContainer().getCanonicalFile();
            if (!worldDir.toPath().startsWith(container.toPath()) || worldDir.equals(container)) {
                context.send(sender, "<red>Refusing to delete unsafe world path.");
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
            context.send(sender, "<green>World deleted.");
        } catch (IOException | IllegalStateException exception) {
            context.send(sender, "<red>Unable to delete world: <white>" + exception.getMessage());
        }
    }

    private void setRule(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            context.send(sender, "<red>Usage: /" + label + " setrule <world> <gamerule|difficulty|pvp> <value>");
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            context.send(sender, "<red>That world is not loaded.");
            return;
        }
        String key = args[2].toLowerCase(Locale.ROOT);
        String value = CommandUtils.joinArgs(args, 3);
        if (key.equals("difficulty")) {
            world.setDifficulty(Difficulty.valueOf(value.toUpperCase(Locale.ROOT)));
        } else if (!applyGameRule(world, key, value)) {
            context.send(sender, "<red>Unknown or invalid gamerule value.");
            return;
        }
        YamlConfiguration yaml = store.load();
        yaml.set("worlds." + world.getName().toLowerCase(Locale.ROOT) + ".settings." + key, value);
        store.save(yaml);
        context.send(sender, "<green>World setting saved.");
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
                world.setDifficulty(Difficulty.valueOf(value.toUpperCase(Locale.ROOT)));
            } else {
                applyGameRule(world, key, value);
            }
        }
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean setGameRule(World world, GameRule<?> rule, Object value) {
        return world.setGameRule((GameRule) rule, value);
    }
}
