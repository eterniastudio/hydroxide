package net.axther.hydroxide.modules.hologram;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.modules.builder.BuilderMaterialResolver;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class HologramModule implements HydroModule {

    private static final String TAG = "hydroxide_hologram";
    private static final String DEFAULT_LINE = "<#44CCFF>Hydroxide Hologram";
    private static final List<String> CREATE_TYPES = List.of("block", "item", "text");
    private static final List<String> LINE_ACTIONS = List.of("add", "remove", "set");

    private HydroxideContext context;
    private YamlStore store;

    @Override
    public String id() {
        return "holograms";
    }

    @Override
    public String displayName() {
        return "Display Holograms";
    }

    @Override
    public String description() {
        return "Modern TextDisplay, ItemDisplay, and BlockDisplay holograms with reload-safe cleanup.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "holograms.yml"));
        context.commands().register("holo", holoCommand());
        recreateAll();
    }

    @Override
    public void onDisable(HydroxideContext context) {
        removeLoadedDisplays();
    }

    @Override
    public void onReload(HydroxideContext context) {
        recreateAll();
    }

    private CommandService holoCommand() {
        return new CommandService(HydroCommand.builder("holo")
                .permission("hydroxide.holo.admin")
                .usage("/{label} create|delete|line|movehere|clone|list|near ...")
                .executor(ctx -> usage(ctx.sender(), ctx.label()))
                .child(HydroCommand.builder("create")
                        .playerOnly(true)
                        .usage("/{label} create <id> [text|item|block] [value]")
                        .executor(ctx -> create((Player) ctx.sender(), ctx.label(), ctx.arguments()))
                        .completer(this::createCompletions)
                        .build())
                .child(HydroCommand.builder("delete")
                        .usage("/{label} delete <id>")
                        .executor(ctx -> delete(ctx.sender(), ctx.label(), ctx.arguments()))
                        .completer(this::idCompletions)
                        .build())
                .child(HydroCommand.builder("line")
                        .usage("/{label} line add|set|remove <id> ...")
                        .executor(ctx -> line(ctx.sender(), ctx.label(), ctx.arguments()))
                        .completer(this::lineCompletions)
                        .build())
                .child(HydroCommand.builder("movehere")
                        .playerOnly(true)
                        .usage("/{label} movehere <id>")
                        .executor(ctx -> moveHere((Player) ctx.sender(), ctx.label(), ctx.arguments()))
                        .completer(this::idCompletions)
                        .build())
                .child(HydroCommand.builder("clone")
                        .usage("/{label} clone <id> <newId>")
                        .executor(ctx -> clone(ctx.sender(), ctx.label(), ctx.arguments()))
                        .completer(this::cloneCompletions)
                        .build())
                .child(HydroCommand.builder("list")
                        .usage("/{label} list")
                        .executor(ctx -> list(ctx.sender()))
                        .build())
                .child(HydroCommand.builder("near")
                        .playerOnly(true)
                        .usage("/{label} near [radius]")
                        .executor(ctx -> near((Player) ctx.sender(), ctx.arguments()))
                        .build())
                .build(), context.messages());
    }

    private List<String> createCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), CREATE_TYPES);
        }
        if (ctx.arguments().size() == 3 && (ctx.argument(1).equalsIgnoreCase("block") || ctx.argument(1).equalsIgnoreCase("item"))) {
            return CommandUtils.matching(ctx.argument(2), MaterialAware.materialKeys(ctx.argument(1).equalsIgnoreCase("block")));
        }
        return List.of();
    }

    private List<String> idCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), ids());
        }
        return List.of();
    }

    private List<String> cloneCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), ids()) : List.of();
    }

    private List<String> lineCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), LINE_ACTIONS);
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), ids());
        }
        return List.of();
    }

    private void create(Player player, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(player, "holograms.create.usage", Map.of("label", label));
            return;
        }
        String id = sanitize(args.get(0));
        HologramDisplayType type = args.size() >= 2 ? HologramDisplayType.from(args.get(1)).orElse(null) : HologramDisplayType.TEXT;
        if (type == null) {
            context.message(player, "holograms.create.usage", Map.of("label", label));
            return;
        }
        String payload = "";
        List<String> lines = List.of(context.messages().template("holograms.default-line", DEFAULT_LINE));
        if (type == HologramDisplayType.TEXT && args.size() >= 3) {
            lines = List.of(joinArgs(args, 2));
        } else if (type == HologramDisplayType.ITEM) {
            Material material = args.size() >= 3
                    ? BuilderMaterialResolver.resolve(args.get(2)).filter(this::isItemMaterial).orElse(null)
                    : player.getInventory().getItemInMainHand().getType();
            if (material == null || material == Material.AIR || !isItemMaterial(material)) {
                context.message(player, "holograms.create.item-usage", Map.of("label", label));
                return;
            }
            payload = material.key().asString();
            lines = List.of();
        } else if (type == HologramDisplayType.BLOCK) {
            if (args.size() < 3) {
                context.message(player, "holograms.create.block-usage", Map.of("label", label));
                return;
            }
            Material material = BuilderMaterialResolver.resolveBlock(args.get(2)).orElse(null);
            if (material == null) {
                context.message(player, "holograms.unknown-block", Map.of());
                return;
            }
            payload = material.key().asString();
            lines = List.of();
        }
        Location location = player.getLocation();
        HologramDefinition definition = new HologramDefinition(id, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), type, payload, lines);
        save(definition);
        spawn(definition);
        context.message(player, "holograms.create.success", Map.of("id", id));
    }

    private void delete(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(sender, "holograms.delete.usage", Map.of("label", label));
            return;
        }
        String id = sanitize(args.get(0));
        YamlConfiguration yaml = store.load();
        yaml.set("holograms." + id, null);
        store.save(yaml);
        removeDisplay(id);
        context.message(sender, "holograms.delete.success", Map.of("id", id));
    }

    private void line(CommandSender sender, String label, List<String> args) {
        if (args.size() < 3) {
            context.message(sender, "holograms.line.usage", Map.of("label", label));
            return;
        }
        String action = args.get(0).toLowerCase(Locale.ROOT);
        String id = sanitize(args.get(1));
        HologramDefinition definition = load(id).orElse(null);
        if (definition == null) {
            context.message(sender, "holograms.missing", Map.of("id", id));
            return;
        }
        if (definition.type() != HologramDisplayType.TEXT) {
            context.message(sender, "holograms.line.text-only", Map.of("id", id));
            return;
        }
        List<String> lines = new ArrayList<>(definition.lines());
        switch (action) {
            case "add" -> lines.add(joinArgs(args, 2));
            case "set" -> {
                if (args.size() < 4) {
                    context.message(sender, "holograms.line.set-usage", Map.of("label", label));
                    return;
                }
                int index = Math.max(0, parseInt(args.get(2), 1) - 1);
                while (lines.size() <= index) {
                    lines.add("");
                }
                lines.set(index, joinArgs(args, 3));
            }
            case "remove" -> {
                int index = Math.max(0, parseInt(args.get(2), 1) - 1);
                if (index < lines.size()) {
                    lines.remove(index);
                }
            }
            default -> {
                context.message(sender, "holograms.line.usage", Map.of("label", label));
                return;
            }
        }
        HologramDefinition updated = new HologramDefinition(definition.id(), definition.worldName(), definition.x(), definition.y(), definition.z(), definition.type(), definition.payload(), lines.isEmpty() ? List.of("") : lines);
        save(updated);
        recreate(updated);
        context.message(sender, "holograms.line.updated", Map.of("id", id));
    }

    private void moveHere(Player player, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(player, "holograms.movehere.usage", Map.of("label", label));
            return;
        }
        String id = sanitize(args.get(0));
        HologramDefinition definition = load(id).orElse(null);
        if (definition == null) {
            context.message(player, "holograms.missing", Map.of("id", id));
            return;
        }
        Location location = player.getLocation();
        HologramDefinition moved = new HologramDefinition(definition.id(), location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), definition.type(), definition.payload(), definition.lines());
        save(moved);
        recreate(moved);
        context.message(player, "holograms.movehere.success", Map.of("id", id));
    }

    private void clone(CommandSender sender, String label, List<String> args) {
        if (args.size() < 2) {
            context.message(sender, "holograms.clone.usage", Map.of("label", label));
            return;
        }
        String sourceId = sanitize(args.get(0));
        HologramDefinition source = load(sourceId).orElse(null);
        if (source == null) {
            context.message(sender, "holograms.missing", Map.of("id", sourceId));
            return;
        }
        String copyId = sanitize(args.get(1));
        HologramDefinition copy = new HologramDefinition(copyId, source.worldName(), source.x(), source.y(), source.z(), source.type(), source.payload(), source.lines());
        save(copy);
        spawn(copy);
        context.message(sender, "holograms.clone.success", Map.of("id", copyId));
    }

    private void list(CommandSender sender) {
        List<String> labels = definitions().stream()
                .map(definition -> definition.id() + " (" + definition.type().key() + ")")
                .toList();
        if (labels.isEmpty()) {
            context.message(sender, "holograms.list.empty", Map.of());
            return;
        }
        context.message(sender, "holograms.list.entries", Map.of("entries", String.join(", ", labels)));
    }

    private void near(Player player, List<String> args) {
        double radius = args.isEmpty() ? 25.0D : parseDouble(args.get(0), 25.0D);
        List<String> nearby = definitions().stream()
                .filter(definition -> definition.worldName().equals(player.getWorld().getName()))
                .filter(definition -> player.getLocation().distanceSquared(new Location(player.getWorld(), definition.x(), definition.y(), definition.z())) <= radius * radius)
                .map(HologramDefinition::id)
                .toList();
        if (nearby.isEmpty()) {
            context.message(player, "holograms.near.empty", Map.of("radius", radius));
            return;
        }
        context.message(player, "holograms.near.entries", Map.of("entries", String.join(", ", nearby), "radius", radius));
    }

    private void recreateAll() {
        removeLoadedDisplays();
        definitions().forEach(this::spawn);
    }

    private void recreate(HologramDefinition definition) {
        removeDisplay(definition.id());
        spawn(definition);
    }

    private void spawn(HologramDefinition definition) {
        World world = Bukkit.getWorld(definition.worldName());
        if (world == null) {
            return;
        }
        Location location = new Location(world, definition.x(), definition.y(), definition.z());
        switch (definition.type()) {
            case TEXT -> spawnText(definition, world, location);
            case ITEM -> spawnItem(definition, world, location);
            case BLOCK -> spawnBlock(definition, world, location);
        }
    }

    private void spawnText(HologramDefinition definition, World world, Location location) {
        TextDisplay display = (TextDisplay) world.spawnEntity(location, EntityType.TEXT_DISPLAY);
        tagDisplay(display, definition);
        display.text(render(definition.lines().isEmpty() ? List.of("") : definition.lines()));
        display.setSeeThrough(true);
        display.setShadowed(true);
    }

    private void spawnItem(HologramDefinition definition, World world, Location location) {
        Material material = BuilderMaterialResolver.resolve(definition.payload()).filter(this::isItemMaterial).orElse(Material.BARRIER);
        ItemDisplay display = (ItemDisplay) world.spawnEntity(location, EntityType.ITEM_DISPLAY);
        tagDisplay(display, definition);
        display.setItemStack(new ItemStack(material));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
    }

    private void spawnBlock(HologramDefinition definition, World world, Location location) {
        Material material = BuilderMaterialResolver.resolveBlock(definition.payload()).orElse(Material.BARRIER);
        BlockDisplay display = (BlockDisplay) world.spawnEntity(location, EntityType.BLOCK_DISPLAY);
        tagDisplay(display, definition);
        display.setBlock(material.createBlockData());
    }

    private void tagDisplay(Display display, HologramDefinition definition) {
        display.addScoreboardTag(TAG);
        display.addScoreboardTag(tag(definition.id()));
        display.setPersistent(true);
        display.setBillboard(Display.Billboard.CENTER);
    }

    private Component render(List<String> lines) {
        Component component = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                component = component.append(Component.newline());
            }
            component = component.append(context.text().format(lines.get(index)));
        }
        return component;
    }

    private void removeLoadedDisplays() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(TAG)) {
                    entity.remove();
                }
            }
        }
    }

    private void removeDisplay(String id) {
        String tag = tag(id);
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(tag)) {
                    entity.remove();
                }
            }
        }
    }

    private List<HologramDefinition> definitions() {
        YamlConfiguration yaml = store.load();
        ConfigurationSection section = yaml.getConfigurationSection("holograms");
        if (section == null) {
            return List.of();
        }
        return section.getKeys(false).stream()
                .map(id -> HologramDefinition.read(id, section.getConfigurationSection(id)))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(HologramDefinition::id))
                .toList();
    }

    private Optional<HologramDefinition> load(String id) {
        return HologramDefinition.read(id, store.load().getConfigurationSection("holograms." + id));
    }

    private void save(HologramDefinition definition) {
        YamlConfiguration yaml = store.load();
        definition.write(yaml.createSection("holograms." + definition.id()));
        store.save(yaml);
    }

    private List<String> ids() {
        return definitions().stream().map(HologramDefinition::id).toList();
    }

    private void usage(CommandSender sender, String label) {
        context.message(sender, "holograms.usage", Map.of("label", label));
    }

    private String joinArgs(List<String> args, int startIndex) {
        if (startIndex >= args.size()) {
            return "";
        }
        return String.join(" ", args.subList(startIndex, args.size()));
    }

    private String sanitize(String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    private String tag(String id) {
        return "hx_holo_" + sanitize(id);
    }

    private boolean isItemMaterial(Material material) {
        try {
            return material.isItem();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static final class MaterialAware {
        private MaterialAware() {
        }

        private static List<String> materialKeys(boolean blocksOnly) {
            return java.util.Arrays.stream(Material.values())
                    .filter(material -> !blocksOnly || material.isBlock())
                    .map(material -> material.key().asString())
                    .toList();
        }
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
