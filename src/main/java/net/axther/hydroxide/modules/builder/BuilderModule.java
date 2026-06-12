package net.axther.hydroxide.modules.builder;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
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

public final class BuilderModule implements HydroModule, Listener, CommandExecutor, TabCompleter, BuilderService {

    private static final List<String> COMMANDS = List.of(
            "build", "buildstatus", "breaktoggle", "placetoggle", "pickblock",
            "wand", "pos1", "pos2", "sel", "setblockarea", "replacearea", "walls", "hollow",
            "copyarea", "pastearea", "undo", "redo", "brush", "fillnear", "drainnear", "fixlight"
    );
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
        for (String command : COMMANDS) {
            context.commands().register(command, this);
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        saveAll();
        context.services().clearBuilderService(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "build" -> build(sender, label, args);
            case "buildstatus" -> buildStatus(sender, args);
            case "breaktoggle" -> toggleOwn(sender, Toggle.BREAK);
            case "placetoggle" -> toggleOwn(sender, Toggle.PLACE);
            case "pickblock" -> pickBlock(sender);
            case "wand" -> wand(sender);
            case "pos1" -> setPosition(sender, true);
            case "pos2" -> setPosition(sender, false);
            case "sel" -> selectionCommand(sender, label, args);
            case "setblockarea" -> setArea(sender, label, args);
            case "replacearea" -> replaceArea(sender, label, args);
            case "walls" -> walls(sender, label, args);
            case "hollow" -> hollow(sender, label, args);
            case "copyarea" -> copyArea(sender);
            case "pastearea" -> pasteArea(sender);
            case "undo" -> history(sender, args, true);
            case "redo" -> history(sender, args, false);
            case "brush" -> brush(sender, label, args);
            case "fillnear" -> fillNear(sender, label, args);
            case "drainnear" -> drainNear(sender, args);
            case "fixlight" -> fixLight(sender, args);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
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
            context.send(event.getPlayer(), "<red>Build mode is required in this world.");
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
            context.send(event.getPlayer(), "<red>Build mode is required in this world.");
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
                context.send(sender, "<red>You cannot change build mode for others.");
                return true;
            }
            target = CommandUtils.onlinePlayer(args[0]).orElse(null);
            mode = args.length > 1 ? args[1] : "toggle";
        }
        if (target == null) {
            context.send(sender, "<red>Usage: /" + label + " [player] [on|off|toggle]");
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
        action(target, enabled ? "<green>Build mode enabled." : "<red>Build mode disabled.");
        context.send(sender, "<green>Build mode for <white>" + target.getName() + " <green>is <white>" + (enabled ? "on" : "off") + "<green>.");
        return true;
    }

