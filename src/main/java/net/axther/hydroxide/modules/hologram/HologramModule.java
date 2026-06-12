package net.axther.hydroxide.modules.hologram;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.modules.builder.BuilderMaterialResolver;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
import java.util.Optional;

public final class HologramModule implements HydroModule, CommandExecutor, TabCompleter {

    private static final String TAG = "hydroxide_hologram";

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
        context.commands().register("holo", this);
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.holo.admin")) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " create|delete|line|movehere|clone|list|near ...");
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, label, args);
            case "delete" -> delete(sender, label, args);
            case "line" -> line(sender, label, args);
            case "movehere" -> moveHere(sender, label, args);
            case "clone" -> clone(sender, label, args);
            case "list" -> list(sender);
            case "near" -> near(sender, args);
            default -> {
                context.send(sender, "<red>Usage: /" + label + " create|delete|line|movehere|clone|list|near ...");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtils.matching(args[0], List.of("clone", "create", "delete", "line", "list", "movehere", "near"));
        }
        if ((args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("movehere") || args[0].equalsIgnoreCase("clone")) && args.length == 2) {
            return CommandUtils.matching(args[1], ids());
        }
        if (args[0].equalsIgnoreCase("create") && args.length == 3) {
            return CommandUtils.matching(args[2], List.of("block", "item", "text"));
        }
        if (args[0].equalsIgnoreCase("create") && args.length == 4 && (args[2].equalsIgnoreCase("block") || args[2].equalsIgnoreCase("item"))) {
            return CommandUtils.matching(args[3], MaterialAware.materialKeys(args[2].equalsIgnoreCase("block")));
        }
        if (args[0].equalsIgnoreCase("line")) {
            if (args.length == 2) {
                return CommandUtils.matching(args[1], List.of("add", "remove", "set"));
            }
            if (args.length == 3) {
                return CommandUtils.matching(args[2], ids());
            }
        }
        return List.of();
    }

    private boolean create(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " create <id> [text|item|block] [value]");
            return true;
        }
        String id = sanitize(args[1]);
        HologramDisplayType type = args.length >= 3 ? HologramDisplayType.from(args[2]).orElse(null) : HologramDisplayType.TEXT;
        if (type == null) {
            context.send(sender, "<red>Usage: /" + label + " create <id> [text|item|block] [value]");
            return true;
        }
        String payload = "";
        List<String> lines = List.of("<#44CCFF>Hydroxide Hologram");
        if (type == HologramDisplayType.TEXT && args.length >= 4) {
            lines = List.of(CommandUtils.joinArgs(args, 3));
        } else if (type == HologramDisplayType.ITEM) {
            Material material = args.length >= 4
                    ? BuilderMaterialResolver.resolve(args[3]).filter(this::isItemMaterial).orElse(null)
                    : player.getInventory().getItemInMainHand().getType();
            if (material == null || material == Material.AIR || !isItemMaterial(material)) {
                context.send(sender, "<red>Usage: /" + label + " create <id> item <material>");
                return true;
            }
            payload = material.key().asString();
            lines = List.of();
        } else if (type == HologramDisplayType.BLOCK) {
            if (args.length < 4) {
                context.send(sender, "<red>Usage: /" + label + " create <id> block <block>");
                return true;
            }
            Material material = BuilderMaterialResolver.resolveBlock(args[3]).orElse(null);
            if (material == null) {
                context.send(sender, "<red>Unknown block.");
                return true;
            }
            payload = material.key().asString();
            lines = List.of();
        }
        Location location = player.getLocation();
        HologramDefinition definition = new HologramDefinition(id, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), type, payload, lines);
        save(definition);
        spawn(definition);
        context.send(sender, "<green>Hologram <white>" + id + " <green>created.");
        return true;
    }

    private boolean delete(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " delete <id>");
            return true;
        }
        String id = sanitize(args[1]);
        YamlConfiguration yaml = store.load();
        yaml.set("holograms." + id, null);
        store.save(yaml);
        removeDisplay(id);
        context.send(sender, "<green>Hologram deleted.");
        return true;
    }

    private boolean line(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            context.send(sender, "<red>Usage: /" + label + " line add|set|remove <id> ...");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String id = sanitize(args[2]);
        HologramDefinition definition = load(id).orElse(null);
        if (definition == null) {
            context.send(sender, "<red>That hologram does not exist.");
            return true;
        }
        if (definition.type() != HologramDisplayType.TEXT) {
            context.send(sender, "<red>Only text holograms have editable lines.");
            return true;
        }
        List<String> lines = new ArrayList<>(definition.lines());
        switch (action) {
            case "add" -> lines.add(CommandUtils.joinArgs(args, 3));
            case "set" -> {
                if (args.length < 5) {
                    context.send(sender, "<red>Usage: /" + label + " line set <id> <line> <text>");
                    return true;
                }
                int index = Math.max(0, parseInt(args[3], 1) - 1);
                while (lines.size() <= index) {
                    lines.add("");
                }
                lines.set(index, CommandUtils.joinArgs(args, 4));
            }
            case "remove" -> {
                int index = Math.max(0, parseInt(args[3], 1) - 1);
                if (index < lines.size()) {
                    lines.remove(index);
                }
            }
            default -> {
                context.send(sender, "<red>Usage: /" + label + " line add|set|remove <id> ...");
                return true;
            }
        }
        HologramDefinition updated = new HologramDefinition(definition.id(), definition.worldName(), definition.x(), definition.y(), definition.z(), definition.type(), definition.payload(), lines.isEmpty() ? List.of("") : lines);
        save(updated);
        recreate(updated);
        context.send(sender, "<green>Hologram lines updated.");
        return true;
    }

    private boolean moveHere(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " movehere <id>");
            return true;
        }
        HologramDefinition definition = load(sanitize(args[1])).orElse(null);
        if (definition == null) {
            context.send(sender, "<red>That hologram does not exist.");
            return true;
        }
        Location location = player.getLocation();
        HologramDefinition moved = new HologramDefinition(definition.id(), location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), definition.type(), definition.payload(), definition.lines());
        save(moved);
        recreate(moved);
        context.send(sender, "<green>Hologram moved.");
        return true;
    }

    private boolean clone(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            context.send(sender, "<red>Usage: /" + label + " clone <id> <newId>");
            return true;
        }
        HologramDefinition source = load(sanitize(args[1])).orElse(null);
        if (source == null) {
            context.send(sender, "<red>That hologram does not exist.");
            return true;
        }
        HologramDefinition copy = new HologramDefinition(sanitize(args[2]), source.worldName(), source.x(), source.y(), source.z(), source.type(), source.payload(), source.lines());
        save(copy);
        spawn(copy);
        context.send(sender, "<green>Hologram cloned.");
        return true;
    }

    private boolean list(CommandSender sender) {
        List<String> ids = ids();
        List<String> labels = definitions().stream()
                .map(definition -> definition.id() + " (" + definition.type().key() + ")")
                .toList();
        context.send(sender, labels.isEmpty() ? "<gray>No holograms have been created." : "<green>Holograms: <white>" + String.join("<gray>, <white>", labels));
        return true;
    }

    private boolean near(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        double radius = args.length > 1 ? parseDouble(args[1], 25.0D) : 25.0D;
        List<String> nearby = definitions().stream()
                .filter(definition -> definition.worldName().equals(player.getWorld().getName()))
                .filter(definition -> player.getLocation().distanceSquared(new Location(player.getWorld(), definition.x(), definition.y(), definition.z())) <= radius * radius)
                .map(HologramDefinition::id)
                .toList();
        context.send(sender, nearby.isEmpty() ? "<gray>No nearby holograms." : "<green>Nearby holograms: <white>" + String.join("<gray>, <white>", nearby));
        return true;
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

    private Player requirePlayer(CommandSender sender) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can use that hologram command.");
        }
        return player;
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
