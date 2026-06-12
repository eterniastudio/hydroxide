package net.axther.hydroxide.modules.shop;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.api.event.EconomyTransactionEvent;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.List;
import java.util.Locale;

public final class ShopModule implements HydroModule, Listener, CommandExecutor {

    private static final String SHOP_TITLE = "Hydroxide Shop";
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
        context.commands().register("shop", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can use the shop.");
            return true;
        }
        player.openInventory(shopInventory());
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!context.text().plain(event.getView().title()).equals(SHOP_TITLE)) {
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
            event.getPlayer().sendMessage(context.text().format("<red>Invalid shop sign."));
            return;
        }
        event.line(0, context.text().format(header.equalsIgnoreCase("[Buy]") ? "<green>[Buy]" : "<gold>[Sell]"));
        event.line(1, Component.text(material.name()));
        event.line(3, Component.text(String.format(Locale.US, "%.2f", price)));
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
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(SHOP_TITLE));
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
            context.send(player, "<red>You need an empty inventory slot first.");
            return;
        }
        double total = ShopPricing.total(item.buyPrice(), item.amount());
        EconomyTransactionEvent event = new EconomyTransactionEvent(player, player, null,
                EconomyTransactionEvent.TransactionType.SHOP_BUY, total, "shop-buy:" + item.material().name());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            context.send(player, "<red>Purchase was cancelled by another plugin.");
            return;
        }
        if (event.amount() <= 0.0) {
            context.send(player, "<red>Invalid shop price.");
            return;
        }
        EconomyResponse response = context.services().economy()
                .map(economy -> economy.withdrawPlayer(player, event.amount()))
                .orElse(null);
        if (response == null || !response.transactionSuccess()) {
            context.send(player, "<red>You cannot afford that purchase.");
            return;
        }
        player.getInventory().addItem(new ItemStack(item.material(), item.amount()));
        context.send(player, "<green>Purchased <white>" + item.amount() + "x " + item.material().name() + "<green>.");
    }

    private void sell(Player player, ShopItem item) {
        ItemStack stack = new ItemStack(item.material(), item.amount());
        if (!player.getInventory().containsAtLeast(stack, item.amount())) {
            context.send(player, "<red>You do not have enough items to sell.");
            return;
        }
        player.getInventory().removeItem(stack);
        double total = ShopPricing.total(item.sellPrice(), item.amount());
        EconomyTransactionEvent event = new EconomyTransactionEvent(player, null, player,
                EconomyTransactionEvent.TransactionType.SHOP_SELL, total, "shop-sell:" + item.material().name());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            player.getInventory().addItem(stack);
            context.send(player, "<red>Sale was cancelled by another plugin.");
            return;
        }
        if (event.amount() <= 0.0) {
            player.getInventory().addItem(stack);
            context.send(player, "<red>Invalid shop price.");
            return;
        }
        context.services().economy().ifPresent(economy -> economy.depositPlayer(player, event.amount()));
        context.send(player, "<green>Sold <white>" + item.amount() + "x " + item.material().name() + "<green>.");
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
        meta.displayName(Component.text(item.material().name()));
        meta.lore(List.of(
                context.text().format("<gray>Left click buy: <white>" + item.buyPrice()),
                context.text().format("<gray>Right click sell: <white>" + item.sellPrice())
        ));
        stack.setItemMeta(meta);
        return stack;
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
}
