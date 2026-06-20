package net.axther.hydroxide.modules.economy;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.api.event.EconomyTransactionEvent;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.modules.options.PlayerOption;
import net.axther.hydroxide.storage.YamlStore;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EconomyModule implements HydroModule, Listener {

    private HydroEconomy economy;
    private HydroxideContext context;
    private ChequeTokenSigner chequeSigner;
    private ChequeRedemptionStore chequeRedemptions;
    private boolean registeredWithVault;

    @Override
    public String id() {
        return "economy";
    }

    @Override
    public String displayName() {
        return "Economy";
    }

    @Override
    public String description() {
        return "Vault-compatible economy provider and player money commands.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        double startingBalance = context.plugin().getConfig().getDouble("economy.starting-balance", 100.0);
        String symbol = context.plugin().getConfig().getString("economy.currency-symbol", "$");
        String currencyName = context.plugin().getConfig().getString("economy.currency-name", "dollars");
        economy = new HydroEconomy(new PlayerDataAccounts(context), startingBalance, symbol, currencyName);
        chequeSigner = new ChequeTokenSigner(chequeSigningSecret(context));
        chequeRedemptions = new ChequeRedemptionStore(new YamlStore(new File(context.plugin().getDataFolder(), "cheques.yml")));
        context.services().economy(economy);
        Bukkit.getPluginManager().registerEvents(this, context.plugin());

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            Bukkit.getServicesManager().register(Economy.class, economy, context.plugin(), ServicePriority.Highest);
            registeredWithVault = true;
            context.plugin().getLogger().info("Registered Hydroxide economy with Vault.");
        } else {
            context.plugin().getLogger().info("Vault not found; economy commands are enabled, Vault provider registration skipped.");
        }

        context.commands().register("balance", command("balance", "hydroxide.command.balance", "/{label} [player]",
                ctx -> balance(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                ctx -> CommandUtils.matching(ctx.argument(0), Bukkit.getOnlinePlayers().stream().map(Player::getName).toList())));
        context.commands().register("pay", command("pay", "hydroxide.command.pay", "/{label} <player> <amount>",
                ctx -> pay(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                ctx -> ctx.arguments().size() <= 1
                        ? CommandUtils.matching(ctx.argument(0), Bukkit.getOnlinePlayers().stream().map(Player::getName).toList())
                        : CommandUtils.matching(ctx.argument(1), List.of("1", "10", "100", "1000"))));
        context.commands().register("paytoggle", command("paytoggle", "hydroxide.command.paytoggle", "/{label} [on|off]",
                ctx -> payToggle(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                ctx -> ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), List.of("off", "on")) : List.of()));
        context.commands().register("cheque", command("cheque", "hydroxide.command.cheque", "/{label} [player] <amount>",
                ctx -> cheque(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                ctx -> chequeCompletions(ctx.arguments())));
        context.commands().register("eco", command("eco", "hydroxide.command.eco", "/{label} <give|take|set> <player> <amount>",
                ctx -> eco(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                ctx -> economyCompletions(ctx.arguments())));
        context.commands().register("baltop", command("baltop", "hydroxide.command.baltop", "/{label} [page]",
                ctx -> baltop(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                ctx -> CommandUtils.matching(ctx.argument(0), List.of("1", "2", "3", "4", "5"))));
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        if (registeredWithVault && economy != null) {
            Bukkit.getServicesManager().unregister(Economy.class, economy);
        }
        if (economy != null) {
            context.services().clearEconomy(economy);
        }
    }

    private CommandService command(String name, String permission, String usage, HydroCommand.HydroCommandExecutor executor, HydroCommand.HydroTabCompleter completer) {
        return new CommandService(HydroCommand.builder(name)
                .permission(permission)
                .usage(usage)
                .executor(executor)
                .completer(completer)
                .build(), context.messages());
    }

    private boolean balance(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.balance")) {
            return true;
        }

        OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                context.message(sender, "validation.usage", Map.of("usage", "/" + label + " <player>"));
                return true;
            }
            target = player;
        } else {
            if (!sender.hasPermission("hydroxide.command.balance.others")) {
                context.message(sender, "economy.balance.others-denied", Map.of());
                return true;
            }
            target = findKnownPlayer(args[0]).orElse(null);
            if (target == null) {
                context.message(sender, "validation.unknown-player", Map.of("target", args[0]));
                return true;
            }
        }

        String key = sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())
                ? "economy.balance.self"
                : "economy.balance.other";
        context.message(sender, key, Map.of(
                "player", playerName(target),
                "balance", economy.format(economy.getBalance(target))
        ));
        return true;
    }

    private boolean pay(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.pay")) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            context.message(sender, "validation.player-only", Map.of("usage", "/" + label));
            return true;
        }
        if (args.length < 2) {
            context.message(sender, "economy.pay.usage", Map.of("label", label));
            return true;
        }

        OfflinePlayer target = findKnownPlayer(args[0]).orElse(null);
        if (target == null) {
            context.message(sender, "validation.unknown-player", Map.of("target", args[0]));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            context.message(sender, "economy.pay.self-denied", Map.of());
            return true;
        }
        if (!paymentsEnabled(target)) {
            context.message(sender, "economy.pay.target-disabled", Map.of("target", playerName(target)));
            return true;
        }

        Optional<Double> amount = parsePositiveAmount(args[1]);
        if (amount.isEmpty()) {
            context.message(sender, "validation.invalid-amount", Map.of("amount", args[1]));
            return true;
        }

        EconomyTransactionEvent event = new EconomyTransactionEvent(player, player, target,
                EconomyTransactionEvent.TransactionType.PAY, amount.get(), "player-pay");
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            context.message(sender, "economy.pay.cancelled", Map.of());
            return true;
        }
        if (event.amount() <= 0.0) {
            context.message(sender, "validation.invalid-amount", Map.of("amount", event.amount()));
            return true;
        }

        EconomyResponse withdrawal = economy.withdrawPlayer(player, event.amount());
        if (!withdrawal.transactionSuccess()) {
            context.message(sender, "validation.invalid-amount", Map.of("amount", args[1], "reason", withdrawal.errorMessage));
            return true;
        }
        EconomyResponse deposit = economy.depositPlayer(target, event.amount());
        if (!deposit.transactionSuccess()) {
            economy.depositPlayer(player, event.amount());
            context.message(sender, "economy.pay.failed-returned", Map.of());
            return true;
        }

        context.message(sender, "economy.pay.sent", Map.of(
                "target", playerName(target),
                "amount", economy.format(event.amount())
        ));
        if (target instanceof Player onlineTarget) {
            context.message(onlineTarget, "economy.pay.received", Map.of(
                    "amount", economy.format(event.amount()),
                    "player", player.getName()
            ));
        }
        return true;
    }

    private boolean payToggle(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.paytoggle")) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            context.message(sender, "validation.player-only", Map.of("usage", "/" + label));
            return true;
        }
        var service = context.services().playerOptionsService().orElse(null);
        if (service == null) {
            context.message(sender, "economy.paytoggle.unavailable", Map.of());
            return true;
        }
        Optional<Boolean> requested = PaymentToggleParser.parse(List.of(args), service.enabled(player.getUniqueId(), PlayerOption.PAYMENTS));
        if (requested.isEmpty()) {
            context.message(sender, "economy.paytoggle.usage", Map.of("label", label));
            return true;
        }
        service.set(player.getUniqueId(), PlayerOption.PAYMENTS, requested.get());
        context.message(sender, requested.get() ? "economy.paytoggle.enabled" : "economy.paytoggle.disabled", Map.of());
        return true;
    }

    private boolean cheque(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.cheque")) {
            return true;
        }
        if (args.length < 1 || args.length > 2) {
            context.message(sender, "economy.cheque.usage", Map.of("label", label));
            return true;
        }

        Player target;
        String amountInput;
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                context.message(sender, "economy.cheque.usage", Map.of("label", label));
                return true;
            }
            target = player;
            amountInput = args[0];
        } else {
            if (!sender.hasPermission("hydroxide.command.cheque.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.cheque.others"));
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                context.message(sender, "economy.cheque.player-offline", Map.of("target", args[0]));
                return true;
            }
            amountInput = args[1];
        }

        Optional<Double> parsedAmount = ChequeAmountParser.parse(amountInput);
        if (parsedAmount.isEmpty()) {
            context.message(sender, "validation.invalid-amount", Map.of("amount", amountInput));
            return true;
        }

        EconomyTransactionEvent event = new EconomyTransactionEvent(
                sender instanceof Player player ? player : null,
                target,
                null,
                EconomyTransactionEvent.TransactionType.CHEQUE_CREATE,
                parsedAmount.get(),
                "cheque-create"
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            context.message(sender, "economy.cheque.cancelled", Map.of());
            return true;
        }

        EconomyResponse withdrawal = economy.withdrawPlayer(target, event.amount());
        if (!withdrawal.transactionSuccess()) {
            context.message(sender, "economy.cheque.insufficient", Map.of(
                    "target", target.getName(),
                    "amount", economy.format(event.amount())
            ));
            return true;
        }

        ItemStack cheque = chequeItem(UUID.randomUUID(), event.amount());
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(cheque);
        if (!leftover.isEmpty()) {
            economy.depositPlayer(target, event.amount());
            context.message(sender, "economy.cheque.inventory-full", Map.of("target", target.getName()));
            return true;
        }

        context.message(sender, "economy.cheque.created", Map.of(
                "target", target.getName(),
                "amount", economy.format(event.amount())
        ));
        if (!sender.equals(target)) {
            context.message(target, "economy.cheque.received", Map.of(
                    "player", sender.getName(),
                    "amount", economy.format(event.amount())
            ));
        }
        return true;
    }

    private boolean eco(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.eco")) {
            return true;
        }
        if (args.length < 3) {
            context.message(sender, "economy.eco.usage", Map.of("label", label));
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (!List.of("give", "take", "set").contains(action)) {
            context.message(sender, "economy.eco.usage", Map.of("label", label));
            return true;
        }
        OfflinePlayer target = findKnownPlayer(args[1]).orElse(null);
        if (target == null) {
            context.message(sender, "validation.unknown-player", Map.of("target", args[1]));
            return true;
        }

        Optional<Double> amount = action.equals("set") ? parseNonNegativeAmount(args[2]) : parsePositiveAmount(args[2]);
        if (amount.isEmpty()) {
            context.message(sender, "validation.invalid-amount", Map.of("amount", args[2]));
            return true;
        }

        EconomyTransactionEvent event = new EconomyTransactionEvent(
                sender instanceof Player player ? player : null,
                action.equals("take") ? target : null,
                action.equals("take") ? null : target,
                transactionType(action),
                amount.get(),
                "admin-" + action
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            context.message(sender, "economy.eco.cancelled", Map.of());
            return true;
        }

        EconomyResponse response = switch (action) {
            case "give" -> economy.depositPlayer(target, event.amount());
            case "take" -> economy.withdrawPlayer(target, event.amount());
            case "set" -> {
                economy.accountStore().setBalance(target.getUniqueId(), event.amount());
                yield new EconomyResponse(event.amount(), event.amount(), EconomyResponse.ResponseType.SUCCESS, null);
            }
            default -> null;
        };

        if (response == null) {
            context.message(sender, "economy.eco.usage", Map.of("label", label));
            return true;
        }
        if (!response.transactionSuccess()) {
            context.message(sender, "validation.invalid-amount", Map.of("amount", args[2], "reason", response.errorMessage));
            return true;
        }

        context.message(sender, "economy.eco.updated", Map.of(
                "player", playerName(target),
                "balance", economy.format(response.balance)
        ));
        return true;
    }

    private boolean baltop(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.baltop")) {
            return true;
        }
        int page = args.length > 0 ? parsePage(args[0]) : 1;
        int pageSize = 10;
        List<Map.Entry<java.util.UUID, Double>> entries = context.playerData().balances().entrySet().stream()
                .sorted(Map.Entry.<java.util.UUID, Double>comparingByValue(Comparator.reverseOrder()))
                .toList();
        if (entries.isEmpty()) {
            context.message(sender, "economy.baltop.empty", Map.of());
            return true;
        }
        int start = Math.max(0, (page - 1) * pageSize);
        if (start >= entries.size()) {
            start = 0;
            page = 1;
        }
        int end = Math.min(entries.size(), start + pageSize);
        context.message(sender, "economy.baltop.header", Map.of("page", page));
        for (int index = start; index < end; index++) {
            Map.Entry<java.util.UUID, Double> entry = entries.get(index);
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            context.message(sender, "economy.baltop.entry", Map.of(
                    "rank", index + 1,
                    "player", playerName(player),
                    "balance", economy.format(entry.getValue())
            ));
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChequeRedeem(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!hasChequeData(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        Optional<ChequeData> data = chequeData(item);
        if (data.isEmpty()) {
            context.message(player, "economy.cheque.invalid", Map.of());
            return;
        }
        ChequeData cheque = data.orElseThrow();
        if (chequeRedemptions.redeemed(cheque.id())) {
            context.message(player, "economy.cheque.already-redeemed", Map.of("amount", economy.format(cheque.amount())));
            return;
        }

        EconomyTransactionEvent transaction = new EconomyTransactionEvent(
                player,
                null,
                player,
                EconomyTransactionEvent.TransactionType.CHEQUE_REDEEM,
                cheque.amount(),
                "cheque-redeem"
        );
        Bukkit.getPluginManager().callEvent(transaction);
        if (transaction.isCancelled()) {
            context.message(player, "economy.cheque.cancelled", Map.of());
            return;
        }

        EconomyResponse deposit = economy.depositPlayer(player, transaction.amount());
        if (!deposit.transactionSuccess()) {
            context.message(player, "economy.cheque.redeem-failed", Map.of("reason", deposit.errorMessage));
            return;
        }

        chequeRedemptions.markRedeemed(cheque.id());
        consumeOne(event);
        context.message(player, "economy.cheque.redeemed", Map.of("amount", economy.format(transaction.amount())));
    }

    private Optional<OfflinePlayer> findKnownPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(online);
        }
        return Optional.ofNullable(Bukkit.getOfflinePlayerIfCached(name));
    }

    private boolean paymentsEnabled(OfflinePlayer player) {
        return context.services().playerOptionsService()
                .map(service -> service.enabled(player.getUniqueId(), PlayerOption.PAYMENTS))
                .orElse(true);
    }

    private List<String> economyCompletions(List<String> args) {
        if (args.size() <= 1) {
            return CommandUtils.matching(args.isEmpty() ? "" : args.get(0), List.of("give", "take", "set"));
        }
        if (args.size() == 2) {
            return CommandUtils.matching(args.get(1), Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (args.size() == 3) {
            return CommandUtils.matching(args.get(2), List.of("1", "10", "100", "1000"));
        }
        return List.of();
    }

    private List<String> chequeCompletions(List<String> args) {
        if (args.size() <= 1) {
            List<String> values = new java.util.ArrayList<>(List.of("1", "10", "100", "1000"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(args.isEmpty() ? "" : args.get(0), values);
        }
        if (args.size() == 2) {
            return CommandUtils.matching(args.get(1), List.of("1", "10", "100", "1000"));
        }
        return List.of();
    }

    private int parsePage(String input) {
        try {
            return Math.max(1, Integer.parseInt(input));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private Optional<Double> parsePositiveAmount(String input) {
        return parseNonNegativeAmount(input).filter(amount -> amount > 0.0);
    }

    private Optional<Double> parseNonNegativeAmount(String input) {
        try {
            double amount = Double.parseDouble(input);
            return amount >= 0.0 && !Double.isNaN(amount) && !Double.isInfinite(amount)
                    && java.math.BigDecimal.valueOf(amount).scale() <= 2
                    ? Optional.of(amount)
                    : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private EconomyTransactionEvent.TransactionType transactionType(String action) {
        return switch (action) {
            case "give" -> EconomyTransactionEvent.TransactionType.ADMIN_GIVE;
            case "take" -> EconomyTransactionEvent.TransactionType.ADMIN_TAKE;
            case "set" -> EconomyTransactionEvent.TransactionType.ADMIN_SET;
            default -> throw new IllegalArgumentException("Unknown economy action: " + action);
        };
    }

    private String playerName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private ItemStack chequeItem(UUID id, double amount) {
        Map<String, Object> placeholders = Map.of(
                "amount", economy.format(amount),
                "id", id.toString()
        );
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(context.messages().component("economy.cheque.name", placeholders));
        meta.lore(List.of(context.messages().component("economy.cheque.lore", placeholders)));
        meta.getPersistentDataContainer().set(chequeKey("id"), PersistentDataType.STRING, id.toString());
        meta.getPersistentDataContainer().set(chequeKey("amount"), PersistentDataType.STRING, ChequeAmountParser.canonical(amount));
        meta.getPersistentDataContainer().set(chequeKey("signature"), PersistentDataType.STRING, chequeSigner.sign(id, amount));
        item.setItemMeta(meta);
        return item;
    }

    private boolean hasChequeData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(chequeKey("id"), PersistentDataType.STRING)
                || item.getItemMeta().getPersistentDataContainer().has(chequeKey("amount"), PersistentDataType.STRING)
                || item.getItemMeta().getPersistentDataContainer().has(chequeKey("signature"), PersistentDataType.STRING);
    }

    private Optional<ChequeData> chequeData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String idRaw = pdc.get(chequeKey("id"), PersistentDataType.STRING);
        String amountRaw = pdc.get(chequeKey("amount"), PersistentDataType.STRING);
        String signature = pdc.get(chequeKey("signature"), PersistentDataType.STRING);
        if (idRaw == null || amountRaw == null || signature == null) {
            return Optional.empty();
        }
        try {
            UUID id = UUID.fromString(idRaw);
            Optional<Double> amount = ChequeAmountParser.parse(amountRaw);
            if (amount.isEmpty() || !chequeSigner.valid(id, amount.orElseThrow(), signature)) {
                return Optional.empty();
            }
            return Optional.of(new ChequeData(id, amount.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private void consumeOne(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            event.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        } else {
            event.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
    }

    private NamespacedKey chequeKey(String value) {
        return new NamespacedKey(context.plugin(), "cheque_" + value);
    }

    private String chequeSigningSecret(HydroxideContext context) {
        String path = "economy.cheques.signing-secret";
        String secret = context.plugin().getConfig().getString(path, "");
        if (secret != null && !secret.isBlank()) {
            return secret;
        }
        String generated = UUID.randomUUID() + ":" + UUID.randomUUID();
        context.plugin().getConfig().set(path, generated);
        context.plugin().saveConfig();
        return generated;
    }

    private record ChequeData(UUID id, double amount) {
    }

    private record PlayerDataAccounts(HydroxideContext context) implements HydroEconomy.AccountStore {
        @Override
        public double balance(java.util.UUID playerId, double defaultBalance) {
            return context.playerData().balance(playerId, defaultBalance);
        }

        @Override
        public void setBalance(java.util.UUID playerId, double balance) {
            context.playerData().setBalance(playerId, balance);
        }
    }
}