    private boolean buildStatus(CommandSender sender, String[] args) {
        Player target = args.length == 0 ? requirePlayer(sender) : CommandUtils.onlinePlayer(args[0]).orElse(null);
        if (target == null) {
            context.send(sender, "<red>That player is not online.");
            return true;
        }
        BuilderProfile profile = profile(target.getUniqueId());
        String toggles = Toggle.keys().stream()
                .map(key -> key + "=" + profile.enabled(Toggle.fromKey(key).orElseThrow()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        context.send(sender, "<#44CCFF>Build status for <white>" + target.getName()
                + "<gray>: mode=<white>" + profile.buildMode()
                + "<gray>, toggles=<white>" + toggles);
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
        action(player, "<#44CCFF>" + toggle.display() + " toggle: <white>" + (enabled ? "on" : "off"));
        return true;
    }

    private boolean toggleSetting(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        Toggle toggle = Toggle.fromKey(args[1]).orElse(null);
        if (toggle == null) {
            context.send(player, "<red>Usage: /" + label + " toggle <" + String.join("|", Toggle.keys()) + "> [on|off]");
            return true;
        }
        BuilderProfile profile = profile(player.getUniqueId());
        boolean enabled = args.length >= 3 ? args[2].equalsIgnoreCase("on") : !profile.enabled(toggle);
        profile.set(toggle, enabled);
        saveProfile(player.getUniqueId());
        action(player, "<#44CCFF>" + toggle.display() + " toggle: <white>" + (enabled ? "on" : "off"));
        return true;
    }

    private boolean pickBlock(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.pickblock")) {
            return true;
        }
        Block block = player.getTargetBlockExact(100);
        if (block == null) {
            context.send(player, "<red>Look at a block first.");
            return true;
        }
        player.getInventory().addItem(new ItemStack(block.getType()));
        context.send(player, "<green>Picked <white>" + block.getType().key().asString() + "<green>.");
        return true;
    }

    private boolean wand(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.wand")) {
            return true;
        }
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.displayName(context.text().format("<#44CCFF>Hydroxide Builder Wand"));
        meta.getPersistentDataContainer().set(key("wand"), PersistentDataType.BOOLEAN, true);
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);
        context.send(player, "<green>Builder wand added.");
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
            context.send(player, "<red>Usage: /" + label + " clear|expand|contract|info");
            return true;
        }
        SelectionState state = selections.get(player.getUniqueId());
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "clear" -> {
                selections.remove(player.getUniqueId());
                context.send(player, "<green>Selection cleared.");
            }
            case "info" -> context.send(player, state == null || state.selection().isEmpty()
                    ? "<red>No complete selection."
                    : "<green>Selection volume: <white>" + state.selection().get().volume());
            case "expand", "contract" -> {
                if (state == null || state.selection().isEmpty()) {
                    context.send(player, "<red>No complete selection.");
                    return true;
                }
                int amount = args[0].equalsIgnoreCase("expand") ? 1 : -1;
                selections.put(player.getUniqueId(), state.expand(amount));
                context.send(player, "<green>Selection updated.");
            }
            default -> context.send(player, "<red>Usage: /" + label + " clear|expand|contract|info");
        }
        return true;
    }

    private boolean setArea(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 1) {
            context.send(sender, "<red>Usage: /" + label + " <block>");
            return true;
        }
        Material material = BuilderMaterialResolver.resolveBlock(args[0]).orElse(null);
        if (material == null) {
            context.send(player, "<red>Unknown block.");
            return true;
        }
        return selectionPlan(player, selection -> plan(selection, (world, x, y, z) -> material, false));
    }

    private boolean replaceArea(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " <from> <to>");
            return true;
        }
        Material from = BuilderMaterialResolver.resolveBlock(args[0]).orElse(null);
        Material to = BuilderMaterialResolver.resolveBlock(args[1]).orElse(null);
        if (from == null || to == null) {
            context.send(player, "<red>Unknown block.");
            return true;
        }
        return selectionPlan(player, selection -> plan(selection, (world, x, y, z) -> world.getBlockAt(x, y, z).getType() == from ? to : null, false));
    }

    private boolean walls(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 1) {
            context.send(sender, "<red>Usage: /" + label + " <block>");
            return true;
        }
        Material material = BuilderMaterialResolver.resolveBlock(args[0]).orElse(null);
        if (material == null) {
            context.send(player, "<red>Unknown block.");
            return true;
        }
        return selectionPlan(player, selection -> plan(selection, (world, x, y, z) -> isWall(selection, x, z) ? material : null, false));
    }

    private boolean hollow(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 1) {
            context.send(sender, "<red>Usage: /" + label + " <block>");
            return true;
        }
        Material material = BuilderMaterialResolver.resolveBlock(args[0]).orElse(null);
        if (material == null) {
            context.send(player, "<red>Unknown block.");
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
            context.send(player, "<red>Selection world is not loaded.");
            return true;
        }
        BlockVector3i min = selection.min();
        List<ClipboardBlock> blocks = new ArrayList<>();
        iterate(selection, (x, y, z) -> blocks.add(new ClipboardBlock(x - min.x(), y - min.y(), z - min.z(), world.getBlockAt(x, y, z).getType())));
        clipboards.put(player.getUniqueId(), new Clipboard(blocks));
        context.send(player, "<green>Copied <white>" + blocks.size() + " <green>blocks.");
        return true;
    }

    private boolean pasteArea(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.builder.paste")) {
            return true;
        }
        Clipboard clipboard = clipboards.get(player.getUniqueId());
        if (clipboard == null) {
            context.send(player, "<red>Your clipboard is empty.");
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
                context.send(player, undo ? "<red>Nothing to undo." : "<red>Nothing to redo.");
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
            context.send(player, "<red>Usage: /" + label + " sphere|cylinder|replace|smooth|none ...");
            return true;
        }
        if (args[0].equalsIgnoreCase("none")) {
            setBrush(player, null);
            context.send(player, "<green>Brush cleared.");
            return true;
        }
        BrushBinding binding = BrushBinding.parse(args).orElse(null);
        if (binding == null) {
            context.send(player, "<red>Invalid brush arguments.");
            return true;
        }
        BrushLimits limits = brushLimits(player);
        if (binding.radius() > limits.maxRadius()) {
            binding = binding.withRadius(limits.maxRadius());
        }
        setBrush(player, binding);
        context.send(player, "<green>Brush bound to held item.");
        return true;
    }

    private boolean fillNear(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " <block> <radius>");
            return true;
        }
        Material material = BuilderMaterialResolver.resolveBlock(args[0]).orElse(null);
        int radius = brushLimits(player).clampRadius(parseInt(args[1], 3));
        if (material == null) {
            context.send(player, "<red>Unknown block.");
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
        context.send(player, "<green>Queued nearby light/chunk refresh hint for <white>" + chunks + "x" + chunks + " <green>chunks.");
        return true;
    }

    private boolean selectionPlan(Player player, java.util.function.Function<CuboidSelection, BlockEditPlan> planner) {
        CuboidSelection selection = requireSelection(player);
        if (selection == null || !context.requirePermission(player, "hydroxide.builder.edit")) {
            return true;
        }
        if (!selection.withinLimit(blockLimit(player))) {
            context.send(player, "<red>Selection is too large. Limit: <white>" + blockLimit(player));
            return true;
        }
        World world = Bukkit.getWorld(selection.worldName());
        if (world == null) {
            context.send(player, "<red>Selection world is not loaded.");
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
            context.send(player, "<gray>No blocks changed.");
            return;
        }
        if (plan.changes().size() > blockLimit(player)) {
            context.send(player, "<red>Edit exceeds your block limit of <white>" + blockLimit(player));
            return;
        }
        if (activeEdits.contains(player.getUniqueId())) {
            context.send(player, "<red>You already have an edit running.");
            return;
        }
        activeEdits.add(player.getUniqueId());
        List<List<BlockChange>> batches = plan.batches(store.load().getInt("batch-size", 250));
        runBatch(player, world, batches, 0, () -> {
            activeEdits.remove(player.getUniqueId());
            if (recordUndo) {
                history(player.getUniqueId()).record(plan, Instant.now());
            }
            action(player, "<green>Edit complete: <white>" + plan.changes().size() + " <green>blocks.");
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
        action(player, "<#44CCFF>Edit progress <white>" + (index + 1) + "/" + batches.size());
        BukkitTask ignored = Bukkit.getScheduler().runTaskLater(context.plugin(), () -> runBatch(player, world, batches, index + 1, done), 1L);
    }

    private void applyBrush(Player player, Block target, BrushBinding binding) {
        long now = System.currentTimeMillis();
        long readyAt = brushCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            action(player, "<red>Brush is cooling down.");
            return;
        }
        brushCooldowns.put(player.getUniqueId(), now + store.load().getLong("brush.cooldown-ms", 500L));
        BrushLimits limits = brushLimits(player);
        if (!limits.withinBlockLimit((int) Math.min(Integer.MAX_VALUE, Math.round((4.0D / 3.0D) * Math.PI * Math.pow(binding.radius(), 3))))) {
            context.send(player, "<red>Brush exceeds your block limit.");
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
        action(player, first ? "<green>Position 1 set." : "<green>Position 2 set.");
    }

    private CuboidSelection requireSelection(Player player) {
        SelectionState state = selections.get(player.getUniqueId());
        if (state == null || state.selection().isEmpty()) {
            context.send(player, "<red>Select both positions first.");
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
            context.send(player, "<red>Hold an item first.");
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
            context.send(sender, "<red>Only players can use builder tools.");
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

    private void action(Player player, String message) {
        player.sendActionBar(context.text().format(message));
    }

    private boolean shouldCancelForToggle(Player player, Toggle toggle) {
        BuilderProfile profile = profile(player.getUniqueId());
        if (!profile.buildMode() || profile.enabled(toggle)) {
            return false;
        }
        action(player, "<red>" + toggle.display() + " toggle is off.");
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
