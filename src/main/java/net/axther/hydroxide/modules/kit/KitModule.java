package net.axther.hydroxide.modules.kit;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class KitModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private static final String MENU_TITLE = "Hydroxide Kits";
    private static final String PREVIEW_TITLE = "Kit Preview: ";
    private HydroxideContext context;
    private YamlStore kitStore;
    private YamlStore cooldownStore;

    @Override
    public String id() {
        return "kits";
    }

    @Override
    public String displayName() {
        return "Kits";
    }

    @Override
    public String description() {
        return "Captures, previews, and distributes serialized item kits with cooldowns.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.kitStore = new YamlStore(new File(context.plugin().getDataFolder(), "kits.yml"));
        this.cooldownStore = new YamlStore(new File(context.plugin().getDataFolder(), "data/kit-cooldowns.yml"));
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("kit", this);
        context.commands().register("kits", this);
        context.commands().register("setkit", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(java.util.Locale.ROOT)) {
            case "setkit" -> setKit(sender, label, args);
            case "kits" -> openMenu(sender);
            case "kit" -> claimKit(sender, label, args);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(java.util.Locale.ROOT);
        if ((name.equals("kit") || name.equals("setkit")) && args.length == 1) {
            return CommandUtils.matching(args[0], kitNames());
        }
        if (name.equals("setkit") && args.length == 2) {
            return CommandUtils.matching(args[1], List.of("0", "60", "300", "3600", "86400"));
        }
        return List.of();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = context.text().plain(event.getView().title());
        if (title.startsWith(PREVIEW_TITLE)) {
            event.setCancelled(true);
            return;
        }
        if (!title.equals(MENU_TITLE)) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String kit = context.text().plain(item.getItemMeta().displayName());
        if (event.isRightClick()) {
            player.openInventory(previewInventory(kit));
        } else {
            giveKit(player, kit);
        }
    }

    private boolean setKit(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.setkit")) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            context.send(sender, "<red>Only players can capture kits.");
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <name> [cooldownSeconds]");
            return true;
        }
        YamlConfiguration yaml = kitStore.load();
        String path = "kits." + args[0].toLowerCase(java.util.Locale.ROOT);
        yaml.set(path + ".cooldown-seconds", args.length >= 2 ? parseLong(args[1], 0L) : 0L);
        yaml.set(path + ".permission", "hydroxide.kit." + args[0].toLowerCase(java.util.Locale.ROOT));
        yaml.set(path + ".items", serialize(player.getInventory().getContents()));
        kitStore.save(yaml);
        context.send(sender, "<green>Kit captured.");
        return true;
    }

    private boolean openMenu(CommandSender sender) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can open kit menus.");
            return true;
        }
        player.openInventory(menuInventory(player));
        return true;
    }

    private boolean claimKit(CommandSender sender, String label, String[] args) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can claim kits.");
            return true;
        }
        if (args.length == 0) {
            player.openInventory(menuInventory(player));
            return true;
        }
        giveKit(player, args[0].toLowerCase(java.util.Locale.ROOT));
        return true;
    }

    private void giveKit(Player player, String kit) {
        ConfigurationSection section = kitStore.load().getConfigurationSection("kits." + kit.toLowerCase(java.util.Locale.ROOT));
        if (section == null) {
            context.send(player, "<red>That kit does not exist.");
            return;
        }
        String permission = section.getString("permission", "");
        if (!permission.isBlank() && !player.hasPermission(permission)) {
            context.send(player, "<red>You do not have permission for that kit.");
            return;
        }
        long cooldownUntil = cooldownStore.load().getLong(player.getUniqueId() + "." + kit, 0L);
        if (cooldownUntil > System.currentTimeMillis()) {
            context.send(player, "<red>That kit is still on cooldown.");
            return;
        }
        for (ItemStack item : deserialize(section.getMapList("items"))) {
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
            }
        }
        long cooldown = section.getLong("cooldown-seconds", 0L);
        if (cooldown > 0) {
            YamlConfiguration yaml = cooldownStore.load();
            yaml.set(player.getUniqueId() + "." + kit, Instant.now().plusSeconds(cooldown).toEpochMilli());
            cooldownStore.save(yaml);
        }
        context.send(player, "<green>Kit claimed.");
    }

    private Inventory menuInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(MENU_TITLE));
        ConfigurationSection kits = kitStore.load().getConfigurationSection("kits");
        if (kits == null) {
            return inventory;
        }
        for (String kit : kits.getKeys(false)) {
            inventory.addItem(named(Material.CHEST, kit, "<gray>Left click claim, right click preview."));
        }
        return inventory;
    }

    private List<String> kitNames() {
        ConfigurationSection kits = kitStore.load().getConfigurationSection("kits");
        return kits == null ? List.of() : kits.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private Inventory previewInventory(String kit) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(PREVIEW_TITLE + kit));
        ConfigurationSection section = kitStore.load().getConfigurationSection("kits." + kit.toLowerCase(java.util.Locale.ROOT));
        if (section != null) {
            for (ItemStack item : deserialize(section.getMapList("items"))) {
                if (item != null && item.getType() != Material.AIR) {
                    inventory.addItem(item);
                }
            }
        }
        return inventory;
    }

    private ItemStack named(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(List.of(context.text().format(lore)));
        item.setItemMeta(meta);
        return item;
    }

    private List<Map<String, Object>> serialize(ItemStack[] items) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                serialized.add(item.serialize());
            }
        }
        return serialized;
    }

    private List<ItemStack> deserialize(List<Map<?, ?>> maps) {
        List<ItemStack> items = new ArrayList<>();
        for (Map<?, ?> map : maps) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            items.add(ItemStack.deserialize(typed));
        }
        return items;
    }

    private long parseLong(String input, long fallback) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
