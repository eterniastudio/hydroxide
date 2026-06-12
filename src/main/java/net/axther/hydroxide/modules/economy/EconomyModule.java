package net.axther.hydroxide.modules.economy;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.api.event.EconomyTransactionEvent;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class EconomyModule implements HydroModule, CommandExecutor, TabCompleter {

    private HydroEconomy economy;
    private HydroxideContext context;
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
        context.services().economy(economy);

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            Bukkit.getServicesManager().register(Economy.class, economy, context.plugin(), ServicePriority.Highest);
            registeredWithVault = true;
            context.plugin().getLogger().info("Registered Hydroxide economy with Vault.");
        } else {
            context.plugin().getLogger().info("Vault not found; economy commands are enabled, Vault provider registration skipped.");
        }

        context.commands().register("balance", this);
        context.commands().register("pay", this);
        context.commands().register("eco", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        if (registeredWithVault && economy != null) {
            Bukkit.getServicesManager().unregister(Economy.class, economy);
        }
        if (economy != null) {
            context.services().clearEconomy(economy);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "balance" -> balance(sender, label, args);
            case "pay" -> pay(sender, label, args);
            case "eco" -> eco(sender, label, args);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("eco") && args.length == 1) {
            return CommandUtils.matching(args[0], List.of("give", "take", "set"));
        }
        if ((name.equals("balance") && args.length == 1)
                || (name.equals("pay") && args.length == 1)
                || (name.equals("eco") && args.length == 2)) {
            return CommandUtils.matching(args[args.length - 1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if ((name.equals("pay") && args.length == 2)
                || (name.equals("eco") && args.length == 3)) {
            return CommandUtils.matching(args[args.length - 1], List.of("1", "10", "100", "1000"));
        }
        return List.of();
    }

    private boolean balance(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.balance")) {
            return true;
        }

        OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                context.send(sender, "<red>Usage: /" + label + " <player>");
                return true;
            }
            target = player;
        } else {
            if (!sender.hasPermission("hydroxide.command.balance.others")) {
                context.send(sender, "<red>You do not have permission to check another player's balance.");
                return true;
            }
            target = findKnownPlayer(args[0]).orElse(null);
            if (target == null) {
                context.send(sender, "<red>That player is not known.");
                return true;
            }
        }

        context.send(sender, "<green>" + playerName(target) + " has <white>" + economy.format(economy.getBalance(target)) + "<green>.");
        return true;
    }

    private boolean pay(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.pay")) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            context.send(sender, "<red>Only players can use /" + label + ".");
            return true;
        }
        if (args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " <player> <amount>");
            return true;
        }

        OfflinePlayer target = findKnownPlayer(args[0]).orElse(null);
        if (target == null) {
            context.send(sender, "<red>That player is not known.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            context.send(sender, "<red>You cannot pay yourself.");
            return true;
        }

        Optional<Double> amount = parsePositiveAmount(args[1]);
        if (amount.isEmpty()) {
            context.send(sender, "<red>Amount must be a positive number.");
            return true;
        }

        EconomyTransactionEvent event = new EconomyTransactionEvent(player, player, target,
                EconomyTransactionEvent.TransactionType.PAY, amount.get(), "player-pay");
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            context.send(sender, "<red>Payment was cancelled by another plugin.");
            return true;
        }
        if (event.amount() <= 0.0) {
            context.send(sender, "<red>Amount must be a positive number.");
            return true;
        }

        EconomyResponse withdrawal = economy.withdrawPlayer(player, event.amount());
        if (!withdrawal.transactionSuccess()) {
            context.send(sender, "<red>" + withdrawal.errorMessage);
            return true;
        }
        EconomyResponse deposit = economy.depositPlayer(target, event.amount());
        if (!deposit.transactionSuccess()) {
            economy.depositPlayer(player, event.amount());
            context.send(sender, "<red>Payment failed and funds were returned.");
            return true;
        }

        context.send(sender, "<green>Paid <white>" + playerName(target) + " " + economy.format(event.amount()) + "<green>.");
        if (target instanceof Player onlineTarget) {
            context.send(onlineTarget, "<green>You received <white>" + economy.format(event.amount()) + " <green>from <white>" + player.getName() + "<green>.");
        }
        return true;
    }

    private boolean eco(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.eco")) {
            return true;
        }
        if (args.length < 3) {
            context.send(sender, "<red>Usage: /" + label + " <give|take|set> <player> <amount>");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (!List.of("give", "take", "set").contains(action)) {
            context.send(sender, "<red>Usage: /" + label + " <give|take|set> <player> <amount>");
            return true;
        }
        OfflinePlayer target = findKnownPlayer(args[1]).orElse(null);
        if (target == null) {
            context.send(sender, "<red>That player is not known.");
            return true;
        }

        Optional<Double> amount = action.equals("set") ? parseNonNegativeAmount(args[2]) : parsePositiveAmount(args[2]);
        if (amount.isEmpty()) {
            context.send(sender, "<red>Amount must be a valid number.");
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
            context.send(sender, "<red>Economy transaction was cancelled by another plugin.");
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
            context.send(sender, "<red>Usage: /" + label + " <give|take|set> <player> <amount>");
            return true;
        }
        if (!response.transactionSuccess()) {
            context.send(sender, "<red>" + response.errorMessage);
            return true;
        }

        context.send(sender, "<green>Balance for <white>" + playerName(target) + " <green>is now <white>" + economy.format(response.balance) + "<green>.");
        return true;
    }

    private Optional<OfflinePlayer> findKnownPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(online);
        }
        return Optional.ofNullable(Bukkit.getOfflinePlayerIfCached(name));
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
