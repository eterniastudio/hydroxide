package net.axther.hydroxide.modules.admin;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.Listener;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.builder.LocationInventoryViewBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AdminUtilityModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private static final List<String> COMMANDS = List.of(
            "invsee", "endersee", "trash", "workbench", "anvil", "cartography", "smithing", "stonecutter",
            "near", "seen", "whois", "sudo", "staffnote"
    );

    private HydroxideContext context;
    private YamlStore store;

    @Override
    public String id() {
        return "admin-utilities";
    }

    @Override
    public String displayName() {
        return "Admin Utilities";
    }

    @Override
    public String description() {
        return "Inventory inspection, virtual workstations, seen/whois, sudo, nearby players, and staff notes.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "admin.yml"));
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        for (String command : COMMANDS) {
            context.commands().register(command, this);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            remember(player);
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "invsee" -> invSee(sender, label, args);
            case "endersee" -> enderSee(sender, label, args);
            case "trash" -> openMenu(sender, MenuType.GENERIC_9X6, "Trash");
            case "workbench" -> openMenu(sender, MenuType.CRAFTING, "Workbench");
            case "anvil" -> openMenu(sender, MenuType.ANVIL, "Anvil");
            case "cartography" -> openMenu(sender, MenuType.CARTOGRAPHY_TABLE, "Cartography");
            case "smithing" -> openMenu(sender, MenuType.SMITHING, "Smithing");
            case "stonecutter" -> openMenu(sender, MenuType.STONECUTTER, "Stonecutter");
            case "near" -> near(sender, args);
            case "seen" -> seen(sender, label, args);
            case "whois" -> whois(sender, label, args);
            case "sudo" -> sudo(sender, label, args);
            case "staffnote" -> staffNote(sender, label, args);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (List.of("invsee", "endersee", "seen", "whois").contains(name) && args.length == 1) {
            return CompletionUtils.onlinePlayers(args[0]);
        }
        if (name.equals("sudo")) {
            if (args.length == 1) {
                return CompletionUtils.onlinePlayers(args[0]);
            }
            if (args.length == 2) {
                return CommandUtils.matching(args[1], List.of("chat", "command"));
            }
        }
        if (name.equals("staffnote")) {
            if (args.length == 1) {
                return CompletionUtils.onlinePlayers(args[0]);
            }
            if (args.length == 2) {
                return CommandUtils.matching(args[1], List.of("add", "clear", "list"));
            }
        }
        return List.of();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        remember(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remember(event.getPlayer());
    }

    private boolean invSee(CommandSender sender, String label, String[] args) {
        Player viewer = player(sender);
        Player target = args.length == 0 ? null : Bukkit.getPlayerExact(args[0]);
        if (viewer == null || target == null) {
            context.send(sender, "<red>Usage: /" + label + " <player>");
            return true;
        }
        if (!context.requirePermission(sender, "hydroxide.admin.invsee")) {
            return true;
        }
        viewer.openInventory(target.getInventory());
        return true;
    }

    private boolean enderSee(CommandSender sender, String label, String[] args) {
        Player viewer = player(sender);
        Player target = args.length == 0 ? null : Bukkit.getPlayerExact(args[0]);
        if (viewer == null || target == null) {
            context.send(sender, "<red>Usage: /" + label + " <player>");
            return true;
        }
        if (!context.requirePermission(sender, "hydroxide.admin.endersee")) {
            return true;
        }
        viewer.openInventory(target.getEnderChest());
        return true;
    }

    private boolean openMenu(CommandSender sender, MenuType.Typed<? extends InventoryView, ? extends LocationInventoryViewBuilder<? extends InventoryView>> menuType, String title) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        InventoryView view = menuType.builder()
                .checkReachable(false)
                .title(context.text().format("<#44CCFF>" + title))
                .build(player);
        player.openInventory(view);
        return true;
    }

    private boolean near(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        double radius = args.length > 0 ? parseDouble(args[0], 100.0D) : 100.0D;
        List<String> nearby = Bukkit.getOnlinePlayers().stream()
                .filter(other -> !other.equals(player))
                .filter(other -> other.getWorld().equals(player.getWorld()))
                .filter(other -> other.getLocation().distanceSquared(player.getLocation()) <= radius * radius)
                .map(other -> other.getName() + " (" + Math.round(other.getLocation().distance(player.getLocation())) + "m)")
                .toList();
        context.send(player, nearby.isEmpty() ? "<gray>No nearby players." : "<green>Nearby: <white>" + String.join("<gray>, <white>", nearby));
        return true;
    }

    private boolean seen(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <player>");
            return true;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        YamlConfiguration yaml = store.load();
        String path = "players." + player.getUniqueId();
        context.send(sender, "<green>Seen <white>" + fallbackName(player) + "<gray>: last seen <white>" + yaml.getString(path + ".last-seen", "never"));
        return true;
    }

    private boolean whois(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <player>");
            return true;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
        Player online = offline.getPlayer();
        YamlConfiguration yaml = store.load();
        String path = "players." + offline.getUniqueId();
        context.send(sender, "<#44CCFF>Whois <white>" + fallbackName(offline));
        context.send(sender, "<gray>UUID: <white>" + offline.getUniqueId());
        context.send(sender, "<gray>Online: <white>" + (online != null));
        context.send(sender, "<gray>Last seen: <white>" + yaml.getString(path + ".last-seen", "never"));
        context.send(sender, "<gray>Last location: <white>" + yaml.getString(path + ".last-location", "unknown"));
        if (sender.hasPermission("hydroxide.admin.ip")) {
            context.send(sender, "<gray>IP hash: <white>" + yaml.getString(path + ".ip-hash", "unknown"));
        }
        return true;
    }

    private boolean sudo(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.admin.sudo")) {
            return true;
        }
        if (args.length < 3) {
            context.send(sender, "<red>Usage: /" + label + " <player> <chat|command> <value>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            context.send(sender, "<red>That player is not online.");
            return true;
        }
        String value = CommandUtils.joinArgs(args, 2);
        if (args[1].equalsIgnoreCase("chat")) {
            target.chat(value);
        } else {
            target.performCommand(value.startsWith("/") ? value.substring(1) : value);
        }
        context.send(sender, "<green>Sudo executed.");
        return true;
    }

    private boolean staffNote(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.admin.staffnote")) {
            return true;
        }
        if (args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " <player> add|list|clear");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        YamlConfiguration yaml = store.load();
        StaffNoteStore notes = new StaffNoteStore(yaml);
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (args.length < 3) {
                    context.send(sender, "<red>Usage: /" + label + " <player> add <note>");
                    return true;
                }
                notes.add(target.getUniqueId(), sender.getName(), CommandUtils.joinArgs(args, 2));
                store.save(yaml);
                context.send(sender, "<green>Staff note added.");
            }
            case "list" -> {
                List<String> list = notes.notes(target.getUniqueId());
                context.send(sender, list.isEmpty() ? "<gray>No staff notes." : "<green>Staff notes:");
                list.forEach(note -> context.send(sender, "<gray>- <white>" + note));
            }
            case "clear" -> {
                notes.clear(target.getUniqueId());
                store.save(yaml);
                context.send(sender, "<green>Staff notes cleared.");
            }
            default -> context.send(sender, "<red>Usage: /" + label + " <player> add|list|clear");
        }
        return true;
    }

    private void remember(Player player) {
        YamlConfiguration yaml = store.load();
        String path = "players." + player.getUniqueId();
        yaml.set(path + ".name", player.getName());
        yaml.set(path + ".last-seen", Instant.now().toString());
        yaml.set(path + ".last-location", player.getWorld().getName() + " "
                + player.getLocation().getBlockX() + " "
                + player.getLocation().getBlockY() + " "
                + player.getLocation().getBlockZ());
        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            yaml.set(path + ".ip-hash", hash(player.getAddress().getAddress().getHostAddress()));
        }
        store.save(yaml);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return "unavailable";
        }
    }

    private Player player(CommandSender sender) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can use this command.");
        }
        return player;
    }

    private String fallbackName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
