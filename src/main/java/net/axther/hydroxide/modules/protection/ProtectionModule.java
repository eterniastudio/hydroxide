package net.axther.hydroxide.modules.protection;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ProtectionModule implements HydroModule, Listener, CommandExecutor {

    private HydroxideContext context;
    private YamlStore lockStore;
    private YamlStore settingsStore;

    @Override
    public String id() {
        return "protection";
    }

    @Override
    public String displayName() {
        return "Protection";
    }

    @Override
    public String description() {
        return "Container locks and lightweight world anti-grief rules.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.lockStore = new YamlStore(new File(context.plugin().getDataFolder(), "locks.yml"));
        this.settingsStore = new YamlStore(new File(context.plugin().getDataFolder(), "world_settings.yml"));
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("lock", this);
        context.commands().register("unlock", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can lock blocks.");
            return true;
        }
        Block target = player.getTargetBlockExact(5);
        if (target == null || !lockable(target.getType())) {
            context.send(sender, "<red>Look at a chest, furnace, barrel, door, or similar block.");
            return true;
        }
        YamlConfiguration yaml = lockStore.load();
        String key = key(target);
        if (command.getName().equalsIgnoreCase("unlock")) {
            if (!owns(player, yaml.getString("locks." + key + ".owner")) && !player.hasPermission("hydroxide.lock.bypass")) {
                context.send(player, "<red>You do not own that lock.");
                return true;
            }
            yaml.set("locks." + key, null);
            lockStore.save(yaml);
            context.send(player, "<green>Unlocked.");
            return true;
        }
        yaml.set("locks." + key + ".owner", player.getUniqueId().toString());
        yaml.set("locks." + key + ".name", player.getName());
        lockStore.save(yaml);
        context.send(player, "<green>Locked.");
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (canBypass(event.getPlayer())) {
            return;
        }
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null
                && event.getClickedBlock().getType() == Material.FARMLAND
                && flag(event.getClickedBlock().getWorld().getName(), "crop-trample", false)) {
            event.setCancelled(true);
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !lockable(block.getType())) {
            return;
        }
        String owner = lockStore.load().getString("locks." + key(block) + ".owner");
        if (owner != null && !owns(event.getPlayer(), owner) && !event.getPlayer().hasPermission("hydroxide.lock.bypass")) {
            event.setCancelled(true);
            context.send(event.getPlayer(), "<red>That block is locked.");
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (flag(event.getLocation().getWorld().getName(), "explosions", false)) {
            event.blockList().clear();
        }
    }

    @EventHandler
    public void onBurn(BlockBurnEvent event) {
        if (flag(event.getBlock().getWorld().getName(), "fire-spread", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpread(BlockSpreadEvent event) {
        if (flag(event.getBlock().getWorld().getName(), "fire-spread", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() == EntityType.ENDERMAN
                && flag(event.getBlock().getWorld().getName(), "enderman-grief", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (canBypass(event.getPlayer())) {
            return;
        }
        List<String> blacklist = settingsStore.load().getStringList("worlds." + event.getBlock().getWorld().getName() + ".block-blacklist");
        if (blacklist.stream().map(value -> value.toUpperCase(Locale.ROOT)).anyMatch(event.getBlock().getType().name()::equals)) {
            event.setCancelled(true);
            context.send(event.getPlayer(), "<red>That block is restricted in this world.");
        }
    }

    private boolean flag(String world, String key, boolean fallback) {
        YamlConfiguration yaml = settingsStore.load();
        if (!yaml.contains("worlds." + world + "." + key)) {
            yaml.set("worlds." + world + ".explosions", false);
            yaml.set("worlds." + world + ".fire-spread", false);
            yaml.set("worlds." + world + ".enderman-grief", false);
            yaml.set("worlds." + world + ".crop-trample", false);
            yaml.set("worlds." + world + ".block-blacklist", List.of("TNT", "LAVA_BUCKET"));
            settingsStore.save(yaml);
        }
        return yaml.getBoolean("worlds." + world + "." + key, fallback);
    }

    private boolean lockable(Material material) {
        return Tag.DOORS.isTagged(material)
                || material.name().contains("CHEST")
                || material == Material.BARREL
                || material == Material.FURNACE
                || material == Material.BLAST_FURNACE
                || material == Material.SMOKER;
    }

    private boolean owns(Player player, String owner) {
        return owner != null && UUID.fromString(owner).equals(player.getUniqueId());
    }

    private String key(Block block) {
        return block.getWorld().getName() + "." + block.getX() + "." + block.getY() + "." + block.getZ();
    }

    private boolean canBypass(Player player) {
        return context.services().builderService()
                .map(service -> service.canBypassHydroxideProtection(player))
                .orElse(false);
    }
}
