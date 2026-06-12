package net.axther.hydroxide.modules.interaction;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InteractionModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private HydroxideContext context;
    private YamlStore store;
    private final Map<UUID, CommandBinding> pendingBlockBindings = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    @Override
    public String id() {
        return "interactions";
    }

    @Override
    public String displayName() {
        return "Interactive Bindings";
    }

    @Override
    public String description() {
        return "Command signs, block bindings, entity bindings, costs, and cooldowns.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core", "economy");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "bindings.yml"));
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("bindcommand", this);
        context.commands().register("bindentity", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.bindcommand")) {
            return true;
        }
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can create interaction bindings.");
            return true;
        }
        if (args.length < 4) {
            context.send(sender, "<red>Usage: /" + label + " <console|player> <cost> <cooldown> <command>");
            return true;
        }
        CommandBinding binding = new CommandBinding(
                args[0].equalsIgnoreCase("console") ? CommandBinding.ExecutionMode.CONSOLE : CommandBinding.ExecutionMode.PLAYER,
                CommandUtils.joinArgs(args, 3),
                parseDouble(args[1], 0.0D),
                Math.max(0, parseInt(args[2], 0))
        );
        if (command.getName().equalsIgnoreCase("bindentity")) {
            Entity entity = player.getNearbyEntities(5.0D, 5.0D, 5.0D).stream().findFirst().orElse(null);
            if (entity == null) {
                context.send(player, "<red>No nearby entity found.");
                return true;
            }
            entity.getPersistentDataContainer().set(key(), PersistentDataType.STRING, encode(binding));
            context.send(player, "<green>Bound command to nearby entity.");
            return true;
        }
        pendingBlockBindings.put(player.getUniqueId(), binding);
        context.send(player, "<green>Right-click a sign, button, pressure plate, or block to bind this command.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtils.matching(args[0], List.of("console", "player"));
        }
        if (args.length == 2) {
            return CommandUtils.matching(args[1], List.of("0", "1", "5", "10", "25", "100"));
        }
        if (args.length == 3) {
            return CommandUtils.matching(args[2], List.of("0", "5", "10", "30", "60", "300"));
        }
        return List.of();
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!context.text().plain(event.line(0)).equalsIgnoreCase("[Command]")) {
            return;
        }
        if (!event.getPlayer().hasPermission("hydroxide.command.bindcommand")) {
            event.line(0, context.text().format("<red>[Denied]"));
            return;
        }
        event.line(0, context.text().format("<#44CCFF>[Command]"));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        CommandBinding pending = pendingBlockBindings.remove(event.getPlayer().getUniqueId());
        if (pending != null) {
            saveBlockBinding(block, pending);
            context.send(event.getPlayer(), "<green>Bound command to block.");
            event.setCancelled(true);
            return;
        }
        Optional<CommandBinding> binding = bindingFromBlock(block);
        if (binding.isPresent()) {
            event.setCancelled(true);
            execute(event.getPlayer(), blockKey(block), binding.get());
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        String encoded = event.getRightClicked().getPersistentDataContainer().get(key(), PersistentDataType.STRING);
        if (encoded == null) {
            return;
        }
        event.setCancelled(true);
        execute(event.getPlayer(), "entity:" + event.getRightClicked().getUniqueId(), decode(encoded));
    }

    private Optional<CommandBinding> bindingFromBlock(Block block) {
        if (block.getState() instanceof Sign sign) {
            List<String> lines = List.of(
                    context.text().plain(sign.getSide(Side.FRONT).line(0)),
                    context.text().plain(sign.getSide(Side.FRONT).line(1)),
                    context.text().plain(sign.getSide(Side.FRONT).line(2)),
                    context.text().plain(sign.getSide(Side.FRONT).line(3))
            );
            Optional<CommandBinding> signBinding = CommandBinding.fromSignLines(lines);
            if (signBinding.isPresent()) {
                return signBinding;
            }
        }
        YamlConfiguration yaml = store.load();
        ConfigurationSection section = yaml.getConfigurationSection("blocks." + blockKey(block));
        if (section == null) {
            return Optional.empty();
        }
        return Optional.of(new CommandBinding(
                CommandBinding.ExecutionMode.valueOf(section.getString("mode", "PLAYER")),
                section.getString("command", ""),
                section.getDouble("cost"),
                section.getInt("cooldown")
        ));
    }

    private void execute(Player player, String targetKey, CommandBinding binding) {
        long now = System.currentTimeMillis();
        String cooldownKey = player.getUniqueId() + ":" + targetKey;
        long last = cooldowns.getOrDefault(cooldownKey, 0L);
        if (!binding.readyAt(last, now)) {
            context.send(player, "<red>That interaction is on cooldown.");
            return;
        }
        if (binding.cost() > 0.0D) {
            EconomyResponse response = context.services().economy()
                    .map(economy -> economy.withdrawPlayer(player, binding.cost()))
                    .orElse(null);
            if (response == null || !response.transactionSuccess()) {
                context.send(player, "<red>You cannot afford that interaction.");
                return;
            }
        }
        String rendered = binding.command().replace("{player}", player.getName());
        if (binding.mode() == CommandBinding.ExecutionMode.CONSOLE) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rendered);
        } else {
            player.performCommand(rendered.startsWith("/") ? rendered.substring(1) : rendered);
        }
        cooldowns.put(cooldownKey, now);
    }

    private void saveBlockBinding(Block block, CommandBinding binding) {
        YamlConfiguration yaml = store.load();
        String path = "blocks." + blockKey(block);
        yaml.set(path + ".mode", binding.mode().name());
        yaml.set(path + ".command", binding.command());
        yaml.set(path + ".cost", binding.cost());
        yaml.set(path + ".cooldown", binding.cooldownSeconds());
        store.save(yaml);
    }

    private String blockKey(Block block) {
        return block.getWorld().getName() + "_" + block.getX() + "_" + block.getY() + "_" + block.getZ();
    }

    private NamespacedKey key() {
        return new NamespacedKey(context.plugin(), "interaction_binding");
    }

    private String encode(CommandBinding binding) {
        String payload = binding.mode().name() + "|" + binding.cost() + "|" + binding.cooldownSeconds() + "|"
                + Base64.getEncoder().encodeToString(binding.command().getBytes(StandardCharsets.UTF_8));
        return payload;
    }

    private CommandBinding decode(String payload) {
        String[] parts = payload.split("\\|", 4);
        if (parts.length < 4) {
            return new CommandBinding(CommandBinding.ExecutionMode.PLAYER, "", 0.0D, 0);
        }
        return new CommandBinding(
                CommandBinding.ExecutionMode.valueOf(parts[0]),
                new String(Base64.getDecoder().decode(parts[3]), StandardCharsets.UTF_8),
                parseDouble(parts[1], 0.0D),
                parseInt(parts[2], 0)
        );
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
