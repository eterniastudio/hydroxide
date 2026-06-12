package net.axther.hydroxide.modules.jail;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.StoredLocation;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class JailModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private HydroxideContext context;
    private YamlStore jailStore;
    private YamlStore punishmentStore;
    private final Map<UUID, JailSentence> jailed = new HashMap<>();
    private BukkitTask releaseTask;

    @Override
    public String id() {
        return "jail";
    }

    @Override
    public String displayName() {
        return "Jail";
    }

    @Override
    public String description() {
        return "Persistent punishment isolation with command, chat, and interaction restrictions.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.jailStore = new YamlStore(new File(context.plugin().getDataFolder(), "jails.yml"));
        this.punishmentStore = new YamlStore(new File(context.plugin().getDataFolder(), "punishments.yml"));
        loadSentences();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("setjail", this);
        context.commands().register("jail", this);
        context.commands().register("unjail", this);
        releaseTask = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::releaseExpired, 20L, 20L);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        if (releaseTask != null) {
            releaseTask.cancel();
        }
        saveSentences();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(java.util.Locale.ROOT)) {
            case "setjail" -> setJail(sender, label, args);
            case "jail" -> jail(sender, label, args);
            case "unjail" -> unjail(sender, label, args);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return switch (command.getName().toLowerCase(java.util.Locale.ROOT)) {
            case "jail" -> {
                if (args.length == 1) {
                    yield net.axther.hydroxide.commands.CompletionUtils.onlinePlayers(args[0]);
                }
                if (args.length == 2) {
                    yield CommandUtils.matching(args[1], jailNames());
                }
                if (args.length == 3) {
                    yield CommandUtils.matching(args[2], List.of("60", "300", "600", "3600", "86400"));
                }
                yield List.of();
            }
            case "unjail" -> args.length == 1
                    ? net.axther.hydroxide.commands.CompletionUtils.onlinePlayers(args[0])
                    : List.of();
            default -> List.of();
        };
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        JailSentence sentence = jailed.get(event.getPlayer().getUniqueId());
        if (sentence != null) {
            teleportToJail(event.getPlayer(), sentence.jailName());
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (jailed.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            context.send(event.getPlayer(), "<red>You cannot chat while jailed.");
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!jailed.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        String root = event.getMessage().split(" ")[0].toLowerCase(java.util.Locale.ROOT);
        List<String> whitelist = context.plugin().getConfig().getStringList("jail.allowed-commands");
        if (whitelist.stream().noneMatch(root::equalsIgnoreCase)) {
            event.setCancelled(true);
            context.send(event.getPlayer(), "<red>You cannot use that command while jailed.");
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        cancelIfJailed(event.getPlayer(), event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        cancelIfJailed(event.getPlayer(), event);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        cancelIfJailed(event.getPlayer(), event);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && jailed.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private boolean setJail(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.setjail")) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            context.send(sender, "<red>Only players can set jail cells.");
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <name>");
            return true;
        }
        YamlConfiguration yaml = jailStore.load();
        StoredLocation.from(player.getLocation()).writeTo(yaml.createSection("jails." + args[0].toLowerCase(java.util.Locale.ROOT)));
        jailStore.save(yaml);
        context.send(sender, "<green>Jail cell set.");
        return true;
    }

    private boolean jail(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.jail")) {
            return true;
        }
        if (args.length < 3) {
            context.send(sender, "<red>Usage: /" + label + " <player> <cell> <seconds> [reason]");
            return true;
        }
        Player target = CommandUtils.onlinePlayer(args[0]).orElse(null);
        if (target == null) {
            context.send(sender, "<red>That player is not online.");
            return true;
        }
        if (jailLocation(args[1]).isEmpty()) {
            context.send(sender, "<red>That jail cell does not exist.");
            return true;
        }
        long seconds = parseLong(args[2], 60L);
        UUID jailerId = sender instanceof Player player ? player.getUniqueId() : new UUID(0L, 0L);
        JailSentence sentence = new JailSentence(
                target.getUniqueId(),
                args[1].toLowerCase(java.util.Locale.ROOT),
                jailerId,
                args.length >= 4 ? CommandUtils.joinArgs(args, 3) : "No reason provided",
                Instant.now().plusSeconds(Math.max(1L, seconds)),
                StoredLocation.from(target.getLocation())
        );
        jailed.put(target.getUniqueId(), sentence);
        saveSentences();
        teleportToJail(target, sentence.jailName());
        context.send(sender, "<green>Jailed <white>" + target.getName() + "<green>.");
        return true;
    }

    private boolean unjail(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.unjail")) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <player>");
            return true;
        }
        Player target = CommandUtils.onlinePlayer(args[0]).orElse(null);
        UUID targetId = target != null ? target.getUniqueId() : null;
        if (targetId == null) {
            context.send(sender, "<red>That player is not online.");
            return true;
        }
        release(targetId);
        context.send(sender, "<green>Released <white>" + target.getName() + "<green>.");
        return true;
    }

    private void releaseExpired() {
        Instant now = Instant.now();
        List<UUID> expired = jailed.values().stream()
                .filter(sentence -> sentence.expired(now))
                .map(JailSentence::playerId)
                .toList();
        expired.forEach(this::release);
    }

    private void release(UUID playerId) {
        JailSentence sentence = jailed.remove(playerId);
        if (sentence == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && sentence.previousLocation() != null) {
            toLocation(sentence.previousLocation()).ifPresent(player::teleportAsync);
            context.send(player, "<green>You have been released from jail.");
        }
        saveSentences();
    }

    private void teleportToJail(Player player, String jailName) {
        jailLocation(jailName).flatMap(this::toLocation).ifPresent(player::teleportAsync);
    }

    private Optional<StoredLocation> jailLocation(String jailName) {
        return StoredLocation.readFrom(jailStore.load().getConfigurationSection("jails." + jailName.toLowerCase(java.util.Locale.ROOT)));
    }

    private List<String> jailNames() {
        ConfigurationSection section = jailStore.load().getConfigurationSection("jails");
        return section == null ? List.of() : section.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private Optional<Location> toLocation(StoredLocation stored) {
        World world = Bukkit.getWorld(stored.worldName());
        return world == null ? Optional.empty() : Optional.of(stored.toLocation(world));
    }

    private void loadSentences() {
        jailed.clear();
        YamlConfiguration yaml = punishmentStore.load();
        ConfigurationSection section = yaml.getConfigurationSection("jailed");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            ConfigurationSection node = section.getConfigurationSection(key);
            if (node == null) {
                continue;
            }
            jailed.put(playerId, new JailSentence(
                    playerId,
                    node.getString("jail", "default"),
                    UUID.fromString(node.getString("jailer", "00000000-0000-0000-0000-000000000000")),
                    node.getString("reason", "No reason provided"),
                    Instant.ofEpochMilli(node.getLong("release-at")),
                    StoredLocation.readFrom(node.getConfigurationSection("previous-location")).orElse(null)
            ));
        }
    }

    private void saveSentences() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (JailSentence sentence : jailed.values()) {
            String path = "jailed." + sentence.playerId();
            yaml.set(path + ".jail", sentence.jailName());
            yaml.set(path + ".jailer", sentence.jailerId().toString());
            yaml.set(path + ".reason", sentence.reason());
            yaml.set(path + ".release-at", sentence.releaseAt().toEpochMilli());
            if (sentence.previousLocation() != null) {
                sentence.previousLocation().writeTo(yaml.createSection(path + ".previous-location"));
            }
        }
        punishmentStore.save(yaml);
    }

    private void cancelIfJailed(Player player, org.bukkit.event.Cancellable event) {
        if (jailed.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private long parseLong(String input, long fallback) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
