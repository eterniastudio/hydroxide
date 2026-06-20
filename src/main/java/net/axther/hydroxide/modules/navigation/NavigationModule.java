package net.axther.hydroxide.modules.navigation;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.StoredLocation;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
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

public final class NavigationModule implements HydroModule, Listener {

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
        context.commands().register("homesgui", command("homesgui", "hydroxide.command.homesgui", player -> player.openInventory(homeInventory(player))));
        context.commands().register("warpgui", command("warpgui", "hydroxide.command.warpgui", player -> player.openInventory(warpInventory())));
    }

    @Override
    public void onDisable(HydroxideContext context) {
        warmups.values().forEach(warmup -> warmup.task().cancel());
        warmups.clear();
    }

    private CommandService command(String name, String permission, java.util.function.Consumer<Player> executor) {
        return new CommandService(HydroCommand.builder(name)
                .permission(permission)
                .playerOnly(true)
                .usage("/{label}")
                .executor(ctx -> executor.accept((Player) ctx.sender()))
                .build(), context.messages());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = context.text().plain(event.getView().title());
        if (!title.equals(homesTitle()) && !title.equals(warpsTitle())) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String name = context.text().plain(item.getItemMeta().displayName());
        Optional<StoredLocation> location = title.equals(homesTitle())
                ? context.playerData().home(player.getUniqueId(), name)
                : context.warps().get(name);
        location.ifPresent(stored -> startWarmup(player, stored));
        if (title.equals(homesTitle()) && event.getClick() == ClickType.SHIFT_RIGHT) {
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
            cancelWarmup(event.getPlayer(), "navigation.warmup.cancelled-moved");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && warmups.containsKey(player.getUniqueId())) {
            cancelWarmup(player, "navigation.warmup.cancelled-damaged");
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (warmups.containsKey(event.getPlayer().getUniqueId())) {
            cancelWarmup(event.getPlayer(), "navigation.warmup.cancelled-block-break");
        }
    }

    private Inventory homeInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, context.messages().component("navigation.homes-title", Map.of()));
        for (String home : context.playerData().homes(player.getUniqueId())) {
            inventory.addItem(namedItem(Material.RED_BED, Component.text(home), context.messages().component("navigation.home-lore", Map.of())));
        }
        int limit = HomeLimitService.highestLimit(player::hasPermission,
                context.plugin().getConfig().getInt("navigation.default-home-limit", 3),
                context.plugin().getConfig().getInt("navigation.max-home-limit", 25));
        inventory.setItem(53, namedItem(Material.COMPASS,
                context.messages().component("navigation.home-limit-name", Map.of("limit", limit)),
                context.messages().component("navigation.home-limit-lore", Map.of("count", context.playerData().homes(player.getUniqueId()).size()))));
        return inventory;
    }

    private Inventory warpInventory() {
        Inventory inventory = Bukkit.createInventory(null, 54, context.messages().component("navigation.warps-title", Map.of()));
        for (String warp : context.warps().names()) {
            inventory.addItem(namedItem(Material.ENDER_PEARL, Component.text(warp), context.messages().component("navigation.warp-lore", Map.of())));
        }
        return inventory;
    }

    private ItemStack namedItem(Material material, Component name, Component lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(List.of(lore));
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
        context.message(player, "navigation.warmup.started", Map.of("seconds", seconds));
    }

    private void cancelWarmup(Player player, String messageKey) {
        Warmup warmup = warmups.remove(player.getUniqueId());
        if (warmup != null) {
            warmup.task().cancel();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
            if (messageKey != null) {
                context.message(player, messageKey, Map.of());
            }
        }
    }

    private String homesTitle() {
        return context.text().plain(context.messages().component("navigation.homes-title", Map.of()));
    }

    private String warpsTitle() {
        return context.text().plain(context.messages().component("navigation.warps-title", Map.of()));
    }

    private Optional<Location> toLocation(StoredLocation stored) {
        World world = Bukkit.getWorld(stored.worldName());
        return world == null ? Optional.empty() : Optional.of(stored.toLocation(world));
    }

    private record Warmup(Location origin, BukkitTask task) {
    }
}
