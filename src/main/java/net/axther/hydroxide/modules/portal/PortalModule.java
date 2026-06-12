package net.axther.hydroxide.modules.portal;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.StoredLocation;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PortalModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private HydroxideContext context;
    private YamlStore portalStore;
    private BukkitTask particleTask;

    @Override
    public String id() {
        return "portals";
    }

    @Override
    public String displayName() {
        return "Portals";
    }

    @Override
    public String description() {
        return "Coordinate region triggers for warps, velocity launches, and BungeeCord server links.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.portalStore = new YamlStore(new File(context.plugin().getDataFolder(), "portals.yml"));
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        Bukkit.getMessenger().registerOutgoingPluginChannel(context.plugin(), "BungeeCord");
        context.commands().register("portal", this);
        particleTask = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::drawParticles, 40L, 40L);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        if (particleTask != null) {
            particleTask.cancel();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.portal")) {
            return true;
        }
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can create portals.");
            return true;
        }
        if (args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " <create|delete> <name> [destination] [radius]");
            return true;
        }
        YamlConfiguration yaml = portalStore.load();
        String path = "portals." + args[1].toLowerCase(java.util.Locale.ROOT);
        if (args[0].equalsIgnoreCase("delete")) {
            yaml.set(path, null);
            portalStore.save(yaml);
            context.send(sender, "<green>Portal deleted.");
            return true;
        }
        if (!args[0].equalsIgnoreCase("create") || args.length < 3) {
            context.send(sender, "<red>Usage: /" + label + " create <name> <warp|server:name|velocity:x,y,z> [radius]");
            return true;
        }
        double radius = args.length >= 4 ? parseDouble(args[3], 1.5D) : 1.5D;
        Location location = player.getLocation();
        yaml.set(path + ".world", location.getWorld().getName());
        yaml.set(path + ".min.x", location.getX() - radius);
        yaml.set(path + ".min.y", location.getY() - 0.5D);
        yaml.set(path + ".min.z", location.getZ() - radius);
        yaml.set(path + ".max.x", location.getX() + radius);
        yaml.set(path + ".max.y", location.getY() + 2.5D);
        yaml.set(path + ".max.z", location.getZ() + radius);
        yaml.set(path + ".destination", args[2]);
        portalStore.save(yaml);
        context.send(sender, "<green>Portal created.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtils.matching(args[0], List.of("create", "delete"));
        }
        if (args.length == 2) {
            return CommandUtils.matching(args[1], portalNames());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            List<String> destinations = new ArrayList<>(context.warps().names());
            destinations.add("server:");
            destinations.add("velocity:0,1,0");
            return CommandUtils.matching(args[2], destinations);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return CommandUtils.matching(args[3], List.of("1", "1.5", "2", "3", "5"));
        }
        return List.of();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) {
            return;
        }
        Player player = event.getPlayer();
        for (PortalRegion region : loadRegions()) {
            Location to = event.getTo();
            if (region.contains(to.getWorld().getName(), to.getX(), to.getY(), to.getZ())) {
                activate(player, region.destination());
                return;
            }
        }
    }

    private void activate(Player player, String destination) {
        if (destination.startsWith("server:")) {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF("Connect");
            output.writeUTF(destination.substring("server:".length()));
            player.sendPluginMessage(context.plugin(), "BungeeCord", output.toByteArray());
            return;
        }
        if (destination.startsWith("velocity:")) {
            String[] parts = destination.substring("velocity:".length()).split(",");
            if (parts.length == 3) {
                player.setVelocity(new org.bukkit.util.Vector(parseDouble(parts[0], 0), parseDouble(parts[1], 0), parseDouble(parts[2], 0)));
            }
            return;
        }
        Optional<StoredLocation> warp = context.warps().get(destination);
        warp.flatMap(this::toLocation).ifPresent(player::teleportAsync);
    }

    private List<PortalRegion> loadRegions() {
        YamlConfiguration yaml = portalStore.load();
        ConfigurationSection section = yaml.getConfigurationSection("portals");
        if (section == null) {
            return List.of();
        }
        List<PortalRegion> regions = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection node = section.getConfigurationSection(key);
            if (node == null) {
                continue;
            }
            regions.add(new PortalRegion(
                    key,
                    node.getString("world", "world"),
                    node.getDouble("min.x"),
                    node.getDouble("min.y"),
                    node.getDouble("min.z"),
                    node.getDouble("max.x"),
                    node.getDouble("max.y"),
                    node.getDouble("max.z"),
                    node.getString("destination", "")
            ));
        }
        return regions;
    }

    private List<String> portalNames() {
        ConfigurationSection section = portalStore.load().getConfigurationSection("portals");
        return section == null ? List.of() : section.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private void drawParticles() {
        for (PortalRegion region : loadRegions()) {
            World world = Bukkit.getWorld(region.worldName());
            if (world == null) {
                continue;
            }
            for (double x = region.minX(); x <= region.maxX(); x += 0.75D) {
                world.spawnParticle(Particle.PORTAL, x, region.minY(), region.minZ(), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.PORTAL, x, region.maxY(), region.maxZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    private Optional<Location> toLocation(StoredLocation stored) {
        World world = Bukkit.getWorld(stored.worldName());
        return world == null ? Optional.empty() : Optional.of(stored.toLocation(world));
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() == second.getWorld()
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
