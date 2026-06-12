package net.axther.hydroxide.modules.utility;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.kyori.adventure.text.Component;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class UtilityBindingModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private static final List<String> COMMANDS = List.of(
            "signedit", "signcopy", "signpaste", "signglow", "bookedit",
            "itemname", "itemlore", "itemflag", "itemenchant", "itemrepair", "itemcopy", "itempaste", "attachcommand"
    );

    private HydroxideContext context;
    private final Map<UUID, List<String>> signClipboards = new HashMap<>();
    private final Map<UUID, ItemStack> itemClipboards = new HashMap<>();

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
        for (String command : COMMANDS) {
            context.commands().register(command, this);
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "signedit" -> signEdit(sender, label, args);
            case "signcopy" -> signCopy(sender);
            case "signpaste" -> signPaste(sender);
            case "signglow" -> signGlow(sender, args);
            case "bookedit" -> bookEdit(sender, label, args);
            case "itemname" -> itemName(sender, label, args);
            case "itemlore" -> itemLore(sender, label, args);
            case "itemflag" -> itemFlag(sender, label, args);
            case "itemenchant" -> itemEnchant(sender, label, args);
            case "itemrepair" -> itemRepair(sender);
            case "itemcopy" -> itemCopy(sender);
            case "itempaste" -> itemPaste(sender);
            case "attachcommand" -> attachCommand(sender, label, args);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("attachcommand")) {
            if (args.length == 1) {
                return CommandUtils.matching(args[0], List.of("left", "right"));
            }
            if (args.length == 2) {
                return CommandUtils.matching(args[1], List.of("console", "player"));
            }
            if (args.length == 3) {
                return CommandUtils.matching(args[2], List.of("1", "3", "10", "infinite"));
            }
        }
        if (name.equals("itemflag") && args.length == 1) {
            return CommandUtils.matching(args[0], java.util.Arrays.stream(ItemFlag.values()).map(Enum::name).map(String::toLowerCase).toList());
        }
        if (name.equals("bookedit") && args.length == 1) {
            return CommandUtils.matching(args[0], List.of("addpage", "author", "page", "title"));
        }
        if (name.equals("signglow") && args.length == 1) {
            return CommandUtils.matching(args[0], java.util.Arrays.stream(DyeColor.values()).map(Enum::name).map(String::toLowerCase).toList());
        }
        return List.of();
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
            context.send(player, "<red>Look at a sign first.");
            return true;
        }
        if (args.length < 2) {
            context.send(player, "<red>Usage: /" + label + " <1-4> <text>");
            return true;
        }
        int line = Math.max(0, Math.min(3, parseInt(args[0], 1) - 1));
        sign.getSide(Side.FRONT).line(line, context.text().format(CommandUtils.joinArgs(args, 1)));
        sign.update(true);
        context.send(player, "<green>Sign updated.");
        return true;
    }

    private boolean signCopy(CommandSender sender) {
        Player player = player(sender);
        Sign sign = player == null ? null : sign(player);
        if (player == null || sign == null) {
            context.send(sender, "<red>Look at a sign first.");
            return true;
        }
        signClipboards.put(player.getUniqueId(), sign.getSide(Side.FRONT).lines().stream().map(context.text()::plain).toList());
        context.send(player, "<green>Sign copied.");
        return true;
    }

    private boolean signPaste(CommandSender sender) {
        Player player = player(sender);
        Sign sign = player == null ? null : sign(player);
        if (player == null || sign == null) {
            context.send(sender, "<red>Look at a sign first.");
            return true;
        }
        List<String> lines = signClipboards.get(player.getUniqueId());
        if (lines == null) {
            context.send(player, "<red>Your sign clipboard is empty.");
            return true;
        }
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            sign.getSide(Side.FRONT).line(i, context.text().format(lines.get(i)));
        }
        sign.update(true);
        context.send(player, "<green>Sign pasted.");
        return true;
    }

    private boolean signGlow(CommandSender sender, String[] args) {
        Player player = player(sender);
        Sign sign = player == null ? null : sign(player);
        if (player == null || sign == null) {
            context.send(sender, "<red>Look at a sign first.");
            return true;
        }
        SignSide side = sign.getSide(Side.FRONT);
        side.setGlowingText(true);
        if (args.length > 0) {
            try {
                side.setColor(DyeColor.valueOf(args[0].toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                context.send(player, "<red>Unknown dye color.");
                return true;
            }
        }
        sign.update(true);
        context.send(player, "<green>Sign glow updated.");
        return true;
    }

    private boolean bookEdit(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null || !(item.getItemMeta() instanceof BookMeta meta)) {
            context.send(sender, "<red>Hold a writable or written book.");
            return true;
        }
        if (args.length < 2) {
            context.send(player, "<red>Usage: /" + label + " title|author|page|addpage ...");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "title" -> meta.title(context.text().format(CommandUtils.joinArgs(args, 1)));
            case "author" -> meta.author(context.text().format(CommandUtils.joinArgs(args, 1)));
            case "addpage" -> meta.addPages(context.text().format(CommandUtils.joinArgs(args, 1)));
            case "page" -> {
                if (args.length < 3) {
                    return true;
                }
                int page = Math.max(1, parseInt(args[1], 1));
                while (meta.getPageCount() < page) {
                    meta.addPages(Component.empty());
                }
                meta.page(page, context.text().format(CommandUtils.joinArgs(args, 2)));
            }
            default -> {
                context.send(player, "<red>Usage: /" + label + " title|author|page|addpage ...");
                return true;
            }
        }
        item.setItemMeta(meta);
        context.send(player, "<green>Book updated.");
        return true;
    }

    private boolean itemName(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null || args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <name>");
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        meta.displayName(context.text().format(CommandUtils.joinArgs(args, 0)));
        item.setItemMeta(meta);
        context.send(player, "<green>Item name updated.");
        return true;
    }

    private boolean itemLore(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null || args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " add|set|remove ...");
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new java.util.ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> lore.add(context.text().format(CommandUtils.joinArgs(args, 1)));
            case "set" -> {
                if (args.length < 3) {
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
                context.send(player, "<red>Usage: /" + label + " add|set|remove ...");
                return true;
            }
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        context.send(player, "<green>Item lore updated.");
        return true;
    }

    private boolean itemFlag(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null || args.length < 1) {
            context.send(sender, "<red>Usage: /" + label + " <flag>");
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
            context.send(player, "<green>Item flag toggled.");
        } catch (IllegalArgumentException exception) {
            context.send(player, "<red>Unknown item flag.");
        }
        return true;
    }

    private boolean itemEnchant(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null || args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " <enchantment> <level>");
            return true;
        }
        Enchantment enchantment = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft(args[0].toLowerCase(Locale.ROOT).replace("minecraft:", "")));
        if (enchantment == null) {
            context.send(player, "<red>Unknown enchantment.");
            return true;
        }
        item.addUnsafeEnchantment(enchantment, Math.max(1, parseInt(args[1], 1)));
        context.send(player, "<green>Item enchanted.");
        return true;
    }

    private boolean itemRepair(CommandSender sender) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
            item.setItemMeta(meta);
        }
        context.send(player, "<green>Item repaired.");
        return true;
    }

    private boolean itemCopy(CommandSender sender) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null) {
            return true;
        }
        itemClipboards.put(player.getUniqueId(), item.clone());
        context.send(player, "<green>Item copied.");
        return true;
    }

    private boolean itemPaste(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        ItemStack item = itemClipboards.get(player.getUniqueId());
        if (item == null) {
            context.send(player, "<red>Your item clipboard is empty.");
            return true;
        }
        player.getInventory().addItem(item.clone());
        context.send(player, "<green>Item pasted.");
        return true;
    }

    private boolean attachCommand(CommandSender sender, String label, String[] args) {
        Player player = player(sender);
        ItemStack item = held(player);
        if (player == null || item == null || args.length < 4) {
            context.send(sender, "<red>Usage: /" + label + " <left|right> <player|console> <uses|infinite> <command...>");
            return true;
        }
        if (!context.requirePermission(sender, "hydroxide.item.attach")) {
            return true;
        }
        Optional<AttachedCommand> parsed = AttachedCommand.parse(String.join(" ", args));
        if (parsed.isEmpty()) {
            context.send(player, "<red>Invalid attached command.");
            return true;
        }
        AttachedCommand attached = parsed.get();
        if (!attached.canAttach(player.hasPermission("hydroxide.item.attach.console"))) {
            context.send(player, "<red>You need <white>hydroxide.item.attach.console <red>to bind console commands.");
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, attached.serialize());
        item.setItemMeta(meta);
        context.send(player, "<green>Command attached to item.");
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

    private Player player(CommandSender sender) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can use this command.");
        }
        return player;
    }

    private NamespacedKey key() {
        return new NamespacedKey(context.plugin(), "attached_command");
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
