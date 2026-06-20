package net.axther.hydroxide.modules.builder;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class BuilderModule implements HydroModule, Listener, BuilderService {

    private static final List<String> SEL_ACTIONS = List.of("clear", "contract", "expand", "info");
    private static final List<String> BRUSH_ACTIONS = List.of("cylinder", "none", "replace", "smooth", "sphere");

    private HydroxideContext context;
    private YamlStore store;
    private final Map<UUID, BuilderProfile> profiles = new HashMap<>();
    private final Map<UUID, SelectionState> selections = new HashMap<>();
    private final Map<UUID, Clipboard> clipboards = new HashMap<>();
    private final Map<UUID, UndoHistory> histories = new HashMap<>();
    private final Map<UUID, Long> brushCooldowns = new HashMap<>();
    private final Set<UUID> activeEdits = new HashSet<>();

    @Override
    public String id() {
        return "builder";
    }

    @Override
    public String displayName() {
        return "Builder Utilities";
    }

    @Override
    public String description() {
        return "Build mode, bounded block-editing tools, brushes, selection, copy/paste, and undo.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "builder.yml"));
        seedDefaults();
        context.services().builderService(this);
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("build", command("build", "hydroxide.builder.mode", "/{label} [player] [on|off|toggle]", false,
                ctx -> build(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new))));
        context.commands().register("buildstatus", command("buildstatus", "hydroxide.builder.mode", "/{label} [player]", false,
                ctx -> buildStatus(ctx.sender(), ctx.arguments().toArray(String[]::new))));
        context.commands().register("breaktoggle", command("breaktoggle", "hydroxide.builder.mode", "/{label}",
                ctx -> toggleOwn(ctx.sender(), Toggle.BREAK)));
        context.commands().register("placetoggle", command("placetoggle", "hydroxide.builder.mode", "/{label}",
                ctx -> toggleOwn(ctx.sender(), Toggle.PLACE)));
        context.commands().register("pickblock", command("pickblock", "hydroxide.builder.pickblock", "/{label}",
                ctx -> pickBlock(ctx.sender())));
        context.commands().register("blockcycling", command("blockcycling", "hydroxide.builder.blockcycling", "/{label} [forward|backward]",
                ctx -> blockCycling(ctx.sender(), ctx.label(), ctx.arguments())));
        context.commands().register("wand", command("wand", "hydroxide.builder.wand", "/{label}",
                ctx -> wand(ctx.sender())));
        context.commands().register("pos1", command("pos1", "hydroxide.builder.selection", "/{label}",
                ctx -> setPosition(ctx.sender(), true)));
        context.commands().register("pos2", command("pos2", "hydroxide.builder.selection", "/{label}",
                ctx -> setPosition(ctx.sender(), false)));
        context.commands().register("sel", command("sel", "hydroxide.builder.selection", "/{label} <clear|expand|contract|info>",
                ctx -> selectionCommand(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new))));
        context.commands().register("setblockarea", command("setblockarea", "hydroxide.builder.edit", "/{label} <block>",
                ctx -> setArea(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new))));
        context.commands().register("replacearea", command("replacearea", "hydroxide.builder.edit", "/{label} <from> <to>",
                ctx -> replaceArea(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new))));
        context.commands().register("walls", command("walls", "hydroxide.builder.edit", "/{label} <block>",
                ctx -> walls(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new))));
        context.commands().register("hollow", command("hollow", "hydroxide.builder.edit", "/{label} <block>",
                ctx -> hollow(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new))));
        context.commands().register("copyarea", command("copyarea", "hydroxide.builder.copy", "/{label}",
                ctx -> copyArea(ctx.sender())));
        context.commands().register("pastearea", command("pastearea", "hydroxide.builder.paste", "/{label}",
                ctx -> pasteArea(ctx.sender())));
        context.commands().register("undo", command("undo", "hydroxide.builder.undo", "/{label} [steps]",
                ctx -> history(ctx.sender(), ctx.arguments().toArray(String[]::new), true)));
        context.commands().register("redo", command("redo", "hydroxide.builder.redo", "/{label} [steps]",
                ctx -> history(ctx.sender(), ctx.arguments().toArray(String[]::new), false)));
        context.commands().register("brush", command("brush", "hydroxide.builder.brush", "/{label} <sphere|cylinder|replace|smooth|none> ...",
                ctx -> brush(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new))));
        context.commands().register("fillnear", command("fillnear", "hydroxide.builder.brush", "/{label} <block> <radius>",
                ctx -> fillNear(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new))));
        context.commands().register("drainnear", command("drainnear", "hydroxide.builder.brush", "/{label} [radius]",
                ctx -> drainNear(ctx.sender(), ctx.arguments().toArray(String[]::new))));
        context.commands().register("fixlight", command("fixlight", "hydroxide.builder.brush", "/{label} [radius]",
                ctx -> fixLight(ctx.sender(), ctx.arguments().toArray(String[]::new))));
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        saveAll();
        context.services().clearBuilderService(this);
    }

    private CommandService command(String name, String permission, String usage, HydroCommand.HydroCommandExecutor executor) {
        return command(name, permission, usage, true, executor);
    }

    private CommandService command(String name, String permission, String usage, boolean playerOnly, HydroCommand.HydroCommandExecutor executor) {
        return new CommandService(HydroCommand.builder(name)
                .permission(permission)
                .playerOnly(playerOnly)
                .usage(usage)
                .executor(executor)
                .completer(ctx -> completions(name, ctx))
                .build(), context.messages());
    }

    private List<String> completions(String name, CommandContext ctx) {
        String[] args = ctx.arguments().toArray(String[]::new);
        if (name.equals("build")) {
            if (args.length == 1) {
                List<String> values = new ArrayList<>(List.of("off", "on", "toggle"));
                values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                return CommandUtils.matching(args[0], values);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
                return CommandUtils.matching(args[1], Toggle.keys());
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("toggle")) {
                return CommandUtils.matching(args[2], List.of("off", "on"));
            }
            if (args.length == 2) {
                return CommandUtils.matching(args[1], List.of("off", "on", "toggle"));
            }
        }
        if (name.equals("buildstatus") && args.length == 1) {
            return CompletionUtils.onlinePlayers(args[0]);
        }
        if (name.equals("blockcycling") && args.length == 1) {
            return CommandUtils.matching(args[0], List.of("forward", "backward", "next", "prev"));
        }
        if (name.equals("sel") && args.length == 1) {
            return CommandUtils.matching(args[0], SEL_ACTIONS);
        }
        if (List.of("setblockarea", "walls", "hollow", "fillnear").contains(name) && args.length == 1) {
            return CompletionUtils.materials(args[0]);
        }
        if (name.equals("replacearea") && args.length <= 2) {
            return CompletionUtils.materials(args[args.length - 1]);
        }
        if (name.equals("brush")) {
            if (args.length == 1) {
                return CommandUtils.matching(args[0], BRUSH_ACTIONS);
            }
            if ((args[0].equalsIgnoreCase("sphere") || args[0].equalsIgnoreCase("cylinder") || args[0].equalsIgnoreCase("fill")) && args.length == 2) {
                return CompletionUtils.materials(args[1]);
            }
            if (args[0].equalsIgnoreCase("replace") && (args.length == 2 || args.length == 3)) {
                return CompletionUtils.materials(args[args.length - 1]);
            }
        }
        if ((name.equals("undo") || name.equals("redo") || name.equals("drainnear") || name.equals("fixlight")) && args.length == 1) {
            return CompletionUtils.integerRange(args[0], 1, 10);
        }
        return List.of();
    }

    @Override
    public boolean buildMode(Player player) {
        return profile(player.getUniqueId()).buildMode();
    }

    @Override
    public boolean canBypassHydroxideProtection(Player player) {
        return store.load().getBoolean("allow-hydroxide-protection-bypass", true)
                && player.hasPermission("hydroxide.builder.bypass")
                && buildMode(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        BuilderProfile profile = profile(event.getPlayer().getUniqueId());
        if (requiresBuildMode(event.getBlock().getWorld()) && !profile.buildMode()) {
            event.setCancelled(true);
            context.message(event.getPlayer(), "builder.protection.build-mode-required", Map.of());
            return;
        }
        if (shouldCancelForToggle(event.getPlayer(), Toggle.BREAK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        BuilderProfile profile = profile(event.getPlayer().getUniqueId());
        if (requiresBuildMode(event.getBlock().getWorld()) && !profile.buildMode()) {
            event.setCancelled(true);
            context.message(event.getPlayer(), "builder.protection.build-mode-required", Map.of());
            return;
        }
        if (shouldCancelForToggle(event.getPlayer(), Toggle.PLACE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (shouldCancelForToggle(event.getPlayer(), Toggle.LIQUID)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (player == null && event.getIgnitingEntity() instanceof Player igniter) {
            player = igniter;
        }
        if (player != null && shouldCancelForToggle(player, Toggle.FIRE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() != null
                && event.getEntity() instanceof ItemFrame
                && shouldCancelForToggle(event.getPlayer(), Toggle.ITEM_FRAME)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Player player = playerFrom(event.getRemover());
        if (player != null
                && event.getEntity() instanceof ItemFrame
                && shouldCancelForToggle(player, Toggle.ITEM_FRAME)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (shouldCancelForToggle(event.getPlayer(), Toggle.ARMOR_STAND)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Entity target = event.getRightClicked();
        Toggle toggle = entityToggle(target);
        if (toggle != null && shouldCancelForToggle(event.getPlayer(), toggle)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player player = playerFrom(event.getDamager());
        Toggle toggle = entityToggle(event.getEntity());
        if (player != null && toggle != null && shouldCancelForToggle(player, toggle)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isWand(item) && event.getClickedBlock() != null) {
            event.setCancelled(true);
            setSelection(player, event.getClickedBlock(), event.getAction() == Action.LEFT_CLICK_BLOCK);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            BrushBinding binding = brushBinding(item).orElse(null);
            if (binding != null) {
                event.setCancelled(true);
                applyBrush(player, event.getClickedBlock(), binding);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (store.load().getBoolean("persist", true)) {
            saveProfile(event.getPlayer().getUniqueId());
        }
        selections.remove(event.getPlayer().getUniqueId());
        activeEdits.remove(event.getPlayer().getUniqueId());
    }

    private boolean build(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.builder.mode")) {
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("toggle")) {
            return toggleSetting(sender, label, args);
        }
        Player target;
        String mode;
        if (args.length == 0) {
            target = requirePlayer(sender);
            mode = "toggle";
        } else if (args.length == 1 && List.of("on", "off", "toggle").contains(args[0].toLowerCase(Locale.ROOT))) {
            target = requirePlayer(sender);
            mode = args[0];
        } else {
            if (!sender.hasPermission("hydroxide.builder.mode.others")) {
                context.message(sender, "builder.mode.others-denied", Map.of());
                return true;
            }
            target = CommandUtils.onlinePlayer(args[0]).orElse(null);
            mode = args.length > 1 ? args[1] : "toggle";
        }
        if (target == null) {
            context.message(sender, "builder.mode.usage", Map.of("label", label));
            return true;
        }
        BuilderProfile profile = profile(target.getUniqueId());
        boolean enabled = switch (mode.toLowerCase(Locale.ROOT)) {
            case "on" -> true;
            case "off" -> false;
            default -> !profile.buildMode();
        };
        profile.buildMode(enabled);
        saveProfile(target.getUniqueId());
        action(target, enabled ? "builder.mode.enabled-action" : "builder.mode.disabled-action", Map.of());
        context.message(sender, "builder.mode.updated", Map.of("target", target.getName(), "state", state(enabled)));
        return true;
    }

    private boolean buildStatus(CommandSender sender, String[] args) {
        Player target = args.length == 0 ? requirePlayer(sender) : CommandUtils.onlinePlayer(args[0]).orElse(null);
        if (target == null) {
            context.message(sender, "builder.mode.player-offline", Map.of("target", args.length == 0 ? "" : args[0]));
            return true;
        }
        BuilderProfile profile = profile(target.getUniqueId());
        String toggles = Toggle.keys().stream()
                .map(key -> key + "=" + profile.enabled(Toggle.fromKey(key).orElseThrow()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        context.message(sender, "builder.mode.status", Map.of("target", target.getName(), "mode", profile.buildMode(), "toggles", toggles));
        return true;
    }

    private boolean toggleOwn(CommandSender sender, Toggle toggle) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.mode")) {
            return true;
        }
        BuilderProfile profile = profile(player.getUniqueId());
        boolean enabled = !profile.enabled(toggle);
        profile.set(toggle, enabled);
        saveProfile(player.getUniqueId());
        action(player, "builder.toggle.updated-action", Map.of("toggle", toggle.display(), "state", state(enabled)));
        return true;
    }

    private boolean toggleSetting(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        Toggle toggle = Toggle.fromKey(args[1]).orElse(null);
        if (toggle == null) {
            context.message(player, "builder.toggle.usage", Map.of("label", label, "toggles", String.join("|", Toggle.keys())));
            return true;
        }
        BuilderProfile profile = profile(player.getUniqueId());
        boolean enabled = args.length >= 3 ? args[2].equalsIgnoreCase("on") : !profile.enabled(toggle);
        profile.set(toggle, enabled);
        saveProfile(player.getUniqueId());
        action(player, "builder.toggle.updated-action", Map.of("toggle", toggle.display(), "state", state(enabled)));
        return true;
    }

    private boolean pickBlock(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.pickblock")) {
            return true;
        }
        Block block = player.getTargetBlockExact(100);
        if (block == null) {
            context.message(player, "builder.target-block-required", Map.of());
            return true;
        }
        player.getInventory().addItem(new ItemStack(block.getType()));
        context.message(player, "builder.pickblock.picked", Map.of("block", block.getType().key().asString()));
        return true;
    }

    private boolean blockCycling(CommandSender sender, String label, List<String> args) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.blockcycling")) {
            return true;
        }
        Optional<BlockCyclingCommandParser.Request> request = BlockCyclingCommandParser.parse(args);
        if (request.isEmpty()) {
            context.message(player, "builder.blockcycling.usage", Map.of("label", label));
            return true;
        }
        Block block = player.getTargetBlockExact(100);
        if (block == null) {
            context.message(player, "builder.target-block-required", Map.of());
            return true;
        }
        Optional<BlockData> cycled = BlockDataCycler.cycle(block.getBlockData(), request.orElseThrow().direction());
        if (cycled.isEmpty()) {
            context.message(player, "builder.blockcycling.unsupported", Map.of("block", block.getType().key().asString()));
            return true;
        }
        BlockData next = cycled.orElseThrow();
        block.setBlockData(next, true);
        context.message(player, "builder.blockcycling.changed", Map.of(
                "block", block.getType().key().asString(),
                "state", next.getAsString()
        ));
        return true;
    }

    private boolean wand(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.wand")) {
            return true;
        }
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.displayName(context.messages().component("builder.wand.name", Map.of()));
        meta.getPersistentDataContainer().set(key("wand"), PersistentDataType.BOOLEAN, true);
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);
        context.message(player, "builder.wand.added", Map.of());
        return true;
    }

    private boolean setPosition(CommandSender sender, boolean first) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.selection")) {
            return true;
        }
        Block block = player.getTargetBlockExact(100);
        setSelection(player, block == null ? player.getLocation().getBlock() : block, first);
        return true;
    }

    private boolean selectionCommand(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.selection")) {
            return true;
        }
        if (args.length == 0) {
            context.message(player, "builder.selection.usage", Map.of("label", label));
            return true;
        }
        SelectionState state = selections.get(player.getUniqueId());
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "clear" -> {
                selections.remove(player.getUniqueId());
                context.message(player, "builder.selection.cleared", Map.of());
            }
            case "info" -> {
                if (state == null || state.selection().isEmpty()) {
                    context.message(player, "builder.selection.incomplete", Map.of());
                } else {
                    context.message(player, "builder.selection.volume", Map.of("volume", state.selection().get().volume()));
                }
            }
            case "expand", "contract" -> {
                if (state == null || state.selection().isEmpty()) {
                    context.message(player, "builder.selection.incomplete", Map.of());
                    return true;
                }
                int amount = args[0].equalsIgnoreCase("expand") ? 1 : -1;
                selections.put(player.getUniqueId(), state.expand(amount));
                context.message(player, "builder.selection.updated", Map.of());
            }
            default -> context.message(player, "builder.selection.usage", Map.of("label", label));
        }
        return true;
    }

    private boolean setArea(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 1) {
            context.message(sender, "builder.edit.usage-block", Map.of("label", label));
            return true;
        }
        Material material = BuilderMaterialResolver.resolveBlock(args[0]).orElse(null);
        if (material == null) {
            context.message(player, "builder.edit.unknown-block", Map.of("block", args[0]));
            return true;
        }
        return selectionPlan(player, selection -> plan(selection, (world, x, y, z) -> material, false));
    }

    private boolean replaceArea(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 2) {
            context.message(sender, "builder.edit.usage-replace", Map.of("label", label));
            return true;
        }
        Material from = BuilderMaterialResolver.resolveBlock(args[0]).orElse(null);
        Material to = BuilderMaterialResolver.resolveBlock(args[1]).orElse(null);
        if (from == null || to == null) {
            context.message(player, "builder.edit.unknown-block", Map.of("block", from == null ? args[0] : args[1]));
            return true;
        }
        return selectionPlan(player, selection -> plan(selection, (world, x, y, z) -> world.getBlockAt(x, y, z).getType() == from ? to : null, false));
    }

    private boolean walls(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 1) {
            context.message(sender, "builder.edit.usage-block", Map.of("label", label));
            return true;
        }
        Material material = BuilderMaterialResolver.resolveBlock(args[0]).orElse(null);
        if (material == null) {
            context.message(player, "builder.edit.unknown-block", Map.of("block", args[0]));
            return true;
        }
        return selectionPlan(player, selection -> plan(selection, (world, x, y, z) -> isWall(selection, x, z) ? material : null, false));
    }

    private boolean hollow(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 1) {
            context.message(sender, "builder.edit.usage-block", Map.of("label", label));
            return true;
        }
        Material material = BuilderMaterialResolver.resolveBlock(args[0]).orElse(null);
        if (material == null) {
            context.message(player, "builder.edit.unknown-block", Map.of("block", args[0]));
            return true;
        }
        return selectionPlan(player, selection -> plan(selection, (world, x, y, z) -> isShell(selection, x, y, z) ? material : Material.AIR, false));
    }

    private boolean copyArea(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.copy")) {
            return true;
        }
        CuboidSelection selection = requireSelection(player);
        if (selection == null) {
            return true;
        }
        World world = Bukkit.getWorld(selection.worldName());
        if (world == null) {
            context.message(player, "builder.edit.world-unloaded", Map.of("world", selection.worldName()));
            return true;
        }
        BlockVector3i min = selection.min();
        List<ClipboardBlock> blocks = new ArrayList<>();
        iterate(selection, (x, y, z) -> blocks.add(new ClipboardBlock(x - min.x(), y - min.y(), z - min.z(), world.getBlockAt(x, y, z).getType())));
        clipboards.put(player.getUniqueId(), new Clipboard(blocks));
        context.message(player, "builder.clipboard.copied", Map.of("blocks", blocks.size()));
        return true;
    }

    private boolean pasteArea(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.paste")) {
            return true;
        }
        Clipboard clipboard = clipboards.get(player.getUniqueId());
        if (clipboard == null) {
            context.message(player, "builder.clipboard.empty", Map.of());
            return true;
        }
        Block origin = player.getLocation().getBlock();
        List<BlockChange> changes = new ArrayList<>();
        for (ClipboardBlock block : clipboard.blocks()) {
            Block target = origin.getWorld().getBlockAt(origin.getX() + block.x(), origin.getY() + block.y(), origin.getZ() + block.z());
            changes.add(new BlockChange(new BlockVector3i(target.getX(), target.getY(), target.getZ()), target.getType(), block.material()));
        }
        execute(player, origin.getWorld(), new BlockEditPlan(changes), true);
        return true;
    }

    private boolean history(CommandSender sender, String[] args, boolean undo) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, undo ? "hydroxide.builder.undo" : "hydroxide.builder.redo")) {
            return true;
        }
        int steps = args.length > 0 ? parseInt(args[0], 1) : 1;
        UndoHistory history = history(player.getUniqueId());
        for (int i = 0; i < Math.max(1, steps); i++) {
            Optional<BlockEditPlan> plan = undo ? history.undo(Instant.now()) : history.redo(Instant.now());
            if (plan.isEmpty()) {
                context.message(player, undo ? "builder.history.undo-empty" : "builder.history.redo-empty", Map.of());
                return true;
            }
            execute(player, player.getWorld(), undo ? plan.get().inverse() : plan.get(), false);
        }
        return true;
    }

    private boolean brush(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.brush")) {
            return true;
        }
        if (args.length == 0) {
            context.message(player, "builder.brush.usage", Map.of("label", label));
            return true;
        }
        if (args[0].equalsIgnoreCase("none")) {
            setBrush(player, null);
            context.message(player, "builder.brush.cleared", Map.of());
            return true;
        }
        BrushBinding binding = BrushBinding.parse(args).orElse(null);
        if (binding == null) {
            context.message(player, "builder.brush.invalid", Map.of());
            return true;
        }
        BrushLimits limits = brushLimits(player);
        if (binding.radius() > limits.maxRadius()) {
            binding = binding.withRadius(limits.maxRadius());
        }
        setBrush(player, binding);
        context.message(player, "builder.brush.bound", Map.of());
        return true;
    }

    private boolean fillNear(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 2) {
            context.message(sender, "builder.near.fill-usage", Map.of("label", label));
            return true;
        }
        Material material = BuilderMaterialResolver.resolveBlock(args[0]).orElse(null);
        int radius = brushLimits(player).clampRadius(parseInt(args[1], 3));
        if (material == null) {
            context.message(player, "builder.edit.unknown-block", Map.of("block", args[0]));
            return true;
        }
        Block center = player.getLocation().getBlock();
        execute(player, center.getWorld(), spherePlan(center, radius, block -> block.getType() == Material.AIR ? material : null), true);
        return true;
    }

    private boolean drainNear(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        int radius = brushLimits(player).clampRadius(args.length > 0 ? parseInt(args[0], 3) : 3);
        Block center = player.getLocation().getBlock();
        execute(player, center.getWorld(), spherePlan(center, radius, block ->
                block.getType() == Material.WATER || block.getType() == Material.LAVA ? Material.AIR : null), true);
        return true;
    }

    private boolean fixLight(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        int radius = brushLimits(player).clampRadius(args.length > 0 ? parseInt(args[0], 3) : 3);
        int chunks = Math.max(1, ((radius * 2) / 16) + 1);
        context.message(player, "builder.near.fixlight-queued", Map.of("chunks", chunks, "radius", radius));
        return true;
    }

    private boolean selectionPlan(Player player, java.util.function.Function<CuboidSelection, BlockEditPlan> planner) {
        CuboidSelection selection = requireSelection(player);
        if (selection == null || !context.requirePermission(player, "hydroxide.builder.edit")) {
            return true;
        }
        if (!selection.withinLimit(blockLimit(player))) {
            context.message(player, "builder.edit.selection-too-large", Map.of("limit", blockLimit(player)));
            return true;
        }
        World world = Bukkit.getWorld(selection.worldName());
        if (world == null) {
            context.message(player, "builder.edit.world-unloaded", Map.of("world", selection.worldName()));
            return true;
        }
        execute(player, world, planner.apply(selection), true);
        return true;
    }

    private BlockEditPlan plan(CuboidSelection selection, MaterialProvider provider, boolean includeNulls) {
        World world = Bukkit.getWorld(selection.worldName());
        if (world == null) {
            return new BlockEditPlan(List.of());
        }
        List<BlockChange> changes = new ArrayList<>();
        iterate(selection, (x, y, z) -> {
            Material after = provider.material(world, x, y, z);
            if (after == null && !includeNulls) {
                return;
            }
            Block block = world.getBlockAt(x, y, z);
            if (after != null && block.getType() != after) {
                changes.add(new BlockChange(new BlockVector3i(x, y, z), block.getType(), after));
            }
        });
        return new BlockEditPlan(changes);
    }

    private BlockEditPlan spherePlan(Block center, int radius, java.util.function.Function<Block, Material> provider) {
        List<BlockChange> changes = new ArrayList<>();
        int radiusSquared = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSquared) {
                        continue;
                    }
                    Block block = center.getWorld().getBlockAt(center.getX() + x, center.getY() + y, center.getZ() + z);
                    Material after = provider.apply(block);
                    if (after != null && block.getType() != after) {
                        changes.add(new BlockChange(new BlockVector3i(block.getX(), block.getY(), block.getZ()), block.getType(), after));
                    }
                }
            }
        }
        return new BlockEditPlan(changes);
    }

    private void execute(Player player, World world, BlockEditPlan plan, boolean recordUndo) {
        if (plan.changes().isEmpty()) {
            context.message(player, "builder.edit.no-changes", Map.of());
            return;
        }
        if (plan.changes().size() > blockLimit(player)) {
            context.message(player, "builder.edit.limit-exceeded", Map.of("limit", blockLimit(player), "blocks", plan.changes().size()));
            return;
        }
        if (activeEdits.contains(player.getUniqueId())) {
            context.message(player, "builder.edit.already-running", Map.of());
            return;
        }
        activeEdits.add(player.getUniqueId());
        List<List<BlockChange>> batches = plan.batches(store.load().getInt("batch-size", 250));
        runBatch(player, world, batches, 0, () -> {
            activeEdits.remove(player.getUniqueId());
            if (recordUndo) {
                history(player.getUniqueId()).record(plan, Instant.now());
            }
            action(player, "builder.edit.complete-action", Map.of("blocks", plan.changes().size()));
        });
    }

    private void runBatch(Player player, World world, List<List<BlockChange>> batches, int index, Runnable done) {
        if (!player.isOnline() || index >= batches.size()) {
            done.run();
            return;
        }
        for (BlockChange change : batches.get(index)) {
            world.getBlockAt(change.position().x(), change.position().y(), change.position().z()).setType(change.after(), false);
        }
        action(player, "builder.edit.progress-action", Map.of("current", index + 1, "total", batches.size()));
        BukkitTask ignored = Bukkit.getScheduler().runTaskLater(context.plugin(), () -> runBatch(player, world, batches, index + 1, done), 1L);
    }

    private void applyBrush(Player player, Block target, BrushBinding binding) {
        long now = System.currentTimeMillis();
        long readyAt = brushCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            action(player, "builder.brush.cooldown-action", Map.of());
            return;
        }
        brushCooldowns.put(player.getUniqueId(), now + store.load().getLong("brush.cooldown-ms", 500L));
        BrushLimits limits = brushLimits(player);
        if (!limits.withinBlockLimit((int) Math.min(Integer.MAX_VALUE, Math.round((4.0D / 3.0D) * Math.PI * Math.pow(binding.radius(), 3))))) {
            context.message(player, "builder.brush.limit-exceeded", Map.of("limit", blockLimit(player)));
            return;
        }
        BlockEditPlan plan = switch (binding.type()) {
            case SPHERE -> spherePlan(target, binding.radius(), block -> binding.to());
            case REPLACE -> spherePlan(target, binding.radius(), block -> block.getType() == binding.from() ? binding.to() : null);
            case CYLINDER -> cylinderPlan(target, binding.radius(), binding.height(), binding.to());
            case SMOOTH -> smoothPlan(target, binding.radius());
        };
        execute(player, target.getWorld(), plan, true);
    }

    private BlockEditPlan cylinderPlan(Block center, int radius, int height, Material material) {
        List<BlockChange> changes = new ArrayList<>();
        int radiusSquared = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radiusSquared) {
                    continue;
                }
                for (int y = 0; y < height; y++) {
                    Block block = center.getWorld().getBlockAt(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (block.getType() != material) {
                        changes.add(new BlockChange(new BlockVector3i(block.getX(), block.getY(), block.getZ()), block.getType(), material));
                    }
                }
            }
        }
        return new BlockEditPlan(changes);
    }

    private BlockEditPlan smoothPlan(Block center, int radius) {
        List<BlockChange> changes = new ArrayList<>();
        int radiusSquared = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSquared) {
                        continue;
                    }
                    Block block = center.getWorld().getBlockAt(center.getX() + x, center.getY() + y, center.getZ() + z);
                    Material current = block.getType();
                    if (current.isAir()) {
                        continue;
                    }
                    Material majority = majorityNeighbor(block).orElse(null);
                    if (majority != null && majority != current) {
                        changes.add(new BlockChange(new BlockVector3i(block.getX(), block.getY(), block.getZ()), current, majority));
                    }
                }
            }
        }
        return new BlockEditPlan(changes);
    }

    private Optional<Material> majorityNeighbor(Block block) {
        Map<Material, Integer> counts = new HashMap<>();
        for (int[] offset : List.of(
                new int[]{1, 0, 0},
                new int[]{-1, 0, 0},
                new int[]{0, 1, 0},
                new int[]{0, -1, 0},
                new int[]{0, 0, 1},
                new int[]{0, 0, -1}
        )) {
            Material material = block.getRelative(offset[0], offset[1], offset[2]).getType();
            if (!material.isAir()) {
                counts.merge(material, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 4)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    private void setSelection(Player player, Block block, boolean first) {
        SelectionState state = selections.computeIfAbsent(player.getUniqueId(), ignored -> new SelectionState(block.getWorld().getName(), null, null));
        SelectionState updated = first
                ? new SelectionState(block.getWorld().getName(), vector(block), state.second())
                : new SelectionState(block.getWorld().getName(), state.first(), vector(block));
        selections.put(player.getUniqueId(), updated);
        action(player, first ? "builder.selection.pos1-action" : "builder.selection.pos2-action", Map.of());
    }

    private CuboidSelection requireSelection(Player player) {
        SelectionState state = selections.get(player.getUniqueId());
        if (state == null || state.selection().isEmpty()) {
            context.message(player, "builder.selection.required", Map.of());
            return null;
        }
        return state.selection().get();
    }

    private BuilderProfile profile(UUID playerId) {
        return profiles.computeIfAbsent(playerId, this::loadProfile);
    }

    private BuilderProfile loadProfile(UUID playerId) {
        YamlConfiguration yaml = store.load();
        BuilderProfile profile = new BuilderProfile(yaml.getBoolean("players." + playerId + ".build-mode", false));
        for (Toggle toggle : Toggle.values()) {
            profile.set(toggle, yaml.getBoolean("players." + playerId + ".toggles." + toggle.key(), true));
        }
        return profile;
    }

    private void saveProfile(UUID playerId) {
        if (!store.load().getBoolean("persist", true)) {
            return;
        }
        BuilderProfile profile = profiles.get(playerId);
        if (profile == null) {
            return;
        }
        YamlConfiguration yaml = store.load();
        yaml.set("players." + playerId + ".build-mode", profile.buildMode());
        for (Toggle toggle : Toggle.values()) {
            yaml.set("players." + playerId + ".toggles." + toggle.key(), profile.enabled(toggle));
        }
        store.save(yaml);
    }

    private void saveAll() {
        for (UUID playerId : profiles.keySet()) {
            saveProfile(playerId);
        }
    }

    private boolean requiresBuildMode(World world) {
        YamlConfiguration yaml = store.load();
        return yaml.getBoolean("require-build-mode.enabled", false)
                && yaml.getStringList("require-build-mode.worlds").stream().anyMatch(world.getName()::equalsIgnoreCase);
    }

    private long blockLimit(Player player) {
        long best = store.load().getLong("default-limit", 2500L);
        for (var permission : player.getEffectivePermissions()) {
            String node = permission.getPermission().toLowerCase(Locale.ROOT);
            if (node.startsWith("hydroxide.builder.limit.")) {
                best = Math.max(best, parseLong(node.substring("hydroxide.builder.limit.".length()), best));
            }
        }
        return best;
    }

    private BrushLimits brushLimits(Player player) {
        YamlConfiguration yaml = store.load();
        return new BrushLimits(yaml.getInt("brush.max-radius", 6), (int) Math.min(Integer.MAX_VALUE, blockLimit(player)));
    }

    private UndoHistory history(UUID playerId) {
        return histories.computeIfAbsent(playerId, ignored -> new UndoHistory(
                store.load().getInt("undo.max-snapshots", 10),
                Duration.ofMinutes(store.load().getLong("undo.expire-minutes", 30))
        ));
    }

    private void setBrush(Player player, BrushBinding binding) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            context.message(player, "builder.brush.hold-required", Map.of());
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (binding == null) {
            meta.getPersistentDataContainer().remove(key("brush"));
        } else {
            meta.getPersistentDataContainer().set(key("brush"), PersistentDataType.STRING, binding.serialize());
        }
        item.setItemMeta(meta);
    }

    private Optional<BrushBinding> brushBinding(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String raw = item.getItemMeta().getPersistentDataContainer().get(key("brush"), PersistentDataType.STRING);
        return BrushBinding.deserialize(raw);
    }

    private boolean isWand(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key("wand"), PersistentDataType.BOOLEAN);
    }

    private NamespacedKey key(String value) {
        return new NamespacedKey(context.plugin(), "builder_" + value);
    }

    private Player requirePlayer(CommandSender sender) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.message(sender, "validation.player-only", Map.of("usage", ""));
        }
        return player;
    }

    private BlockVector3i vector(Block block) {
        return new BlockVector3i(block.getX(), block.getY(), block.getZ());
    }

    private void iterate(CuboidSelection selection, CoordinateConsumer consumer) {
        BlockVector3i min = selection.min();
        BlockVector3i max = selection.max();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    consumer.accept(x, y, z);
                }
            }
        }
    }

    private boolean isWall(CuboidSelection selection, int x, int z) {
        BlockVector3i min = selection.min();
        BlockVector3i max = selection.max();
        return x == min.x() || x == max.x() || z == min.z() || z == max.z();
    }

    private boolean isShell(CuboidSelection selection, int x, int y, int z) {
        BlockVector3i min = selection.min();
        BlockVector3i max = selection.max();
        return isWall(selection, x, z) || y == min.y() || y == max.y();
    }

    private String state(boolean enabled) {
        return enabled ? "on" : "off";
    }

    private void action(Player player, String key, Map<String, ?> placeholders) {
        player.sendActionBar(context.messages().component(key, placeholders));
    }

    private boolean shouldCancelForToggle(Player player, Toggle toggle) {
        BuilderProfile profile = profile(player.getUniqueId());
        if (!profile.buildMode() || profile.enabled(toggle)) {
            return false;
        }
        action(player, "builder.toggle.disabled-action", Map.of("toggle", toggle.display()));
        return true;
    }

    private Toggle entityToggle(Entity entity) {
        if (entity instanceof ArmorStand) {
            return Toggle.ARMOR_STAND;
        }
        if (entity instanceof ItemFrame) {
            return Toggle.ITEM_FRAME;
        }
        return Toggle.ENTITY_EDIT;
    }

    private Player playerFrom(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private long parseLong(String input, long fallback) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void seedDefaults() {
        YamlConfiguration yaml = store.load();
        if (yaml.contains("default-limit")) {
            return;
        }
        yaml.set("persist", true);
        yaml.set("default-limit", 2500);
        yaml.set("batch-size", 250);
        yaml.set("allow-hydroxide-protection-bypass", true);
        yaml.set("require-build-mode.enabled", false);
        yaml.set("require-build-mode.worlds", List.of("admin", "build"));
        yaml.set("brush.max-radius", 6);
        yaml.set("brush.cooldown-ms", 500);
        yaml.set("undo.max-snapshots", 10);
        yaml.set("undo.expire-minutes", 30);
        store.save(yaml);
    }

    private interface CoordinateConsumer {
        void accept(int x, int y, int z);
    }

    private interface MaterialProvider {
        Material material(World world, int x, int y, int z);
    }

    private enum Toggle {
        PLACE("place", "Place"),
        BREAK("break", "Break"),
        LIQUID("liquid", "Liquid"),
        FIRE("fire", "Fire"),
        ITEM_FRAME("item-frame", "Item frame"),
        ARMOR_STAND("armor-stand", "Armor stand"),
        ENTITY_EDIT("entity-edit", "Entity edit");

        private final String key;
        private final String display;

        Toggle(String key, String display) {
            this.key = key;
            this.display = display;
        }

        String key() {
            return key;
        }

        String display() {
            return display;
        }

        static Optional<Toggle> fromKey(String key) {
            for (Toggle toggle : values()) {
                if (toggle.key.equalsIgnoreCase(key)) {
                    return Optional.of(toggle);
                }
            }
            return Optional.empty();
        }

        static List<String> keys() {
            List<String> keys = new ArrayList<>();
            for (Toggle toggle : values()) {
                keys.add(toggle.key);
            }
            return keys;
        }
    }

    private static final class BuilderProfile {
        private boolean buildMode;
        private final Map<Toggle, Boolean> toggles = new HashMap<>();

        private BuilderProfile(boolean buildMode) {
            this.buildMode = buildMode;
            for (Toggle toggle : Toggle.values()) {
                toggles.put(toggle, true);
            }
        }

        boolean buildMode() {
            return buildMode;
        }

        void buildMode(boolean buildMode) {
            this.buildMode = buildMode;
        }

        boolean enabled(Toggle toggle) {
            return toggles.getOrDefault(toggle, true);
        }

        void set(Toggle toggle, boolean enabled) {
            toggles.put(toggle, enabled);
        }
    }

    private record SelectionState(String worldName, BlockVector3i first, BlockVector3i second) {
        Optional<CuboidSelection> selection() {
            if (first == null || second == null) {
                return Optional.empty();
            }
            return Optional.of(new CuboidSelection(worldName, first, second));
        }

        SelectionState expand(int amount) {
            if (first == null || second == null) {
                return this;
            }
            BlockVector3i min = new CuboidSelection(worldName, first, second).min();
            BlockVector3i max = new CuboidSelection(worldName, first, second).max();
            return new SelectionState(worldName,
                    new BlockVector3i(min.x() - amount, min.y() - amount, min.z() - amount),
                    new BlockVector3i(max.x() + amount, max.y() + amount, max.z() + amount));
        }
    }

    private record Clipboard(List<ClipboardBlock> blocks) {
    }

    private record ClipboardBlock(int x, int y, int z, Material material) {
    }

    private enum BrushType {
        SPHERE,
        CYLINDER,
        REPLACE,
        SMOOTH
    }

    private record BrushBinding(BrushType type, Material from, Material to, int radius, int height) {
        static Optional<BrushBinding> parse(String[] args) {
            try {
                return switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "sphere" -> args.length >= 3
                            ? BuilderMaterialResolver.resolveBlock(args[1]).map(material -> new BrushBinding(BrushType.SPHERE, null, material, parse(args[2], 3), 1))
                            : Optional.empty();
                    case "cylinder" -> args.length >= 4
                            ? BuilderMaterialResolver.resolveBlock(args[1]).map(material -> new BrushBinding(BrushType.CYLINDER, null, material, parse(args[2], 3), parse(args[3], 1)))
                            : Optional.empty();
                    case "replace" -> parseReplace(args);
                    case "smooth" -> args.length >= 2
                            ? Optional.of(new BrushBinding(BrushType.SMOOTH, null, Material.AIR, parse(args[1], 3), 1))
                            : Optional.empty();
                    default -> Optional.empty();
                };
            } catch (RuntimeException exception) {
                return Optional.empty();
            }
        }

        private static Optional<BrushBinding> parseReplace(String[] args) {
            if (args.length < 4) {
                return Optional.empty();
            }
            Optional<Material> from = BuilderMaterialResolver.resolveBlock(args[1]);
            Optional<Material> to = BuilderMaterialResolver.resolveBlock(args[2]);
            if (from.isEmpty() || to.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new BrushBinding(BrushType.REPLACE, from.get(), to.get(), parse(args[3], 3), 1));
        }

        BrushBinding withRadius(int radius) {
            return new BrushBinding(type, from, to, radius, height);
        }

        String serialize() {
            return type.name() + "|" + (from == null ? "" : from.name()) + "|" + to.name() + "|" + radius + "|" + height;
        }

        static Optional<BrushBinding> deserialize(String raw) {
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 5) {
                return Optional.empty();
            }
            try {
                Material from = parts[1].isBlank() ? null : Material.valueOf(parts[1]);
                Material to = Material.valueOf(parts[2]);
                return Optional.of(new BrushBinding(BrushType.valueOf(parts[0]), from, to, parse(parts[3], 3), parse(parts[4], 1)));
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        private static int parse(String input, int fallback) {
            try {
                return Math.max(1, Integer.parseInt(input));
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }
    }
}
