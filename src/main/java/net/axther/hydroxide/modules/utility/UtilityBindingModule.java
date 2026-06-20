package net.axther.hydroxide.modules.utility;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.kyori.adventure.text.Component;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class UtilityBindingModule implements HydroModule, Listener {

    private static final List<String> BOOK_ACTIONS = List.of("addpage", "add", "author", "page", "title", "unlock");
    private static final List<String> ATTACH_CLICKS = List.of("left", "right");
    private static final List<String> ATTACH_EXECUTORS = List.of("console", "player");
    private static final List<String> ATTACH_USES = List.of("1", "3", "10", "infinite");
    private static final List<String> ENCHANT_ACTIONS = List.of("add", "remove", "clear");
    private static final List<String> ITEM_FRAME_ACTIONS = List.of("invisible", "fixed", "invulnerable", "all");
    private static final List<String> UNBREAKABLE_STATES = List.of(
            "true", "false", "toggle", "on", "off", "enabled", "disabled"
    );
    private static final List<String> FIREWORK_ACTIONS = List.of("power", "clear", "fire");
    private static final List<String> HIDE_FLAG_SUGGESTIONS = hideFlagSuggestions();
    private static final List<String> COMMON_ENCHANTMENTS = List.of(
            "aqua_affinity", "bane_of_arthropods", "blast_protection", "breach", "channeling", "density",
            "depth_strider", "efficiency", "feather_falling", "fire_aspect", "fire_protection", "flame",
            "fortune", "frost_walker", "impaling", "infinity", "knockback", "looting", "loyalty",
            "luck_of_the_sea", "lure", "mending", "multishot", "piercing", "power", "projectile_protection",
            "protection", "punch", "quick_charge", "respiration", "riptide", "sharpness", "silk_touch",
            "smite", "soul_speed", "sweeping_edge", "swift_sneak", "thorns", "unbreaking", "wind_burst"
    );
    private static final List<String> DYE_SUGGESTIONS = dyeSuggestions();
    private static final List<String> POWERTOOL_ACTIONS = List.of("clear", "remove", "none", "off");

    private HydroxideContext context;
    private final Map<UUID, List<String>> signClipboards = new HashMap<>();
    private final Map<UUID, ItemStack> itemClipboards = new HashMap<>();
    private final Set<UUID> powerToolsDisabled = ConcurrentHashMap.newKeySet();

    @Override
    public String id() {
        return "utility-bindings";
    }

    @Override
    public String displayName() {
        return "Sign, Book, Item Utilities";
    }

    @Override
    public String description() {
        return "Sign editing/copying, item editing/copying, books, and PDC command-bound items.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("signedit", command("signedit", "hydroxide.item.sign", "/{label} <1-4> <text>",
                ctx -> signEdit(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), null));
        context.commands().register("signcopy", command("signcopy", "hydroxide.item.sign", "/{label}",
                ctx -> signCopy(ctx.sender()), null));
        context.commands().register("signpaste", command("signpaste", "hydroxide.item.sign", "/{label}",
                ctx -> signPaste(ctx.sender()), null));
        context.commands().register("signglow", command("signglow", "hydroxide.item.sign", "/{label} [color]",
                ctx -> signGlow(ctx.sender(), ctx.arguments().toArray(String[]::new)), this::signGlowCompletions));
        context.commands().register("bookedit", command("bookedit", "hydroxide.item.edit", "/{label} title|author|page|addpage|unlock ...",
                ctx -> bookEdit(ctx.sender(), ctx.label(), ctx.arguments()), this::bookCompletions));
        context.commands().register("itemname", command("itemname", "hydroxide.item.edit", "/{label} <name>",
                ctx -> itemName(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), null));
        context.commands().register("itemlore", command("itemlore", "hydroxide.item.edit", "/{label} add|set|remove ...",
                ctx -> itemLore(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), null));
        context.commands().register("itemflag", command("itemflag", "hydroxide.item.edit", "/{label} <flag>",
                ctx -> itemFlag(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::itemFlagCompletions));
        context.commands().register("hideflags", command("hideflags", "hydroxide.item.edit", "/{label} <flag|all|clear> [flag...]",
                ctx -> hideFlags(ctx.sender(), ctx.label(), ctx.arguments()), this::hideFlagsCompletions));
        context.commands().register("itemenchant", command("itemenchant", "hydroxide.item.edit", "/{label} [add|remove|clear] <enchantment> [level]",
                ctx -> itemEnchant(ctx.sender(), ctx.label(), ctx.arguments()), this::itemEnchantCompletions));
        context.commands().register("itemrepair", command("itemrepair", "hydroxide.item.edit", "/{label} [hand|all]",
                ctx -> itemRepair(ctx.sender(), ctx.label(), ctx.arguments()), this::itemRepairCompletions));
        context.commands().register("anvilrepaircost", command("anvilrepaircost", "hydroxide.item.edit", "/{label} <amount>",
                ctx -> anvilRepairCost(ctx.sender(), ctx.label(), ctx.arguments()), null));
        context.commands().register("unbreakable", command("unbreakable", "hydroxide.item.edit", "/{label} [player] [true|false|toggle]", false,
                ctx -> unbreakable(ctx.sender(), ctx.label(), ctx.arguments()), this::unbreakableCompletions));
        context.commands().register("more", command("more", "hydroxide.item.edit", "/{label} [amount]",
                ctx -> itemMore(ctx.sender(), ctx.label(), ctx.arguments()), null));
        context.commands().register("firework", command("firework", "hydroxide.item.firework", "/{label} <power [amount]|clear|fire [amount]>",
                ctx -> firework(ctx.sender(), ctx.label(), ctx.arguments()), this::fireworkCompletions));
        context.commands().register("dye", command("dye", "hydroxide.item.dye", "/{label} <red,green,blue|hex|color|random|clear>",
                ctx -> itemDye(ctx.sender(), ctx.label(), ctx.arguments()), this::dyeCompletions));
        context.commands().register("itemframe", command("itemframe", "hydroxide.item.frame", "/{label} <invisible|fixed|invulnerable|all>",
                ctx -> itemFrame(ctx.sender(), ctx.label(), ctx.arguments()), this::itemFrameCompletions));
        context.commands().register("iteminfo", command("iteminfo", "hydroxide.item.info", "/{label}",
                ctx -> itemInfo(ctx.sender()), null));
        context.commands().register("blockinfo", command("blockinfo", "hydroxide.admin.blockinfo", "/{label}",
                ctx -> blockInfo(ctx.sender()), null));
        context.commands().register("entityinfo", command("entityinfo", "hydroxide.admin.entityinfo", "/{label}",
                ctx -> entityInfo(ctx.sender()), null));
        context.commands().register("recipe", command("recipe", "hydroxide.item.info", "/{label} [hand|material]",
                ctx -> recipe(ctx.sender(), ctx.label(), ctx.arguments()), this::recipeCompletions));
        context.commands().register("itemcopy", command("itemcopy", "hydroxide.item.edit", "/{label}",
                ctx -> itemCopy(ctx.sender()), null));
        context.commands().register("itempaste", command("itempaste", "hydroxide.item.edit", "/{label}",
                ctx -> itemPaste(ctx.sender()), null));
        context.commands().register("attachcommand", command("attachcommand", "hydroxide.item.attach", "/{label} <left|right> <player|console> <uses|infinite> <command...>",
                ctx -> attachCommand(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::attachCompletions));
        context.commands().register("powertool", command("powertool", "hydroxide.item.attach", "/{label} <command...|clear>",
                ctx -> powerTool(ctx.sender(), ctx.label(), ctx.arguments()), this::powerToolCompletions));
        context.commands().register("powertoollist", command("powertoollist", "hydroxide.item.attach", "/{label}",
                ctx -> powerToolList(ctx.sender()), null));
        context.commands().register("powertooltoggle", command("powertooltoggle", "hydroxide.item.attach", "/{label} [on|off|toggle]",
                ctx -> powerToolToggle(ctx.sender(), ctx.label(), ctx.arguments()), this::powerToolToggleCompletions));
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    private CommandService command(String name, String permission, String usage, HydroCommand.HydroCommandExecutor executor,
                                   HydroCommand.HydroTabCompleter completer) {
        return command(name, permission, usage, true, executor, completer);
    }

    private CommandService command(String name, String permission, String usage, boolean playerOnly,
                                   HydroCommand.HydroCommandExecutor executor,
                                   HydroCommand.HydroTabCompleter completer) {
        HydroCommand.Builder builder = HydroCommand.builder(name)
                .permission(permission)
                .playerOnly(playerOnly)
                .usage(usage)
                .executor(executor);
        if (completer != null) {
            builder.completer(completer);
        }
        return new CommandService(builder.build(), context.messages());
    }

    private List<String> signGlowCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1
                ? CommandUtils.matching(ctx.argument(0), java.util.Arrays.stream(DyeColor.values()).map(Enum::name).map(String::toLowerCase).toList())
                : List.of();
    }

    private List<String> bookCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), BOOK_ACTIONS) : List.of();
    }

    private List<String> itemFlagCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1
                ? CommandUtils.matching(ctx.argument(0), java.util.Arrays.stream(ItemFlag.values()).map(Enum::name).map(String::toLowerCase).toList())
                : List.of();
    }

    private List<String> hideFlagsCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() > 1 && List.of("all", "clear", "reset").contains(ctx.argument(0).toLowerCase(Locale.ROOT))) {
            return List.of();
        }
        return CommandUtils.matching(ctx.argument(ctx.arguments().size() - 1), HIDE_FLAG_SUGGESTIONS);
    }

    private List<String> itemEnchantCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> options = new ArrayList<>(ENCHANT_ACTIONS);
            options.addAll(COMMON_ENCHANTMENTS);
            return CommandUtils.matching(ctx.argument(0), options);
        }
        if (ctx.arguments().size() == 2 && List.of("add", "remove").contains(ctx.argument(0).toLowerCase(Locale.ROOT))) {
            return CommandUtils.matching(ctx.argument(1), COMMON_ENCHANTMENTS);
        }
        if ((ctx.arguments().size() == 2 && !ENCHANT_ACTIONS.contains(ctx.argument(0).toLowerCase(Locale.ROOT)))
                || (ctx.arguments().size() == 3 && ctx.argument(0).equalsIgnoreCase("add"))) {
            return CommandUtils.matching(ctx.argument(ctx.arguments().size() - 1), List.of("1", "2", "3", "4", "5"));
        }
        return List.of();
    }

    private List<String> itemRepairCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), List.of("all", "hand")) : List.of();
    }

    private List<String> unbreakableCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            List<String> values = new ArrayList<>(UNBREAKABLE_STATES);
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            return CompletionUtils.matching(ctx.argument(1), UNBREAKABLE_STATES);
        }
        return List.of();
    }

    private List<String> dyeCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), DYE_SUGGESTIONS) : List.of();
    }

    private List<String> fireworkCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), FIREWORK_ACTIONS);
        }
        if (ctx.arguments().size() == 2) {
            return switch (ctx.argument(0).toLowerCase(Locale.ROOT)) {
                case "power" -> CommandUtils.matching(ctx.argument(1), List.of("0", "1", "2", "3"));
                case "fire", "launch" -> CommandUtils.matching(ctx.argument(1), List.of("1", "2", "4", "8", "16"));
                default -> List.of();
            };
        }
        return List.of();
    }

    private List<String> itemFrameCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), ITEM_FRAME_ACTIONS) : List.of();
    }

    private List<String> recipeCompletions(CommandContext ctx) {
        if (ctx.arguments().size() != 1) {
            return List.of();
        }
        List<String> options = new ArrayList<>(List.of("hand", "held"));
        options.addAll(Arrays.stream(Material.values())
                .filter(Material::isItem)
                .map(material -> material.name().toLowerCase(Locale.ROOT))
                .toList());
        return CommandUtils.matching(ctx.argument(0), options);
    }

    private List<String> attachCompletions(CommandContext ctx) {
        return switch (ctx.arguments().size()) {
            case 1 -> CommandUtils.matching(ctx.argument(0), ATTACH_CLICKS);
            case 2 -> CommandUtils.matching(ctx.argument(1), ATTACH_EXECUTORS);
            case 3 -> CommandUtils.matching(ctx.argument(2), ATTACH_USES);
            default -> List.of();
        };
    }

    private List<String> powerToolCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), POWERTOOL_ACTIONS) : List.of();
    }

    private List<String> powerToolToggleCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), List.of("on", "off", "toggle")) : List.of();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        Optional<AttachedCommand> command = attached(item);
        if (command.isEmpty()) {
            return;
        }
        AttachedCommand.Click click = action.name().startsWith("LEFT") ? AttachedCommand.Click.LEFT : AttachedCommand.Click.RIGHT;
        if (command.get().click() != click) {
            return;
        }
        if (powerToolsDisabled.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        execute(event.getPlayer(), item, command.get());
    }

    private boolean signEdit(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        if (player == null || !context.requirePermission(sender, "hydroxide.item.sign")) {
            return true;
        }
        Sign sign = sign(player);
        if (sign == null) {
            context.message(player, "utility.sign.target-required", Map.of());
            return true;
        }
        if (args.length < 2) {
            context.message(player, "utility.sign.edit-usage", Map.of("label", label));
            return true;
        }
        int line = Math.max(0, Math.min(3, parseInt(args[0], 1) - 1));
        sign.getSide(Side.FRONT).line(line, context.text().format(CommandUtils.joinArgs(args, 1)));
        sign.update(true);
        context.message(player, "utility.sign.updated", Map.of());
        return true;
    }

    private boolean signCopy(CommandSender sender) {
        Player player = player(sender);
        Sign sign = player == null ? null : sign(player);
        if (player == null || sign == null) {
            context.message(sender, "utility.sign.target-required", Map.of());
            return true;
        }
        signClipboards.put(player.getUniqueId(), sign.getSide(Side.FRONT).lines().stream().map(context.text()::plain).toList());
        context.message(player, "utility.sign.copied", Map.of());
        return true;
    }

    private boolean signPaste(CommandSender sender) {
        Player player = player(sender);
        Sign sign = player == null ? null : sign(player);
        if (player == null || sign == null) {
            context.message(sender, "utility.sign.target-required", Map.of());
            return true;
        }
        List<String> lines = signClipboards.get(player.getUniqueId());
        if (lines == null) {
            context.message(player, "utility.sign.clipboard-empty", Map.of());
            return true;
        }
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            sign.getSide(Side.FRONT).line(i, context.text().format(lines.get(i)));
        }
        sign.update(true);
        context.message(player, "utility.sign.pasted", Map.of());
        return true;
    }

    private boolean signGlow(CommandSender sender, String[] args) {
        Player player = player(sender);
        Sign sign = player == null ? null : sign(player);
        if (player == null || sign == null) {
            context.message(sender, "utility.sign.target-required", Map.of());
            return true;
        }
        SignSide side = sign.getSide(Side.FRONT);
        side.setGlowingText(true);
        if (args.length > 0) {
            try {
                side.setColor(DyeColor.valueOf(args[0].toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                context.message(player, "utility.sign.unknown-dye", Map.of("color", args[0]));
                return true;
            }
        }
        sign.update(true);
        context.message(player, "utility.sign.glow-updated", Map.of());
        return true;
    }

    private boolean bookEdit(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null || !(item.getItemMeta() instanceof BookMeta meta)) {
            context.message(sender, "utility.book.hold-required", Map.of());
            return true;
        }
        Optional<BookEditCommandParser.Request> parsed = BookEditCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(player, "utility.book.usage", Map.of("label", label));
            return true;
        }

        BookEditCommandParser.Request request = parsed.get();
        switch (request.action()) {
            case TITLE -> meta.title(context.text().format(argumentTail(args, request.valueStartIndex())));
            case AUTHOR -> meta.author(context.text().format(argumentTail(args, request.valueStartIndex())));
            case ADD_PAGE -> meta.addPages(context.text().format(argumentTail(args, request.valueStartIndex())));
            case PAGE -> {
                int page = request.page().orElseThrow();
                while (meta.getPageCount() < page) {
                    meta.addPages(Component.empty());
                }
                meta.page(page, context.text().format(argumentTail(args, request.valueStartIndex())));
            }
            case UNLOCK -> {
                ItemStack unlocked = item.withType(Material.WRITABLE_BOOK);
                unlocked.setItemMeta(meta);
                player.getInventory().setItemInMainHand(unlocked);
                context.message(player, "utility.book.unlocked", Map.of());
                return true;
            }
        }
        item.setItemMeta(meta);
        context.message(player, "utility.book.updated", Map.of());
        return true;
    }

    private boolean itemName(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null) {
            return true;
        }
        if (item == null) {
            context.message(player, "utility.item.hold-required", Map.of());
            return true;
        }
        if (args.length == 0) {
            context.message(player, "utility.item.name-usage", Map.of("label", label));
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        meta.displayName(context.text().format(CommandUtils.joinArgs(args, 0)));
        item.setItemMeta(meta);
        context.message(player, "utility.item.name-updated", Map.of());
        return true;
    }

    private boolean itemLore(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null) {
            return true;
        }
        if (item == null) {
            context.message(player, "utility.item.hold-required", Map.of());
            return true;
        }
        if (args.length < 2) {
            context.message(player, "utility.item.lore-usage", Map.of("label", label));
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new java.util.ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> lore.add(context.text().format(CommandUtils.joinArgs(args, 1)));
            case "set" -> {
                if (args.length < 3) {
                    context.message(player, "utility.item.lore-usage", Map.of("label", label));
                    return true;
                }
                int index = Math.max(0, parseInt(args[1], 1) - 1);
                while (lore.size() <= index) {
                    lore.add(Component.empty());
                }
                lore.set(index, context.text().format(CommandUtils.joinArgs(args, 2)));
            }
            case "remove" -> {
                int index = Math.max(0, parseInt(args[1], 1) - 1);
                if (index < lore.size()) {
                    lore.remove(index);
                }
            }
            default -> {
                context.message(player, "utility.item.lore-usage", Map.of("label", label));
                return true;
            }
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        context.message(player, "utility.item.lore-updated", Map.of());
        return true;
    }

    private boolean itemFlag(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null) {
            return true;
        }
        if (item == null) {
            context.message(player, "utility.item.hold-required", Map.of());
            return true;
        }
        if (args.length < 1) {
            context.message(player, "utility.item.flag-usage", Map.of("label", label));
            return true;
        }
        try {
            ItemFlag flag = ItemFlag.valueOf(args[0].toUpperCase(Locale.ROOT));
            ItemMeta meta = item.getItemMeta();
            if (meta.hasItemFlag(flag)) {
                meta.removeItemFlags(flag);
            } else {
                meta.addItemFlags(flag);
            }
            item.setItemMeta(meta);
            context.message(player, "utility.item.flag-updated", Map.of("flag", flag.name().toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            context.message(player, "utility.item.flag-unknown", Map.of("flag", args[0]));
        }
        return true;
    }

    private boolean hideFlags(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null) {
            return true;
        }
        if (item == null) {
            context.message(player, "utility.item.hold-required", Map.of());
            return true;
        }
        Optional<HideFlagsCommandParser.Request> request = HideFlagsCommandParser.parse(args);
        if (request.isEmpty()) {
            context.message(player, "utility.item.hideflags-usage", Map.of("label", label));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (request.get().action() == HideFlagsCommandParser.Action.CLEAR) {
            int count = meta.getItemFlags().size();
            meta.removeItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
            context.message(player, "utility.item.hideflags-cleared", Map.of("count", count));
            return true;
        }

        ItemFlag[] flags = request.get().flags().toArray(ItemFlag[]::new);
        meta.addItemFlags(flags);
        item.setItemMeta(meta);
        context.message(player, "utility.item.hideflags-updated", Map.of(
                "count", flags.length,
                "flags", Arrays.stream(flags).map(flag -> flag.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", "))
        ));
        return true;
    }

    private boolean itemEnchant(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null) {
            return true;
        }
        if (item == null) {
            context.message(player, "utility.item.hold-required", Map.of());
            return true;
        }
        Optional<ItemEnchantCommandParser.Request> parsed = ItemEnchantCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(player, "utility.item.enchant-usage", Map.of("label", label));
            return true;
        }

        ItemEnchantCommandParser.Request request = parsed.get();
        if (request.action() == ItemEnchantCommandParser.Action.CLEAR) {
            int removed = item.getEnchantments().size();
            item.getEnchantments().keySet().forEach(item::removeEnchantment);
            context.message(player, "utility.item.enchant-cleared", Map.of("count", removed));
            return true;
        }

        String enchantmentName = request.enchantment().orElseThrow();
        Enchantment enchantment = resolveEnchantment(enchantmentName);
        if (enchantment == null) {
            context.message(player, "utility.item.enchant-unknown", Map.of("enchantment", enchantmentName));
            return true;
        }
        if (request.action() == ItemEnchantCommandParser.Action.REMOVE) {
            item.removeEnchantment(enchantment);
            context.message(player, "utility.item.enchant-removed", Map.of("enchantment", enchantmentName));
            return true;
        }

        int level = request.level().orElse(1);
        item.addUnsafeEnchantment(enchantment, level);
        context.message(player, "utility.item.enchanted", Map.of("enchantment", enchantmentName, "level", level));
        return true;
    }

    private Enchantment resolveEnchantment(String input) {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft(input.toLowerCase(Locale.ROOT).replace("minecraft:", "")));
    }

    private boolean itemRepair(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        Optional<ItemRepairMode> mode = ItemRepairMode.from(args);
        if (mode.isEmpty()) {
            context.message(player, "utility.item.repair-usage", Map.of("label", label));
            return true;
        }

        if (mode.get() == ItemRepairMode.ALL) {
            int repaired = repairInventory(player);
            context.message(player, "utility.item.repaired-all", Map.of("count", repaired));
            return true;
        }

        ItemStack item = held(player);
        if (item == null) {
            context.message(player, "utility.item.hold-required", Map.of());
            return true;
        }
        repairItem(item);
        context.message(player, "utility.item.repaired", Map.of());
        return true;
    }

    private int repairInventory(Player player) {
        int repaired = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            repaired += repairItem(item) ? 1 : 0;
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            repaired += repairItem(item) ? 1 : 0;
        }
        repaired += repairItem(player.getInventory().getItemInOffHand()) ? 1 : 0;
        return repaired;
    }

    private boolean repairItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable && damageable.getDamage() > 0) {
            damageable.setDamage(0);
            item.setItemMeta(meta);
            return true;
        }
        return false;
    }

    private boolean itemMore(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null) {
            if (player != null) {
                context.message(player, "utility.item.hold-required", Map.of());
            }
            return true;
        }
        OptionalInt amount = ItemAmountParser.targetAmount(args, item.getMaxStackSize());
        if (amount.isEmpty()) {
            context.message(player, "utility.item.more-usage", Map.of("label", label));
            return true;
        }
        item.setAmount(amount.getAsInt());
        context.message(player, "utility.item.more-updated", Map.of("amount", amount.getAsInt()));
        return true;
    }

    private boolean firework(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }

        ItemStack item = held(player);
        if (item == null || item.getType() != Material.FIREWORK_ROCKET || !(item.getItemMeta() instanceof FireworkMeta meta)) {
            context.message(player, "utility.item.firework-hold-required", Map.of());
            return true;
        }

        Optional<FireworkCommandParser.Request> parsed = FireworkCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(player, "utility.item.firework-usage", Map.of("label", label));
            return true;
        }

        FireworkCommandParser.Request request = parsed.get();
        switch (request.action()) {
            case POWER -> {
                meta.setPower(request.amount());
                item.setItemMeta(meta);
                context.message(player, "utility.item.firework-power-updated", Map.of("amount", request.amount()));
            }
            case CLEAR -> {
                meta.clearEffects();
                meta.setPower(0);
                item.setItemMeta(meta);
                context.message(player, "utility.item.firework-cleared", Map.of());
            }
            case FIRE -> {
                launchFireworks(player, meta, request.amount());
                context.message(player, "utility.item.firework-fired", Map.of("amount", request.amount()));
            }
        }
        return true;
    }

    private void launchFireworks(Player player, FireworkMeta meta, int amount) {
        for (int index = 0; index < amount; index++) {
            Firework firework = player.getWorld().spawn(player.getEyeLocation(), Firework.class);
            firework.setFireworkMeta(meta);
            firework.setVelocity(player.getLocation().getDirection().multiply(0.35D));
        }
    }

    private boolean anvilRepairCost(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null) {
            return true;
        }
        if (item == null) {
            context.message(player, "utility.item.hold-required", Map.of());
            return true;
        }
        Optional<Integer> cost = AnvilRepairCostParser.parse(args);
        if (cost.isEmpty()) {
            context.message(player, "utility.item.repair-cost-usage", Map.of("label", label));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Repairable repairable)) {
            context.message(player, "utility.item.repair-cost-unsupported", Map.of());
            return true;
        }
        repairable.setRepairCost(cost.get());
        item.setItemMeta(meta);
        context.message(player, "utility.item.repair-cost-updated", Map.of("amount", cost.get()));
        return true;
    }

    private boolean unbreakable(CommandSender sender, String label, List<String> args) {
        Optional<UnbreakableCommandParser.Request> parsed = UnbreakableCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "utility.item.unbreakable-usage", Map.of("label", label));
            return true;
        }

        UnbreakableCommandParser.Request request = parsed.get();
        Player target;
        if (request.target().isPresent()) {
            String targetName = request.target().orElseThrow();
            target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                context.message(sender, "utility.item.player-offline", Map.of("target", targetName));
                return true;
            }
        } else {
            target = player(sender);
            if (target == null) {
                return true;
            }
        }

        ItemStack item = held(target);
        if (item == null) {
            context.message(sender, sender.equals(target)
                    ? "utility.item.hold-required"
                    : "utility.item.hold-required-target", Map.of("target", target.getName()));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        boolean enabled = request.state().orElse(!meta.isUnbreakable());
        meta.setUnbreakable(enabled);
        item.setItemMeta(meta);

        boolean self = sender.equals(target);
        context.message(sender, enabled
                ? (self ? "utility.item.unbreakable-enabled" : "utility.item.unbreakable-target-enabled")
                : (self ? "utility.item.unbreakable-disabled" : "utility.item.unbreakable-target-disabled"),
                Map.of("target", target.getName()));
        if (!self) {
            context.message(target, enabled
                    ? "utility.item.unbreakable-notify-enabled"
                    : "utility.item.unbreakable-notify-disabled", Map.of());
        }
        return true;
    }

    private boolean itemDye(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null) {
            return true;
        }
        if (item == null) {
            context.message(player, "utility.item.hold-required", Map.of());
            return true;
        }
        if (!(item.getItemMeta() instanceof LeatherArmorMeta meta)) {
            context.message(player, "utility.item.dye-leather-required", Map.of());
            return true;
        }
        Optional<DyeCommandParser.Request> request = DyeCommandParser.parse(args);
        if (request.isEmpty()) {
            context.message(player, args.size() == 1 ? "utility.item.dye-invalid" : "utility.item.dye-usage", Map.of("label", label));
            return true;
        }

        DyeCommandParser.Request parsed = request.get();
        if (parsed.action() == DyeCommandParser.Action.CLEAR) {
            meta.setColor(null);
            item.setItemMeta(meta);
            context.message(player, "utility.item.dye-cleared", Map.of());
            return true;
        }

        Color color = parsed.action() == DyeCommandParser.Action.RANDOM
                ? randomColor()
                : parsed.color().orElseThrow();
        meta.setColor(color);
        item.setItemMeta(meta);
        context.message(player, "utility.item.dye-updated", Map.of("color", colorHex(color)));
        return true;
    }

    private boolean itemFrame(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        ItemFrame frame = targetItemFrame(player);
        if (frame == null) {
            context.message(player, "utility.itemframe.target-required", Map.of());
            return true;
        }
        Optional<ItemFrameCommandParser.Request> request = ItemFrameCommandParser.parse(args);
        if (request.isEmpty()) {
            context.message(player, "utility.itemframe.usage", Map.of("label", label));
            return true;
        }

        applyItemFrameChange(frame, request.get().property());
        context.message(player, "utility.itemframe.updated", Map.of(
                "visible", frame.isVisible(),
                "fixed", frame.isFixed(),
                "invulnerable", frame.isInvulnerable()
        ));
        return true;
    }

    private void applyItemFrameChange(ItemFrame frame, ItemFrameCommandParser.Property property) {
        switch (property) {
            case INVISIBLE -> frame.setVisible(!frame.isVisible());
            case FIXED -> frame.setFixed(!frame.isFixed());
            case INVULNERABLE -> frame.setInvulnerable(!frame.isInvulnerable());
            case ALL -> {
                boolean enableAll = frame.isVisible() || !frame.isFixed() || !frame.isInvulnerable();
                frame.setVisible(!enableAll);
                frame.setFixed(enableAll);
                frame.setInvulnerable(enableAll);
            }
        }
    }

    private boolean itemInfo(CommandSender sender) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null) {
            return true;
        }
        if (item == null) {
            context.message(player, "utility.item.info-hold-required", Map.of());
            return true;
        }
        ItemInfoFormatter.Details details = ItemInfoFormatter.details(item);
        context.message(player, "utility.item.info-line", Map.of(
                "material", details.material(),
                "key", details.key(),
                "amount", details.amount(),
                "max_stack", details.maxStackSize(),
                "damage", details.damage(),
                "display_name", details.hasDisplayName(),
                "lore_lines", details.loreLines(),
                "enchantments", details.enchantments()
        ));
        return true;
    }

    private boolean blockInfo(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        Block block = player.getTargetBlockExact(8);
        if (block == null) {
            context.message(player, "utility.blockinfo.no-target", Map.of());
            return true;
        }
        BlockInfoFormatter.Details details = BlockInfoFormatter.details(block);
        context.message(player, "utility.blockinfo.line", Map.of(
                "material", details.material(),
                "key", details.key(),
                "location", details.location(),
                "light", details.light(),
                "biome", details.biome(),
                "solid", details.solid()
        ));
        return true;
    }

    private boolean entityInfo(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        Entity target = player.getTargetEntity(8);
        if (target == null) {
            context.message(player, "utility.entityinfo.no-target", Map.of());
            return true;
        }
        EntityInfoFormatter.Details details = EntityInfoFormatter.details(target);
        context.message(player, "utility.entityinfo.line", Map.of(
                "type", details.type(),
                "key", details.key(),
                "uuid", details.uuid(),
                "name", details.name(),
                "location", details.location(),
                "valid", details.valid(),
                "dead", details.dead()
        ));
        return true;
    }

    private boolean recipe(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }

        Optional<RecipeCommandParser.Request> parsed = RecipeCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(player, "utility.recipe.usage", Map.of("label", label));
            return true;
        }

        Optional<Material> material = recipeMaterial(player, parsed.get());
        if (material.isEmpty()) {
            return true;
        }

        Recipe recipe = firstRecipe(material.get());
        if (recipe == null) {
            context.message(player, "utility.recipe.none", Map.of("material", material.get().name().toLowerCase(Locale.ROOT)));
            return true;
        }

        context.message(player, "utility.recipe.header", Map.of(
                "material", material.get().name().toLowerCase(Locale.ROOT),
                "type", recipeType(recipe)
        ));
        for (String line : recipeLines(recipe)) {
            context.message(player, "utility.recipe.line", Map.of("line", line));
        }
        return true;
    }

    private Optional<Material> recipeMaterial(Player player, RecipeCommandParser.Request request) {
        if (request.source() == RecipeCommandParser.Source.HAND) {
            ItemStack item = held(player);
            if (item == null) {
                context.message(player, "utility.recipe.hold-required", Map.of());
                return Optional.empty();
            }
            return Optional.of(item.getType());
        }

        String materialName = request.material().orElse("");
        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem()) {
            context.message(player, "utility.recipe.unknown-material", Map.of("material", materialName));
            return Optional.empty();
        }
        return Optional.of(material);
    }

    private Recipe firstRecipe(Material material) {
        Iterator<Recipe> recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            ItemStack result = recipe.getResult();
            if (result.getType() == material) {
                return recipe;
            }
        }
        return null;
    }

    private String recipeType(Recipe recipe) {
        if (recipe instanceof ShapedRecipe) {
            return context.messages().template("utility.recipe.type.shaped", "shaped");
        }
        if (recipe instanceof ShapelessRecipe) {
            return context.messages().template("utility.recipe.type.shapeless", "shapeless");
        }
        if (recipe instanceof CookingRecipe<?>) {
            return context.messages().template("utility.recipe.type.cooking", "cooking");
        }
        if (recipe instanceof StonecuttingRecipe) {
            return context.messages().template("utility.recipe.type.stonecutting", "stonecutting");
        }
        return recipe.getClass().getSimpleName();
    }

    private List<String> recipeLines(Recipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            Map<Character, RecipeChoice> choices = shaped.getChoiceMap();
            return Arrays.stream(shaped.getShape())
                    .map(row -> row.chars()
                            .mapToObj(code -> ingredientLabel(choices.get((char) code)))
                            .collect(Collectors.joining(context.messages().template("utility.recipe.separator", " | "))))
                    .toList();
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            return List.of(context.messages().template("utility.recipe.ingredients", "Ingredients: {ingredients}")
                    .replace("{ingredients}", shapeless.getChoiceList().stream()
                            .map(this::ingredientLabel)
                            .collect(Collectors.joining(context.messages().template("utility.recipe.separator", ", ")))));
        }
        if (recipe instanceof CookingRecipe<?> cooking) {
            return List.of(context.messages().template("utility.recipe.transform", "{input} -> {result}")
                    .replace("{input}", ingredientLabel(cooking.getInputChoice()))
                    .replace("{result}", recipe.getResult().getType().name().toLowerCase(Locale.ROOT)));
        }
        if (recipe instanceof StonecuttingRecipe stonecutting) {
            return List.of(context.messages().template("utility.recipe.transform", "{input} -> {result}")
                    .replace("{input}", ingredientLabel(stonecutting.getInputChoice()))
                    .replace("{result}", recipe.getResult().getType().name().toLowerCase(Locale.ROOT)));
        }
        return List.of(context.messages().template("utility.recipe.unsupported", "This recipe type cannot be rendered yet."));
    }

    private String ingredientLabel(RecipeChoice choice) {
        if (choice == null) {
            return context.messages().template("utility.recipe.empty-slot", "-");
        }
        if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
            return materialChoice.getChoices().stream()
                    .findFirst()
                    .map(material -> material.name().toLowerCase(Locale.ROOT))
                    .orElse(context.messages().template("utility.recipe.empty-slot", "-"));
        }
        if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
            return exactChoice.getChoices().stream()
                    .findFirst()
                    .map(item -> item.getType().name().toLowerCase(Locale.ROOT))
                    .orElse(context.messages().template("utility.recipe.empty-slot", "-"));
        }
        return choice.toString();
    }

    private boolean itemCopy(CommandSender sender) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null) {
            if (player != null) {
                context.message(player, "utility.item.hold-required", Map.of());
            }
            return true;
        }
        itemClipboards.put(player.getUniqueId(), item.clone());
        context.message(player, "utility.item.copied", Map.of());
        return true;
    }

    private boolean itemPaste(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        ItemStack item = itemClipboards.get(player.getUniqueId());
        if (item == null) {
            context.message(player, "utility.item.clipboard-empty", Map.of());
            return true;
        }
        player.getInventory().addItem(item.clone());
        context.message(player, "utility.item.pasted", Map.of());
        return true;
    }

    private boolean attachCommand(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null) {
            return true;
        }
        if (item == null) {
            context.message(player, "utility.item.hold-required", Map.of());
            return true;
        }
        if (args.length < 4) {
            context.message(player, "utility.attach.usage", Map.of("label", label));
            return true;
        }
        if (!context.requirePermission(sender, "hydroxide.item.attach")) {
            return true;
        }
        Optional<AttachedCommand> parsed = AttachedCommand.parse(String.join(" ", args));
        if (parsed.isEmpty()) {
            context.message(player, "utility.attach.invalid", Map.of());
            return true;
        }
        AttachedCommand attached = parsed.get();
        if (!attached.canAttach(player.hasPermission("hydroxide.item.attach.console"))) {
            context.message(player, "utility.attach.console-permission", Map.of("permission", "hydroxide.item.attach.console"));
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, attached.serialize());
        item.setItemMeta(meta);
        context.message(player, "utility.attach.attached", Map.of());
        return true;
    }

    private boolean powerTool(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }

        ItemStack item = held(player);
        if (item == null) {
            context.message(player, "utility.powertool.hold-required", Map.of());
            return true;
        }

        Optional<PowerToolCommandParser.Request> parsed = PowerToolCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(player, "utility.powertool.usage", Map.of("label", label));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (parsed.get().action() == PowerToolCommandParser.Action.CLEAR) {
            meta.getPersistentDataContainer().remove(key());
            item.setItemMeta(meta);
            context.message(player, "utility.powertool.cleared", Map.of());
            return true;
        }

        AttachedCommand command = new AttachedCommand(AttachedCommand.Click.RIGHT, AttachedCommand.Executor.PLAYER, -1,
                parsed.get().command().orElseThrow());
        meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, command.serialize());
        item.setItemMeta(meta);
        context.message(player, "utility.powertool.attached", Map.of("command", command.command()));
        return true;
    }

    private boolean powerToolList(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }

        ItemStack item = held(player);
        Optional<AttachedCommand> command = attached(item);
        if (command.isEmpty()) {
            context.message(player, "utility.powertool.list-empty", Map.of());
            return true;
        }

        context.message(player, "utility.powertool.list-entry", PowerToolListFormatter.placeholders(command.orElseThrow()));
        return true;
    }

    private boolean powerToolToggle(CommandSender sender, String label, List<String> args) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        Optional<PowerToolToggleParser.State> state = PowerToolToggleParser.parse(args);
        if (state.isEmpty()) {
            context.message(player, "utility.powertool.toggle-usage", Map.of("label", label));
            return true;
        }

        boolean enabled = switch (state.get()) {
            case ENABLED -> true;
            case DISABLED -> false;
            case TOGGLE -> powerToolsDisabled.contains(player.getUniqueId());
        };
        if (enabled) {
            powerToolsDisabled.remove(player.getUniqueId());
            context.message(player, "utility.powertool.toggle-enabled", Map.of());
        } else {
            powerToolsDisabled.add(player.getUniqueId());
            context.message(player, "utility.powertool.toggle-disabled", Map.of());
        }
        return true;
    }

    private void execute(Player player, ItemStack item, AttachedCommand command) {
        String rendered = command.render(player.getName(), player.getUniqueId().toString(), player.getWorld().getName(),
                player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
        if (command.executor() == AttachedCommand.Executor.CONSOLE) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rendered.startsWith("/") ? rendered.substring(1) : rendered);
        } else {
            player.performCommand(rendered.startsWith("/") ? rendered.substring(1) : rendered);
        }
        Optional<AttachedCommand> updated = command.consumeUse();
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (updated.isPresent() && updated.get().usesRemaining() != 0) {
                meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, updated.get().serialize());
            } else {
                meta.getPersistentDataContainer().remove(key());
            }
            item.setItemMeta(meta);
        }
    }

    private Optional<AttachedCommand> attached(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        return AttachedCommand.parse(item.getItemMeta().getPersistentDataContainer().get(key(), PersistentDataType.STRING));
    }

    private Sign sign(Player player) {
        Block block = player.getTargetBlockExact(8);
        return block != null && block.getState() instanceof Sign sign ? sign : null;
    }

    private ItemStack held(Player player) {
        if (player == null) {
            return null;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        return item.getType() == Material.AIR ? null : item;
    }

    private ItemFrame targetItemFrame(Player player) {
        Entity target = player.getTargetEntity(8);
        return target instanceof ItemFrame itemFrame ? itemFrame : null;
    }

    private Player player(CommandSender sender) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.message(sender, "validation.player-only", Map.of("usage", ""));
        }
        return player;
    }

    private NamespacedKey key() {
        return new NamespacedKey(context.plugin(), "attached_command");
    }

    private static List<String> dyeSuggestions() {
        List<String> suggestions = new ArrayList<>(List.of("random", "clear", "reset", "#44CCFF", "255,0,0"));
        suggestions.addAll(Arrays.stream(DyeColor.values())
                .map(color -> color.name().toLowerCase(Locale.ROOT))
                .toList());
        return List.copyOf(suggestions);
    }

    private static List<String> hideFlagSuggestions() {
        List<String> suggestions = new ArrayList<>(List.of("all", "clear", "reset"));
        suggestions.addAll(Arrays.stream(ItemFlag.values())
                .map(flag -> flag.name().toLowerCase(Locale.ROOT))
                .toList());
        return List.copyOf(suggestions);
    }

    private static Color randomColor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    private static String colorHex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String argumentTail(List<String> args, int startIndex) {
        return String.join(" ", args.subList(startIndex, args.size()));
    }
}
