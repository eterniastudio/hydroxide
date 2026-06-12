package net.axther.hydroxide.modules.spawn;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.StoredLocation;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.title.Title;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class AdvancedSpawnModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private HydroxideContext context;
    private YamlStore spawnStore;
    private YamlStore firstJoinStore;
    private Permission permissions;

    @Override
    public String id() {
        return "advanced-spawn";
    }

    @Override
    public String displayName() {
        return "Advanced Spawn";
    }

    @Override
    public String description() {
        return "Group-aware spawn routing, first-join welcome actions, and starter items.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.spawnStore = new YamlStore(new File(context.plugin().getDataFolder(), "spawn.yml"));
        this.firstJoinStore = new YamlStore(new File(context.plugin().getDataFolder(), "data/first-joins.yml"));
        RegisteredServiceProvider<Permission> provider = Bukkit.getServicesManager().getRegistration(Permission.class);
        this.permissions = provider == null ? null : provider.getProvider();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("groupspawn", this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.groupspawn")) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            context.send(sender, "<red>Only players can set group spawns.");
            return true;
        }
        if (args.length < 2 || (!args[0].equalsIgnoreCase("set") && !args[0].equalsIgnoreCase("delete"))) {
            context.send(sender, "<red>Usage: /" + label + " <set|delete> <group> [priority]");
            return true;
        }

        String group = args[1].toLowerCase(Locale.ROOT);
        YamlConfiguration yaml = spawnStore.load();
        if (args[0].equalsIgnoreCase("delete")) {
            yaml.set("groups." + group, null);
            spawnStore.save(yaml);
            context.send(sender, "<green>Deleted spawn group <white>" + group + "<green>.");
            return true;
        }

        int priority = args.length >= 3 ? parseInt(args[2], 0) : 0;
        ConfigurationSection section = yaml.createSection("groups." + group);
        section.set("priority", priority);
        StoredLocation.from(player.getLocation()).writeTo(section.createSection("location"));
        spawnStore.save(yaml);
        context.send(sender, "<green>Set spawn group <white>" + group + " <green>with priority <white>" + priority + "<green>.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtils.matching(args[0], List.of("delete", "set"));
        }
        if (args.length == 2) {
            return CommandUtils.matching(args[1], groupNames());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return CommandUtils.matching(args[2], List.of("0", "10", "50", "100"));
        }
        return List.of();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (markFirstJoin(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
                resolveSpawn(player).ifPresent(location -> teleport(player, location));
                runFirstJoinActions(player);
                context.services().vanishService().ifPresent(service -> service.reconcileVisibility(player));
            }, 5L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        resolveSpawn(event.getPlayer()).flatMap(this::toLocation).ifPresent(event::setRespawnLocation);
    }

    private Optional<StoredLocation> resolveSpawn(Player player) {
        return new GroupSpawnResolver(loadEntries()).resolve(playerGroups(player));
    }

    private List<GroupSpawnResolver.Entry> loadEntries() {
        YamlConfiguration yaml = spawnStore.load();
        ConfigurationSection groups = yaml.getConfigurationSection("groups");
        if (groups == null) {
            return List.of();
        }
        List<GroupSpawnResolver.Entry> entries = new ArrayList<>();
        for (String group : groups.getKeys(false)) {
            ConfigurationSection section = groups.getConfigurationSection(group);
            Optional<StoredLocation> location = StoredLocation.readFrom(section == null ? null : section.getConfigurationSection("location"));
            location.ifPresent(stored -> entries.add(new GroupSpawnResolver.Entry(group, section.getInt("priority"), stored)));
        }
        return entries;
    }

    private List<String> groupNames() {
        ConfigurationSection groups = spawnStore.load().getConfigurationSection("groups");
        return groups == null ? List.of() : groups.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private List<String> playerGroups(Player player) {
        List<String> groups = new ArrayList<>();
        if (permissions != null) {
            String primary = permissions.getPrimaryGroup(player);
            if (primary != null) {
                groups.add(primary);
            }
        }
        ConfigurationSection groupSection = spawnStore.load().getConfigurationSection("groups");
        if (groupSection != null) {
            for (String group : groupSection.getKeys(false)) {
                if (player.hasPermission("hydroxide.spawn.group." + group.toLowerCase(Locale.ROOT))) {
                    groups.add(group);
                }
            }
        }
        groups.add("default");
        return groups;
    }

    private void runFirstJoinActions(Player player) {
        String title = context.plugin().getConfig().getString("first-join.title", "<#44CCFF><bold>Welcome</bold>");
        String subtitle = context.plugin().getConfig().getString("first-join.subtitle", "<gray>Enjoy your stay, <white>{player}<gray>!");
        player.showTitle(Title.title(
                context.text().format(title.replace("{player}", player.getName())),
                context.text().format(subtitle.replace("{player}", player.getName())),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(750))
        ));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
        if (context.plugin().getConfig().getBoolean("first-join.firework", true)) {
            Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK_ROCKET);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(0);
            firework.setFireworkMeta(meta);
            firework.detonate();
        }
        giveStarterItems(player);
    }

    private void giveStarterItems(Player player) {
        for (String entry : context.plugin().getConfig().getStringList("first-join.items")) {
            String[] parts = entry.split(":", 2);
            Material material = Material.matchMaterial(parts[0]);
            if (material == null) {
                continue;
            }
            int amount = parts.length == 2 ? parseInt(parts[1], 1) : 1;
            player.getInventory().addItem(new ItemStack(material, Math.max(1, amount)));
        }
    }

    private boolean markFirstJoin(UUID playerId) {
        YamlConfiguration yaml = firstJoinStore.load();
        String path = "seen." + playerId;
        if (yaml.getBoolean(path, false)) {
            return false;
        }
        yaml.set(path, true);
        firstJoinStore.save(yaml);
        return true;
    }

    private void teleport(Player player, StoredLocation stored) {
        toLocation(stored).ifPresent(player::teleportAsync);
    }

    private Optional<Location> toLocation(StoredLocation stored) {
        World world = Bukkit.getWorld(stored.worldName());
        return world == null ? Optional.empty() : Optional.of(stored.toLocation(world));
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
