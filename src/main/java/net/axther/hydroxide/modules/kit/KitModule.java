package net.axther.hydroxide.modules.kit;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class KitModule implements HydroModule, Listener {
    private HydroxideContext context;
    private YamlStore kitStore;
    private YamlStore cooldownStore;

    @Override
    public String id() {
        return "kits";
    }

    @Override
    public String displayName() {
        return "Kits";
    }

    @Override
    public String description() {
        return "Captures, previews, and distributes serialized item kits with cooldowns.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.kitStore = new YamlStore(new File(context.plugin().getDataFolder(), "kits.yml"));
        this.cooldownStore = new YamlStore(new File(context.plugin().getDataFolder(), "data/kit-cooldowns.yml"));
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("kit", kitCommand());
        context.commands().register("kits", kitsCommand());
        context.commands().register("setkit", setKitCommand());
        context.commands().register("createkit", createKitCommand());
        context.commands().register("delkit", deleteKitCommand());
        context.commands().register("showkit", showKitCommand());
        context.commands().register("kitcdreset", kitCooldownResetCommand());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    private CommandService kitCommand() {
        return new CommandService(HydroCommand.builder("kit")
                .permission("hydroxide.command.kit")
                .usage("/{label} [kit] [player] [-s] [-open|-preview] [-c]")
                .executor(ctx -> kit(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::kitCompletions)
                .build(), context.messages());
    }

    private CommandService kitsCommand() {
        return new CommandService(HydroCommand.builder("kits")
                .permission("hydroxide.command.kits")
                .playerOnly(true)
                .usage("/{label}")
                .executor(ctx -> openMenu(ctx.sender()))
                .build(), context.messages());
    }

    private CommandService setKitCommand() {
        return new CommandService(HydroCommand.builder("setkit")
                .permission("hydroxide.command.setkit")
                .playerOnly(true)
                .usage("/{label} <name> [cooldownSeconds]")
                .executor(ctx -> setKit(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(this::setKitCompletions)
                .build(), context.messages());
    }

    private CommandService createKitCommand() {
        return new CommandService(HydroCommand.builder("createkit")
                .permission("hydroxide.command.createkit")
                .playerOnly(true)
                .usage("/{label} <name> [cooldownSeconds]")
                .executor(ctx -> setKit(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(this::setKitCompletions)
                .build(), context.messages());
    }

    private CommandService deleteKitCommand() {
        return new CommandService(HydroCommand.builder("delkit")
                .permission("hydroxide.command.delkit")
                .usage("/{label} <kit>")
                .executor(ctx -> deleteKit(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::deleteKitCompletions)
                .build(), context.messages());
    }

    private CommandService showKitCommand() {
        return new CommandService(HydroCommand.builder("showkit")
                .permission("hydroxide.command.showkit")
                .usage("/{label} <kit> [player]")
                .executor(ctx -> showKit(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::showKitCompletions)
                .build(), context.messages());
    }

    private CommandService kitCooldownResetCommand() {
        return new CommandService(HydroCommand.builder("kitcdreset")
                .permission("hydroxide.command.kitcdreset")
                .usage("/{label} <kit> <player|all>")
                .executor(ctx -> resetKitCooldown(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::kitCooldownResetCompletions)
                .build(), context.messages());
    }

    private List<String> kitCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), kitNames());
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new ArrayList<>(List.of("-preview", "-open", "-s", "-c"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(1), values);
        }
        return CommandUtils.matching(ctx.argument(ctx.arguments().size() - 1), List.of("-preview", "-open", "-s", "-c"));
    }

    private List<String> setKitCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), kitNames());
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("0", "60", "300", "3600", "86400"));
        }
        return List.of();
    }

    private List<String> deleteKitCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), kitNames()) : List.of();
    }

    private List<String> showKitCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), kitNames());
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return List.of();
    }

    private List<String> kitCooldownResetCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), kitNames());
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new ArrayList<>(List.of("all"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(1), values);
        }
        return List.of();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = context.text().plain(event.getView().title());
        if (title.startsWith(previewTitlePrefix())) {
            event.setCancelled(true);
            return;
        }
        if (!title.equals(menuTitle())) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String kit = context.text().plain(item.getItemMeta().displayName());
        if (event.isRightClick()) {
            player.openInventory(previewInventory(kit));
        } else {
            giveKit(player, player, kit, false, false);
        }
    }

    private boolean setKit(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            context.message(sender, "kits.set.usage", Map.of("label", label));
            return true;
        }
        YamlConfiguration yaml = kitStore.load();
        String kit = args[0].toLowerCase(Locale.ROOT);
        long cooldown = args.length >= 2 ? parseLong(args[1], 0L) : 0L;
        String path = "kits." + kit;
        yaml.set(path + ".cooldown-seconds", cooldown);
        yaml.set(path + ".permission", "hydroxide.kit." + kit);
        yaml.set(path + ".items", serialize(player.getInventory().getContents()));
        kitStore.save(yaml);
        context.message(sender, "kits.set.captured", Map.of("kit", kit, "cooldown", cooldown));
        return true;
    }

    private boolean deleteKit(CommandSender sender, String label, List<String> args) {
        if (args.size() != 1) {
            context.message(sender, "kits.delete.usage", Map.of("label", label));
            return true;
        }

        String kit = args.getFirst().toLowerCase(Locale.ROOT);
        YamlConfiguration kits = kitStore.load();
        YamlConfiguration cooldowns = cooldownStore.load();
        if (!KitStoreEditor.delete(kits, cooldowns, kit)) {
            context.message(sender, "kits.delete.missing", Map.of("kit", kit));
            return true;
        }

        kitStore.save(kits);
        cooldownStore.save(cooldowns);
        context.message(sender, "kits.delete.success", Map.of("kit", kit));
        return true;
    }

    private boolean showKit(CommandSender sender, String label, List<String> args) {
        Optional<ShowKitCommandParser.Request> parsed = ShowKitCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "kits.show.usage", Map.of("label", label));
            return true;
        }

        ShowKitCommandParser.Request request = parsed.orElseThrow();
        if (kitStore.load().getConfigurationSection("kits." + request.kit()) == null) {
            context.message(sender, "kits.claim.missing", Map.of("kit", request.kit()));
            return true;
        }

        Player target = showKitTarget(sender, request).orElse(null);
        if (target == null) {
            return true;
        }
        if (request.target().isPresent() && !sender.equals(target) && !sender.hasPermission("hydroxide.command.showkit.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.showkit.others"));
            return true;
        }

        target.openInventory(previewInventory(request.kit()));
        if (!sender.equals(target)) {
            context.message(sender, "kits.claim.opened", Map.of("kit", request.kit(), "target", target.getName()));
        }
        return true;
    }

    private Optional<Player> showKitTarget(CommandSender sender, ShowKitCommandParser.Request request) {
        if (request.target().isEmpty()) {
            if (sender instanceof Player player) {
                return Optional.of(player);
            }
            context.message(sender, "kits.show.usage", Map.of("label", "showkit"));
            return Optional.empty();
        }
        String targetName = request.target().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "kits.claim.target-offline", Map.of("target", targetName));
            return Optional.empty();
        }
        return Optional.of(target);
    }

    private boolean openMenu(CommandSender sender) {
        Player player = (Player) sender;
        player.openInventory(menuInventory(player));
        return true;
    }

    private boolean kit(CommandSender sender, String label, List<String> args) {
        Optional<KitCommandParser.Request> parsed = KitCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "kits.claim.usage", Map.of("label", label));
            return true;
        }
        KitCommandParser.Request request = parsed.orElseThrow();
        if (request.mode() == KitCommandParser.Mode.MENU) {
            if (sender instanceof Player player) {
                player.openInventory(menuInventory(player));
            } else {
                context.message(sender, "kits.claim.usage", Map.of("label", label));
            }
            return true;
        }

        Player target = kitTarget(sender, request).orElse(null);
        if (target == null) {
            return true;
        }
        if (request.target().isPresent() && !sender.equals(target) && !sender.hasPermission("hydroxide.command.kit.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.kit.others"));
            return true;
        }
        String kit = request.kit().orElseThrow();
        if (request.mode() == KitCommandParser.Mode.PREVIEW || request.mode() == KitCommandParser.Mode.OPEN) {
            target.openInventory(previewInventory(kit));
            if (!sender.equals(target)) {
                context.message(sender, "kits.claim.opened", Map.of("kit", kit, "target", target.getName()));
            }
            return true;
        }
        giveKit(sender, target, kit, request.silent(), request.ignoreCooldown());
        return true;
    }

    private Optional<Player> kitTarget(CommandSender sender, KitCommandParser.Request request) {
        if (request.target().isEmpty()) {
            if (sender instanceof Player player) {
                return Optional.of(player);
            }
            context.message(sender, "kits.claim.usage", Map.of("label", "kit"));
            return Optional.empty();
        }
        String targetName = request.target().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "kits.claim.target-offline", Map.of("target", targetName));
            return Optional.empty();
        }
        return Optional.of(target);
    }

    private void giveKit(CommandSender sender, Player player, String kit, boolean silent, boolean ignoreCooldown) {
        ConfigurationSection section = kitStore.load().getConfigurationSection("kits." + kit.toLowerCase(Locale.ROOT));
        if (section == null) {
            context.message(sender, "kits.claim.missing", Map.of("kit", kit));
            return;
        }
        String permission = section.getString("permission", "");
        if (!permission.isBlank() && !player.hasPermission(permission)) {
            context.message(sender, "kits.claim.no-permission", Map.of("kit", kit));
            return;
        }
        long cooldownUntil = cooldownStore.load().getLong(player.getUniqueId() + "." + kit, 0L);
        if (!ignoreCooldown && cooldownUntil > System.currentTimeMillis()) {
            long remaining = Math.max(1L, (long) Math.ceil((cooldownUntil - System.currentTimeMillis()) / 1000.0D));
            context.message(sender, "kits.claim.cooldown", Map.of("kit", kit, "remaining", remaining));
            return;
        }
        for (ItemStack item : deserialize(section.getMapList("items"))) {
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
            }
        }
        long cooldown = section.getLong("cooldown-seconds", 0L);
        if (cooldown > 0) {
            YamlConfiguration yaml = cooldownStore.load();
            yaml.set(player.getUniqueId() + "." + kit, Instant.now().plusSeconds(cooldown).toEpochMilli());
            cooldownStore.save(yaml);
        }
        if (sender.equals(player)) {
            context.message(player, "kits.claim.claimed", Map.of("kit", kit));
        } else {
            context.message(sender, "kits.claim.given", Map.of("kit", kit, "target", player.getName()));
            if (!silent) {
                context.message(player, "kits.claim.received", Map.of("kit", kit, "player", sender.getName()));
            }
        }
    }

    private boolean resetKitCooldown(CommandSender sender, String label, List<String> args) {
        Optional<KitCooldownResetCommandParser.Request> parsed = KitCooldownResetCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "kits.cooldown-reset.usage", Map.of("label", label));
            return true;
        }
        KitCooldownResetCommandParser.Request request = parsed.orElseThrow();
        YamlConfiguration yaml = cooldownStore.load();
        int removed = 0;
        if (request.target().all()) {
            for (String playerId : yaml.getKeys(false)) {
                if (yaml.contains(playerId + "." + request.kit())) {
                    yaml.set(playerId + "." + request.kit(), null);
                    removed++;
                }
            }
        } else {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(request.target().name());
            String path = target.getUniqueId() + "." + request.kit();
            if (yaml.contains(path)) {
                yaml.set(path, null);
                removed++;
            }
        }
        cooldownStore.save(yaml);
        context.message(sender, "kits.cooldown-reset.success", Map.of(
                "kit", request.kit(),
                "target", request.target().name(),
                "count", removed
        ));
        return true;
    }

    private Inventory menuInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, context.messages().component("kits.menu-title", Map.of()));
        ConfigurationSection kits = kitStore.load().getConfigurationSection("kits");
        if (kits == null) {
            return inventory;
        }
        for (String kit : kits.getKeys(false)) {
            inventory.addItem(named(Material.CHEST, kit, context.messages().component("kits.menu-item-lore", Map.of("kit", kit))));
        }
        return inventory;
    }

    private List<String> kitNames() {
        ConfigurationSection kits = kitStore.load().getConfigurationSection("kits");
        return kits == null ? List.of() : kits.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private Inventory previewInventory(String kit) {
        Inventory inventory = Bukkit.createInventory(null, 54, context.messages().component("kits.preview-title", Map.of("kit", kit)));
        ConfigurationSection section = kitStore.load().getConfigurationSection("kits." + kit.toLowerCase(Locale.ROOT));
        if (section != null) {
            for (ItemStack item : deserialize(section.getMapList("items"))) {
                if (item != null && item.getType() != Material.AIR) {
                    inventory.addItem(item);
                }
            }
        }
        return inventory;
    }

    private ItemStack named(Material material, String name, Component lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String menuTitle() {
        return context.text().plain(context.messages().component("kits.menu-title", Map.of()));
    }

    private String previewTitlePrefix() {
        return context.text().plain(context.messages().component("kits.preview-title-prefix", Map.of()));
    }

    private List<Map<String, Object>> serialize(ItemStack[] items) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                serialized.add(item.serialize());
            }
        }
        return serialized;
    }

    private List<ItemStack> deserialize(List<Map<?, ?>> maps) {
        List<ItemStack> items = new ArrayList<>();
        for (Map<?, ?> map : maps) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            items.add(ItemStack.deserialize(typed));
        }
        return items;
    }

    private long parseLong(String input, long fallback) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
