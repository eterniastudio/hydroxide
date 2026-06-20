package net.axther.hydroxide.modules.shop;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.api.event.EconomyTransactionEvent;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public final class ShopModule implements HydroModule, Listener {

    private HydroxideContext context;
    private YamlStore shopStore;

    @Override
    public String id() {
        return "shop";
    }

    @Override
    public String displayName() {
        return "Shop";
    }

    @Override
    public String description() {
        return "Vault-backed GUI and sign shops with transaction safety checks.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core", "economy");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.shopStore = new YamlStore(new File(context.plugin().getDataFolder(), "shop.yml"));
        seedDefaultShop();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("shop", shopCommand());
        context.commands().register("sell", sellCommand());
        context.commands().register("worth", worthCommand());
        context.commands().register("setworth", setWorthCommand());
        context.commands().register("generateworth", generateWorthCommand());
        context.commands().register("worthlist", worthListCommand());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    private CommandService shopCommand() {
        return new CommandService(HydroCommand.builder("shop")
                .permission("hydroxide.command.shop")
                .playerOnly(true)
                .usage("/{label}")
                .executor(ctx -> ((Player) ctx.sender()).openInventory(shopInventory()))
                .build(), context.messages());
    }

    private CommandService sellCommand() {
        return new CommandService(HydroCommand.builder("sell")
                .permission("hydroxide.command.sell")
                .playerOnly(true)
                .usage("/{label} [hand|all|material] [amount]")
                .executor(ctx -> sellCommand((Player) ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::sellCompletions)
                .build(), context.messages());
    }

    private CommandService worthCommand() {
        return new CommandService(HydroCommand.builder("worth")
                .permission("hydroxide.command.worth")
                .usage("/{label} [hand|material] [amount]")
                .executor(ctx -> worthCommand(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::worthCompletions)
                .build(), context.messages());
    }

    private CommandService setWorthCommand() {
        return new CommandService(HydroCommand.builder("setworth")
                .permission("hydroxide.command.setworth")
                .usage("/{label} [material] <price>")
                .executor(ctx -> setWorthCommand(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::worthCompletions)
                .build(), context.messages());
    }

    private CommandService generateWorthCommand() {
        return new CommandService(HydroCommand.builder("generateworth")
                .permission("hydroxide.command.generateworth")
                .usage("/{label} [basePrice] [-overwrite]")
                .executor(ctx -> generateWorthCommand(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() == 1
                        ? CommandUtils.matching(ctx.argument(0), List.of("1.00", "-overwrite"))
                        : List.of())
                .build(), context.messages());
    }

    private CommandService worthListCommand() {
        return new CommandService(HydroCommand.builder("worthlist")
                .permission("hydroxide.command.worthlist")
                .usage("/{label} [player] [-missing] [-p:page]")
                .executor(ctx -> worthListCommand(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::worthListCompletions)
                .build(), context.messages());
    }

    private List<String> sellCompletions(CommandContext ctx) {
        if (ctx.arguments().size() != 1) {
            return List.of();
        }
        List<String> values = new ArrayList<>(List.of("all", "hand"));
        values.addAll(sellPrices().keySet().stream()
                .map(material -> material.name().toLowerCase(Locale.ROOT))
                .toList());
        return CommandUtils.matching(ctx.argument(0), values);
    }

    private List<String> worthCompletions(CommandContext ctx) {
        if (ctx.arguments().size() != 1) {
            return List.of();
        }
        List<String> values = new ArrayList<>(List.of("hand"));
        values.addAll(sellPrices().keySet().stream()
                .map(material -> material.name().toLowerCase(Locale.ROOT))
                .toList());
        return CommandUtils.matching(ctx.argument(0), values);
    }

    private List<String> worthListCompletions(CommandContext ctx) {
        if (ctx.arguments().size() != 1) {
            return List.of();
        }
        List<String> values = new ArrayList<>(List.of("-missing", "-p:1"));
        if (ctx.sender().hasPermission("hydroxide.command.worthlist.others")) {
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return CommandUtils.matching(ctx.argument(0), values);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!context.text().plain(event.getView().title()).equals(shopTitle())) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        ShopItem shopItem = itemFromDisplay(context.text().plain(item.getItemMeta().displayName()));
        if (shopItem == null) {
            return;
        }
        if (event.isRightClick()) {
            sell(player, shopItem);
        } else {
            buy(player, shopItem);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!event.getPlayer().hasPermission("hydroxide.command.shop.admin")) {
            return;
        }
        String header = context.text().plain(event.line(0));
        if (!header.equalsIgnoreCase("[Buy]") && !header.equalsIgnoreCase("[Sell]")) {
            return;
        }
        Material material = Material.matchMaterial(context.text().plain(event.line(1)));
        double price = parseDouble(context.text().plain(event.line(3)), -1.0);
        if (material == null || !ShopPricing.validMoney(price)) {
            context.message(event.getPlayer(), "shop.sign.invalid", Map.of());
            return;
        }
        event.line(0, context.messages().component(header.equalsIgnoreCase("[Buy]") ? "shop.sign.buy-header" : "shop.sign.sell-header", Map.of()));
        event.line(1, net.kyori.adventure.text.Component.text(material.name()));
        event.line(3, net.kyori.adventure.text.Component.text(String.format(Locale.US, "%.2f", price)));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!(event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof Sign sign)) {
            return;
        }
        String header = context.text().plain(sign.getSide(Side.FRONT).line(0));
        if (!header.equalsIgnoreCase("[Buy]") && !header.equalsIgnoreCase("[Sell]")) {
            return;
        }
        Material material = Material.matchMaterial(context.text().plain(sign.getSide(Side.FRONT).line(1)));
        int amount = Math.max(1, (int) parseDouble(context.text().plain(sign.getSide(Side.FRONT).line(2)), 1.0));
        double price = parseDouble(context.text().plain(sign.getSide(Side.FRONT).line(3)), -1.0);
        if (material == null || !ShopPricing.validMoney(price)) {
            return;
        }
        event.setCancelled(true);
        ShopItem item = new ShopItem(material, amount, price, Math.max(0.0, price * 0.75D));
        if (header.equalsIgnoreCase("[Buy]")) {
            buy(event.getPlayer(), item);
        } else {
            sell(event.getPlayer(), item);
        }
    }

    private Inventory shopInventory() {
        Inventory inventory = Bukkit.createInventory(null, 54, context.messages().component("shop.title", Map.of()));
        ConfigurationSection items = shopStore.load().getConfigurationSection("items");
        if (items == null) {
            return inventory;
        }
        for (String key : items.getKeys(false)) {
            ConfigurationSection section = items.getConfigurationSection(key);
            Material material = Material.matchMaterial(section.getString("material", "STONE"));
            if (material == null) {
                continue;
            }
            double buy = section.getDouble("buy", 0.0);
            double sell = section.getDouble("sell", 0.0);
            inventory.addItem(display(new ShopItem(material, section.getInt("amount", 1), buy, sell)));
        }
        return inventory;
    }

    private void buy(Player player, ShopItem item) {
        if (player.getInventory().firstEmpty() == -1) {
            context.message(player, "shop.inventory-full", Map.of());
            return;
        }
        double total = ShopPricing.total(item.buyPrice(), item.amount());
        EconomyTransactionEvent event = new EconomyTransactionEvent(player, player, null,
                EconomyTransactionEvent.TransactionType.SHOP_BUY, total, "shop-buy:" + item.material().name());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            context.message(player, "shop.buy.cancelled", Map.of());
            return;
        }
        if (event.amount() <= 0.0) {
            context.message(player, "shop.invalid-price", Map.of());
            return;
        }
        EconomyResponse response = context.services().economy()
                .map(economy -> economy.withdrawPlayer(player, event.amount()))
                .orElse(null);
        if (response == null || !response.transactionSuccess()) {
            context.message(player, "shop.cannot-afford", Map.of());
            return;
        }
        player.getInventory().addItem(new ItemStack(item.material(), item.amount()));
        context.message(player, "shop.buy.success", Map.of("amount", item.amount(), "material", item.material().name()));
    }

    private void sell(Player player, ShopItem item) {
        ItemStack stack = new ItemStack(item.material(), item.amount());
        if (!player.getInventory().containsAtLeast(stack, item.amount())) {
            context.message(player, "shop.not-enough-items", Map.of());
            return;
        }
        player.getInventory().removeItem(stack);
        double total = ShopPricing.total(item.sellPrice(), item.amount());
        EconomyTransactionEvent event = new EconomyTransactionEvent(player, null, player,
                EconomyTransactionEvent.TransactionType.SHOP_SELL, total, "shop-sell:" + item.material().name());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            player.getInventory().addItem(stack);
            context.message(player, "shop.sell.cancelled", Map.of());
            return;
        }
        if (event.amount() <= 0.0) {
            player.getInventory().addItem(stack);
            context.message(player, "shop.invalid-price", Map.of());
            return;
        }
        context.services().economy().ifPresent(economy -> economy.depositPlayer(player, event.amount()));
        context.message(player, "shop.sell.success", Map.of("amount", item.amount(), "material", item.material().name()));
    }

    private boolean sellCommand(Player player, String label, List<String> args) {
        SellCommandRequest request = SellCommandParser.parse(label, args).orElse(null);
        if (request == null) {
            context.message(player, "shop.sell-command.usage", Map.of("label", label));
            return true;
        }

        Map<Material, Double> prices = sellPrices();
        Sale sale = switch (request.mode()) {
            case HAND -> saleFromHand(player, prices, request.amount());
            case MATERIAL -> saleFromMaterial(player, prices, request.material().orElse(""), request.amount());
            case ALL -> saleFromInventory(player, prices);
        };
        if (sale.status() != SaleStatus.READY) {
            context.message(player, sale.status().messageKey(), sale.placeholders(label));
            return true;
        }

        EconomyTransactionEvent event = new EconomyTransactionEvent(player, null, player,
                EconomyTransactionEvent.TransactionType.SHOP_SELL, sale.total(), "shop-command:" + sale.materialLabel());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            context.message(player, "shop.sell.cancelled", Map.of());
            return true;
        }
        if (event.amount() <= 0.0) {
            context.message(player, "shop.invalid-price", Map.of());
            return true;
        }

        EconomyResponse response = context.services().economy()
                .map(economy -> economy.depositPlayer(player, event.amount()))
                .orElse(null);
        if (response == null || !response.transactionSuccess()) {
            context.message(player, "shop.invalid-price", Map.of());
            return true;
        }

        removeMaterials(player.getInventory(), sale.items());
        context.message(player, "shop.sell-command.success", Map.of(
                "amount", sale.quantity(),
                "material", sale.materialLabel(),
                "price", context.services().economy()
                        .map(economy -> economy.format(event.amount()))
                        .orElse(String.format(Locale.US, "%.2f", event.amount()))
        ));
        return true;
    }

    private boolean worthCommand(CommandSender sender, String label, List<String> args) {
        WorthCommandRequest request = WorthCommandParser.parseWorth(args).orElse(null);
        if (request == null) {
            context.message(sender, "shop.worth.usage", Map.of("label", label));
            return true;
        }

        WorthTarget target = worthTarget(sender, request, false);
        if (target.status() != WorthStatus.READY) {
            context.message(sender, target.status().messageKey(), target.placeholders(label));
            return true;
        }

        Double unitPrice = sellPrices().get(target.material());
        if (unitPrice == null) {
            context.message(sender, "shop.worth.no-price", Map.of("material", target.material().name()));
            return true;
        }

        int amount = request.amount().orElse(target.amount());
        double total = ShopPricing.total(unitPrice, amount);
        context.message(sender, "shop.worth.result", Map.of(
                "amount", amount,
                "material", target.material().name(),
                "unit_price", money(unitPrice),
                "price", money(total)
        ));
        return true;
    }

    private boolean setWorthCommand(CommandSender sender, String label, List<String> args) {
        SetWorthCommandRequest request = WorthCommandParser.parseSetWorth(args).orElse(null);
        if (request == null) {
            context.message(sender, invalidSetWorthPrice(args) ? "shop.setworth.invalid-price" : "shop.setworth.usage",
                    Map.of("label", label));
            return true;
        }

        WorthTarget target = worthTarget(sender, request, true);
        if (target.status() != WorthStatus.READY) {
            context.message(sender, target.status().messageKey(), target.placeholders(label));
            return true;
        }

        setWorthPrice(target.material(), request.price());
        context.message(sender, "shop.setworth.success", Map.of(
                "material", target.material().name(),
                "price", money(request.price())
        ));
        return true;
    }

    private boolean generateWorthCommand(CommandSender sender, String label, List<String> args) {
        GenerateWorthCommandParser.Request request = GenerateWorthCommandParser.parse(args).orElse(null);
        if (request == null) {
            context.message(sender, "shop.generateworth.usage", Map.of("label", label));
            return true;
        }

        YamlConfiguration yaml = shopStore.load();
        ConfigurationSection worth = yaml.getConfigurationSection("worth");
        Set<String> existing = worth == null ? Set.of() : worth.getKeys(false);
        WorthGenerationPlanner.Plan plan = WorthGenerationPlanner.plan(
                Arrays.asList(Material.values()),
                existing,
                request.basePrice(),
                request.overwrite()
        );
        if (plan.prices().isEmpty()) {
            context.message(sender, "shop.generateworth.nothing", Map.of("skipped", plan.skippedExisting()));
            return true;
        }

        try {
            File backup = backupShopFile();
            plan.prices().forEach((material, price) -> yaml.set("worth." + material.name(), price));
            shopStore.save(yaml);
            context.message(sender, "shop.generateworth.success", Map.of(
                    "created", plan.created(),
                    "overwritten", plan.overwritten(),
                    "skipped", plan.skippedExisting(),
                    "backup", backup.getName()
            ));
        } catch (IOException | RuntimeException exception) {
            context.message(sender, "shop.generateworth.failed", Map.of("reason", exception.getMessage()));
        }
        return true;
    }

    private boolean worthListCommand(CommandSender sender, String label, List<String> args) {
        WorthListCommandParser.Request request = WorthListCommandParser.parse(args).orElse(null);
        if (request == null) {
            context.message(sender, "shop.worthlist.usage", Map.of("label", label));
            return true;
        }
        CommandSender recipient = sender;
        if (request.target().isPresent()) {
            Player target = CommandUtils.onlinePlayer(request.target().orElseThrow()).orElse(null);
            if (target == null) {
                context.message(sender, "shop.worthlist.target-offline", Map.of("target", request.target().orElseThrow()));
                return true;
            }
            if (!sender.equals(target) && !context.requirePermission(sender, "hydroxide.command.worthlist.others")) {
                return true;
            }
            recipient = target;
        }

        List<WorthListEntry> entries = request.missing() ? missingWorthEntries() : configuredWorthEntries();
        String modeKey = request.missing() ? "shop.worthlist.mode.missing" : "shop.worthlist.mode.configured";
        sendWorthList(recipient, label, request.page(), request.missing(), entries, context.messages().template(modeKey, request.missing() ? "missing" : "configured"));
        if (!recipient.equals(sender)) {
            context.message(sender, "shop.worthlist.sent", Map.of(
                    "target", recipient.getName(),
                    "mode", context.messages().template(modeKey, request.missing() ? "missing" : "configured")
            ));
        }
        return true;
    }

    private void sendWorthList(CommandSender recipient, String label, int requestedPage, boolean missing, List<WorthListEntry> entries, String mode) {
        if (entries.isEmpty()) {
            context.message(recipient, missing ? "shop.worthlist.no-missing" : "shop.worthlist.empty", Map.of("label", label));
            return;
        }
        int pageSize = 10;
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) pageSize));
        int page = Math.min(Math.max(1, requestedPage), pages);
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, entries.size());
        context.message(recipient, "shop.worthlist.header", Map.of(
                "mode", mode,
                "page", page,
                "pages", pages,
                "count", entries.size()
        ));
        for (WorthListEntry entry : entries.subList(start, end)) {
            if (missing) {
                context.message(recipient, "shop.worthlist.missing-entry", Map.of("material", entry.material().name()));
            } else {
                context.message(recipient, "shop.worthlist.entry", Map.of(
                        "material", entry.material().name(),
                        "price", money(entry.price().orElse(0.0D))
                ));
            }
        }
    }

    private Sale saleFromHand(Player player, Map<Material, Double> prices, OptionalInt requestedAmount) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            return Sale.holdRequired();
        }
        Double price = prices.get(held.getType());
        if (price == null) {
            return Sale.noPrice(held.getType().name());
        }
        int quantity = Math.min(requestedAmount.orElse(held.getAmount()), held.getAmount());
        return quantity <= 0 ? Sale.nothing() : Sale.ready(Map.of(held.getType(), quantity), total(Map.of(held.getType(), quantity), prices), held.getType().name());
    }

    private Sale saleFromMaterial(Player player, Map<Material, Double> prices, String materialName, OptionalInt requestedAmount) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            return Sale.unknownMaterial(materialName);
        }
        Double price = prices.get(material);
        if (price == null) {
            return Sale.noPrice(material.name());
        }
        int available = countMaterial(player.getInventory(), material);
        int quantity = Math.min(requestedAmount.orElse(available), available);
        return quantity <= 0 ? Sale.nothing() : Sale.ready(Map.of(material, quantity), total(Map.of(material, quantity), prices), material.name());
    }

    private Sale saleFromInventory(Player player, Map<Material, Double> prices) {
        Map<Material, Integer> items = new HashMap<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir() || !prices.containsKey(item.getType())) {
                continue;
            }
            items.merge(item.getType(), item.getAmount(), Integer::sum);
        }
        return items.isEmpty() ? Sale.nothing() : Sale.ready(items, total(items, prices), "items");
    }

    private int countMaterial(PlayerInventory inventory, Material material) {
        int count = 0;
        for (ItemStack item : inventory.getStorageContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeMaterials(PlayerInventory inventory, Map<Material, Integer> materials) {
        for (Map.Entry<Material, Integer> entry : materials.entrySet()) {
            removeMaterial(inventory, entry.getKey(), entry.getValue());
        }
    }

    private void removeMaterial(PlayerInventory inventory, Material material, int quantity) {
        int remaining = quantity;
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) {
                continue;
            }
            int removed = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - removed);
            if (item.getAmount() <= 0) {
                contents[i] = null;
            }
            remaining -= removed;
        }
        inventory.setStorageContents(contents);
    }

    private double total(Map<Material, Integer> quantities, Map<Material, Double> prices) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Material, Integer> entry : quantities.entrySet()) {
            Double price = prices.get(entry.getKey());
            if (price == null || !ShopPricing.validMoney(price) || entry.getValue() < 1) {
                throw new IllegalArgumentException("Invalid shop sale");
            }
            total = total.add(BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(entry.getValue())));
        }
        return total.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Map<Material, Double> sellPrices() {
        Map<Material, Double> prices = new HashMap<>();
        YamlConfiguration yaml = shopStore.load();
        ConfigurationSection worth = yaml.getConfigurationSection("worth");
        if (worth != null) {
            for (String key : worth.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                double sell = worth.getDouble(key, -1.0D);
                if (material != null && sell > 0.0D && ShopPricing.validMoney(sell)) {
                    prices.put(material, sell);
                }
            }
        }
        ConfigurationSection items = yaml.getConfigurationSection("items");
        if (items == null) {
            return prices;
        }
        for (String key : items.getKeys(false)) {
            ConfigurationSection section = items.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Material material = Material.matchMaterial(section.getString("material", ""));
            double sell = section.getDouble("sell", -1.0D);
            if (material != null && sell > 0.0D && ShopPricing.validMoney(sell)) {
                prices.put(material, sell);
            }
        }
        return prices;
    }

    private List<WorthListEntry> configuredWorthEntries() {
        return sellPrices().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Material::name)))
                .map(entry -> new WorthListEntry(entry.getKey(), Optional.of(entry.getValue())))
                .toList();
    }

    private List<WorthListEntry> missingWorthEntries() {
        Set<Material> configured = sellPrices().keySet();
        return Arrays.stream(Material.values())
                .filter(WorthGenerationPlanner::candidate)
                .filter(material -> !configured.contains(material))
                .sorted(java.util.Comparator.comparing(Material::name))
                .map(material -> new WorthListEntry(material, Optional.empty()))
                .toList();
    }

    private void setWorthPrice(Material material, double price) {
        YamlConfiguration yaml = shopStore.load();
        yaml.set("worth." + material.name(), price);
        shopStore.save(yaml);
    }

    private File backupShopFile() throws IOException {
        File source = shopStore.file();
        File parent = source.getAbsoluteFile().getParentFile();
        File backup = new File(parent == null ? new File(".") : parent, source.getName() + ".bak");
        if (source.exists()) {
            Files.copy(source.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return backup;
    }

    private WorthTarget worthTarget(CommandSender sender, WorthCommandRequest request, boolean setting) {
        if (request.source() == WorthCommandRequest.Source.HAND) {
            if (!(sender instanceof Player player)) {
                return setting ? WorthTarget.setWorthHoldRequired() : WorthTarget.consoleMaterialRequired();
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().isAir()) {
                return setting ? WorthTarget.setWorthHoldRequired() : WorthTarget.worthHoldRequired();
            }
            return WorthTarget.ready(item.getType(), item.getAmount());
        }
        Material material = Material.matchMaterial(request.material().orElse(""));
        return material == null
                ? WorthTarget.unknownMaterial(request.material().orElse(""), setting)
                : WorthTarget.ready(material, 1);
    }

    private WorthTarget worthTarget(CommandSender sender, SetWorthCommandRequest request, boolean setting) {
        return worthTarget(sender, new WorthCommandRequest(request.source(), request.material(), OptionalInt.empty()), setting);
    }

    private boolean invalidSetWorthPrice(List<String> args) {
        if (args.size() == 1) {
            return true;
        }
        if (args.size() == 2) {
            try {
                double parsed = Double.parseDouble(args.get(1));
                return !ShopPricing.validMoney(parsed);
            } catch (NumberFormatException exception) {
                return true;
            }
        }
        return false;
    }

    private String money(double amount) {
        return context.services().economy()
                .map(economy -> economy.format(amount))
                .orElse(String.format(Locale.US, "%.2f", amount));
    }

    private ShopItem itemFromDisplay(String materialName) {
        ConfigurationSection items = shopStore.load().getConfigurationSection("items");
        if (items == null) {
            return null;
        }
        for (String key : items.getKeys(false)) {
            ConfigurationSection section = items.getConfigurationSection(key);
            Material material = Material.matchMaterial(section.getString("material", ""));
            if (material != null && material.name().equalsIgnoreCase(materialName)) {
                return new ShopItem(material, section.getInt("amount", 1), section.getDouble("buy"), section.getDouble("sell"));
            }
        }
        return null;
    }

    private ItemStack display(ShopItem item) {
        ItemStack stack = new ItemStack(item.material());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(item.material().name()));
        meta.lore(List.of(
                context.messages().component("shop.lore.buy", Map.of("price", item.buyPrice())),
                context.messages().component("shop.lore.sell", Map.of("price", item.sellPrice()))
        ));
        stack.setItemMeta(meta);
        return stack;
    }

    private String shopTitle() {
        return context.text().plain(context.messages().component("shop.title", Map.of()));
    }

    private void seedDefaultShop() {
        YamlConfiguration yaml = shopStore.load();
        if (yaml.contains("items")) {
            return;
        }
        yaml.set("items.stone.material", "STONE");
        yaml.set("items.stone.amount", 16);
        yaml.set("items.stone.buy", 10.00);
        yaml.set("items.stone.sell", 2.00);
        shopStore.save(yaml);
    }

    private double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private record ShopItem(Material material, int amount, double buyPrice, double sellPrice) {
    }

    private record WorthListEntry(Material material, Optional<Double> price) {
    }

    private enum WorthStatus {
        READY(""),
        WORTH_CONSOLE_MATERIAL_REQUIRED("shop.worth.console-material-required"),
        WORTH_HOLD_REQUIRED("shop.worth.hold-required"),
        WORTH_UNKNOWN_MATERIAL("shop.worth.unknown-material"),
        SETWORTH_HOLD_REQUIRED("shop.setworth.hold-required"),
        SETWORTH_UNKNOWN_MATERIAL("shop.setworth.unknown-material");

        private final String messageKey;

        WorthStatus(String messageKey) {
            this.messageKey = messageKey;
        }

        String messageKey() {
            return messageKey;
        }
    }

    private record WorthTarget(WorthStatus status, Material material, int amount, String materialLabel) {

        static WorthTarget ready(Material material, int amount) {
            return new WorthTarget(WorthStatus.READY, material, Math.max(1, amount), material.name());
        }

        static WorthTarget consoleMaterialRequired() {
            return new WorthTarget(WorthStatus.WORTH_CONSOLE_MATERIAL_REQUIRED, null, 1, "");
        }

        static WorthTarget worthHoldRequired() {
            return new WorthTarget(WorthStatus.WORTH_HOLD_REQUIRED, null, 1, "");
        }

        static WorthTarget setWorthHoldRequired() {
            return new WorthTarget(WorthStatus.SETWORTH_HOLD_REQUIRED, null, 1, "");
        }

        static WorthTarget unknownMaterial(String material, boolean setting) {
            return new WorthTarget(setting ? WorthStatus.SETWORTH_UNKNOWN_MATERIAL : WorthStatus.WORTH_UNKNOWN_MATERIAL,
                    null, 1, material);
        }

        Map<String, ?> placeholders(String label) {
            return Map.of("label", label, "material", materialLabel);
        }
    }

    private enum SaleStatus {
        READY(""),
        HOLD_REQUIRED("shop.sell-command.hold-required"),
        UNKNOWN_MATERIAL("shop.sell-command.unknown-material"),
        NO_PRICE("shop.sell-command.no-price"),
        NOTHING("shop.sell-command.nothing");

        private final String messageKey;

        SaleStatus(String messageKey) {
            this.messageKey = messageKey;
        }

        String messageKey() {
            return messageKey;
        }
    }

    private record Sale(SaleStatus status, Map<Material, Integer> items, double total, String materialLabel) {

        static Sale ready(Map<Material, Integer> items, double total, String materialLabel) {
            return new Sale(SaleStatus.READY, Map.copyOf(items), total, materialLabel);
        }

        static Sale holdRequired() {
            return new Sale(SaleStatus.HOLD_REQUIRED, Map.of(), 0.0D, "");
        }

        static Sale unknownMaterial(String input) {
            return new Sale(SaleStatus.UNKNOWN_MATERIAL, Map.of(), 0.0D, input);
        }

        static Sale noPrice(String material) {
            return new Sale(SaleStatus.NO_PRICE, Map.of(), 0.0D, material);
        }

        static Sale nothing() {
            return new Sale(SaleStatus.NOTHING, Map.of(), 0.0D, "");
        }

        int quantity() {
            return items.values().stream().mapToInt(Integer::intValue).sum();
        }

        Map<String, ?> placeholders(String label) {
            return Map.of("label", label, "material", materialLabel);
        }
    }
}
