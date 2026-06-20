package net.axther.hydroxide.modules.interaction;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
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

public final class InteractionModule implements HydroModule, Listener {

    private static final List<String> EXECUTION_MODES = List.of("console", "player");
    private static final List<String> MONEY_SAMPLES = List.of("0", "1", "5", "10", "25", "100");
    private static final List<String> COOLDOWN_SAMPLES = List.of("0", "5", "10", "30", "60", "300");

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
        context.commands().register("bindcommand", bindCommand("bindcommand", false));
        context.commands().register("bindentity", bindCommand("bindentity", true));
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    private CommandService bindCommand(String name, boolean entityBinding) {
        return new CommandService(HydroCommand.builder(name)
                .permission("hydroxide.command.bindcommand")
                .playerOnly(true)
                .usage("/{label} <console|player> <cost> <cooldown> <command>")
                .executor(ctx -> bind((Player) ctx.sender(), ctx.label(), ctx.arguments(), entityBinding))
                .completer(this::bindCompletions)
                .build(), context.messages());
    }

    private List<String> bindCompletions(CommandContext ctx) {
        return switch (ctx.arguments().size()) {
            case 1 -> CommandUtils.matching(ctx.argument(0), EXECUTION_MODES);
            case 2 -> CommandUtils.matching(ctx.argument(1), MONEY_SAMPLES);
            case 3 -> CommandUtils.matching(ctx.argument(2), COOLDOWN_SAMPLES);
            default -> List.of();
        };
    }

    private void bind(Player player, String label, List<String> args, boolean entityBinding) {
        if (args.size() < 4) {
            context.message(player, "interactions.bind.usage", Map.of("label", label));
            return;
        }
        CommandBinding binding = new CommandBinding(
                args.get(0).equalsIgnoreCase("console") ? CommandBinding.ExecutionMode.CONSOLE : CommandBinding.ExecutionMode.PLAYER,
                joinArgs(args, 3),
                parseDouble(args.get(1), 0.0D),
                Math.max(0, parseInt(args.get(2), 0))
        );
        if (entityBinding) {
            Entity entity = player.getNearbyEntities(5.0D, 5.0D, 5.0D).stream().findFirst().orElse(null);
            if (entity == null) {
                context.message(player, "interactions.bind.entity-missing", Map.of());
                return;
            }
            entity.getPersistentDataContainer().set(key(), PersistentDataType.STRING, encode(binding));
            context.message(player, "interactions.bind.entity-bound", Map.of());
            return;
        }
        pendingBlockBindings.put(player.getUniqueId(), binding);
        context.message(player, "interactions.bind.block-pending", Map.of());
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!context.text().plain(event.line(0)).equalsIgnoreCase("[Command]")) {
            return;
        }
        if (!event.getPlayer().hasPermission("hydroxide.command.bindcommand")) {
            event.line(0, context.messages().component("interactions.sign.denied-header", Map.of()));
            return;
        }
        event.line(0, context.messages().component("interactions.sign.command-header", Map.of()));
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
            context.message(event.getPlayer(), "interactions.bind.block-bound", Map.of());
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
            context.message(player, "interactions.execute.cooldown", Map.of());
            return;
        }
        if (binding.cost() > 0.0D) {
            EconomyResponse response = context.services().economy()
                    .map(economy -> economy.withdrawPlayer(player, binding.cost()))
                    .orElse(null);
            if (response == null || !response.transactionSuccess()) {
                context.message(player, "interactions.execute.cannot-afford", Map.of("cost", binding.cost()));
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

    private String joinArgs(List<String> args, int startIndex) {
        if (startIndex >= args.size()) {
            return "";
        }
        return String.join(" ", args.subList(startIndex, args.size()));
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
