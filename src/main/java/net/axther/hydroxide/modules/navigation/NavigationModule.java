package net.axther.hydroxide.modules.navigation;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.StoredLocation;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NavigationModule implements HydroModule, Listener, CommandExecutor {

    private static final String HOMES_TITLE = "Hydroxide Homes";
    private static final String WARPS_TITLE = "Hydroxide Warps";
    private HydroxideContext context;
    private final Map<UUID, Warmup> warmups = new HashMap<>();

    @Override
    public String id() {
        return "navigation";
    }

    @Override
    public String displayName() {
        return "GUI Navigation";
    }

    @Override
    public String description() {
        return "Chest GUI navigation for homes and warps with teleport warmups.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core", "teleport");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("homesgui", this);
        context.commands().register("warpgui", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        warmups.values().forEach(warmup -> warmup.task().cancel());
        warmups.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can use navigation menus.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("homesgui")) {
            if (!context.requirePermission(sender, "hydroxide.command.homesgui")) {
                return true;
            }
            player.openInventory(homeInventory(player));
            return true;
        }
        if (!context.requirePermission(sender, "hydroxide.command.warpgui")) {
            return true;
        }
        player.openInventory(warpInventory());
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = context.text().plain(event.getView().title());
        if (!title.equals(HOMES_TITLE) && !title.equals(WARPS_TITLE)) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String name = context.text().plain(item.getItemMeta().displayName());
        Optional<StoredLocation> location = title.equals(HOMES_TITLE)
                ? context.playerData().home(player.getUniqueId(), name)
                : context.warps().get(name);
        location.ifPresent(stored -> startWarmup(player, stored));
        if (title.equals(HOMES_TITLE) && event.getClick() == ClickType.SHIFT_RIGHT) {
            context.playerData().removeHome(player.getUniqueId(), name);
            player.openInventory(homeInventory(player));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Warmup warmup = warmups.get(event.getPlayer().getUniqueId());
        if (warmup == null || event.getTo() == null) {
            return;
        }
        if (warmup.origin().distanceSquared(event.getTo()) > 0.35D) {
            cancelWarmup(event.getPlayer(), "<red>Teleport cancelled because you moved.");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && warmups.containsKey(player.getUniqueId())) {
            cancelWarmup(player, "<red>Teleport cancelled because you took damage.");
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (warmups.containsKey(event.getPlayer().getUniqueId())) {
            cancelWarmup(event.getPlayer(), "<red>Teleport cancelled because you broke a block.");
        }
    }

    private Inventory homeInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(HOMES_TITLE));
        for (String home : context.playerData().homes(player.getUniqueId())) {
            inventory.addItem(namedItem(Material.RED_BED, home, "<gray>Left click to teleport. Shift-right to delete."));
        }
        int limit = HomeLimitService.highestLimit(player::hasPermission,
                context.plugin().getConfig().getInt("navigation.default-home-limit", 3),
                context.plugin().getConfig().getInt("navigation.max-home-limit", 25));
        inventory.setItem(53, namedItem(Material.COMPASS, "Limit: " + limit, "<gray>Current homes: " + context.playerData().homes(player.getUniqueId()).size()));
        return inventory;
    }

    private Inventory warpInventory() {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(WARPS_TITLE));
        for (String warp : context.warps().names()) {
            inventory.addItem(namedItem(Material.ENDER_PEARL, warp, "<gray>Click to warp."));
        }
        return inventory;
    }

    private ItemStack namedItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(List.of(context.text().format(lore)));
        item.setItemMeta(meta);
        return item;
    }

    private void startWarmup(Player player, StoredLocation stored) {
        cancelWarmup(player, null);
        int seconds = context.plugin().getConfig().getInt("navigation.warmup-seconds", 3);
        Location origin = player.getLocation();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
            warmups.remove(player.getUniqueId());
            toLocation(stored).ifPresent(location -> player.teleportAsync(location));
        }, Math.max(0, seconds) * 20L);
        warmups.put(player.getUniqueId(), new Warmup(origin, task));
        context.send(player, "<green>Teleporting in <white>" + seconds + " <green>seconds. Do not move.");
    }

    private void cancelWarmup(Player player, String message) {
        Warmup warmup = warmups.remove(player.getUniqueId());
        if (warmup != null) {
            warmup.task().cancel();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
            if (message != null) {
                context.send(player, message);
            }
        }
    }

    private Optional<Location> toLocation(StoredLocation stored) {
        World world = Bukkit.getWorld(stored.worldName());
        return world == null ? Optional.empty() : Optional.of(stored.toLocation(world));
    }

    private record Warmup(Location origin, BukkitTask task) {
    }
}
