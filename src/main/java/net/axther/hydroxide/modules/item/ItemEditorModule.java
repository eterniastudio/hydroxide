package net.axther.hydroxide.modules.item;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.registry.ModernRegistryKeys;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ItemEditorModule implements HydroModule, CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("attribute", "lore", "model", "name");
    private static final List<String> LORE_ACTIONS = List.of("add", "remove", "set");
    private static final List<String> ATTRIBUTE_ACTIONS = List.of("add", "remove");

    private HydroxideContext context;

    @Override
    public String id() {
        return "item-editor";
    }

    @Override
    public String displayName() {
        return "Item Editor";
    }

    @Override
    public String description() {
        return "Admin commands for MiniMessage item names, lore, model data, and attributes.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        context.commands().register("item", this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.item")) {
            return true;
        }
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can edit held items.");
            return true;
        }
        if (args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " <name|lore|model|attribute> ...");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            context.send(sender, "<red>Hold an item first.");
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        boolean changed = switch (args[0].toLowerCase(Locale.ROOT)) {
            case "name" -> name(meta, args);
            case "lore" -> lore(meta, args);
            case "model" -> model(meta, args);
            case "attribute" -> attribute(meta, args);
            default -> false;
        };
        if (!changed) {
            context.send(sender, "<red>Usage: /" + label + " <name|lore|model|attribute> ...");
            return true;
        }
        item.setItemMeta(meta);
        context.send(sender, "<green>Updated held item.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtils.matching(args[0], SUBCOMMANDS);
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("lore")) {
            if (args.length == 2) {
                return CommandUtils.matching(args[1], LORE_ACTIONS);
            }
            if ((args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("remove")) && args.length == 3) {
                return net.axther.hydroxide.commands.CompletionUtils.integerRange(args[2], 1, 20);
            }
        }
        if (subcommand.equals("attribute")) {
            if (args.length == 2) {
                return CommandUtils.matching(args[1], ATTRIBUTE_ACTIONS);
            }
            if (args.length == 3) {
                return CommandUtils.matching(args[2], Registry.ATTRIBUTE.keyStream().map(NamespacedKey::getKey).toList());
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("add")) {
                return CommandUtils.matching(args[3], List.of("0", "1", "2", "5", "10", "-1"));
            }
        }
        if (subcommand.equals("model") && args.length == 2) {
            return CommandUtils.matching(args[1], List.of("0", "1", "1000", "10000"));
        }
        return List.of();
    }

    private boolean name(ItemMeta meta, String[] args) {
        meta.displayName(context.text().format(CommandUtils.joinArgs(args, 1)));
        return true;
    }

    private boolean lore(ItemMeta meta, String[] args) {
        if (args.length < 3) {
            return false;
        }
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add" -> lore.add(context.text().format(CommandUtils.joinArgs(args, 2)));
            case "set" -> {
                int line = args.length >= 4 ? parseInt(args[2], 1) - 1 : 0;
                while (lore.size() <= line) {
                    lore.add(net.kyori.adventure.text.Component.empty());
                }
                lore.set(Math.max(0, line), context.text().format(CommandUtils.joinArgs(args, args.length >= 4 ? 3 : 2)));
            }
            case "remove" -> {
                int line = parseInt(args[2], 1) - 1;
                if (line >= 0 && line < lore.size()) {
                    lore.remove(line);
                }
            }
            default -> {
                return false;
            }
        }
        meta.lore(lore);
        return true;
    }

    private boolean model(ItemMeta meta, String[] args) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setFloats(List.of((float) parseInt(args[1], 0)));
        meta.setCustomModelDataComponent(component);
        return true;
    }

    private boolean attribute(ItemMeta meta, String[] args) {
        if (args.length < 3) {
            return false;
        }
        Attribute attribute = parseAttribute(args[2]);
        if (attribute == null) {
            return false;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("remove")) {
            meta.removeAttributeModifier(attribute);
            return true;
        }
        if (!action.equals("add") || args.length < 4) {
            return false;
        }
        double amount = parseDouble(args[3], 0.0D);
        meta.addAttributeModifier(attribute, new AttributeModifier(
                new NamespacedKey(context.plugin(), "item_" + attribute.getKey().getKey()),
                amount,
                AttributeModifier.Operation.ADD_NUMBER
        ));
        return true;
    }

    private Attribute parseAttribute(String input) {
        String key = ModernRegistryKeys.minecraftKey(input);
        Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
        if (attribute != null) {
            return attribute;
        }
        return Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key.replace('.', '_')));
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
