package net.axther.hydroxide.modules.item;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.registry.ModernRegistryKeys;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ItemEditorModule implements HydroModule {

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
        context.commands().register("item", itemCommand());
    }

    private CommandService itemCommand() {
        return new CommandService(HydroCommand.builder("item")
                .permission("hydroxide.command.item")
                .playerOnly(true)
                .usage("/{label} <name|lore|model|attribute> ...")
                .executor(ctx -> item((Player) ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(this::itemCompletions)
                .build(), context.messages());
    }

    private void item(Player player, String label, String[] args) {
        if (args.length < 2) {
            usage(player, label);
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            context.message(player, "item.hold-required", Map.of());
            return;
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
            usage(player, label);
            return;
        }
        item.setItemMeta(meta);
        context.message(player, "item.updated", Map.of());
    }

    private List<String> itemCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), SUBCOMMANDS);
        }
        String subcommand = ctx.argument(0).toLowerCase(Locale.ROOT);
        if (subcommand.equals("lore")) {
            if (ctx.arguments().size() == 2) {
                return CommandUtils.matching(ctx.argument(1), LORE_ACTIONS);
            }
            if ((ctx.argument(1).equalsIgnoreCase("set") || ctx.argument(1).equalsIgnoreCase("remove")) && ctx.arguments().size() == 3) {
                return net.axther.hydroxide.commands.CompletionUtils.integerRange(ctx.argument(2), 1, 20);
            }
        }
        if (subcommand.equals("attribute")) {
            if (ctx.arguments().size() == 2) {
                return CommandUtils.matching(ctx.argument(1), ATTRIBUTE_ACTIONS);
            }
            if (ctx.arguments().size() == 3) {
                return CommandUtils.matching(ctx.argument(2), Registry.ATTRIBUTE.keyStream().map(NamespacedKey::getKey).toList());
            }
            if (ctx.arguments().size() == 4 && ctx.argument(1).equalsIgnoreCase("add")) {
                return CommandUtils.matching(ctx.argument(3), List.of("0", "1", "2", "5", "10", "-1"));
            }
        }
        if (subcommand.equals("model") && ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("0", "1", "1000", "10000"));
        }
        return List.of();
    }

    private void usage(Player player, String label) {
        context.message(player, "item.usage", Map.of("label", label));
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
