package net.axther.hydroxide.modules.economy;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class HydroEconomy extends AbstractEconomy {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private final AccountStore accountStore;
    private final double startingBalance;
    private final String currencySymbol;
    private final String currencyName;

    public HydroEconomy(AccountStore accountStore, double startingBalance, String currencySymbol, String currencyName) {
        this.accountStore = accountStore;
        this.startingBalance = startingBalance;
        this.currencySymbol = currencySymbol;
        this.currencyName = currencyName;
    }

    AccountStore accountStore() {
        return accountStore;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "Hydroxide";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return currencySymbol + MONEY_FORMAT.format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return currencyName;
    }

    @Override
    public String currencyNameSingular() {
        return currencyName;
    }

    @Override
    public boolean hasAccount(String playerName) {
        return resolve(playerName) != null;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null && player.getUniqueId() != null;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer player = resolve(playerName);
        return player == null ? 0.0 : getBalance(player);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return accountStore.balance(player.getUniqueId(), startingBalance);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        OfflinePlayer player = resolve(playerName);
        return player != null && has(player, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return isValidAmount(amount) && getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        OfflinePlayer player = resolve(playerName);
        return player == null
                ? failure(amount, 0.0, "Unknown player.")
                : withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        if (!isValidAmount(amount)) {
            return failure(amount, balance, "Amount must be positive and finite.");
        }
        if (balance < amount) {
            return failure(amount, balance, "Insufficient funds.");
        }
        double newBalance = balance - amount;
        accountStore.setBalance(player.getUniqueId(), newBalance);
        return success(amount, newBalance);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        OfflinePlayer player = resolve(playerName);
        return player == null
                ? failure(amount, 0.0, "Unknown player.")
                : depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        if (!isValidAmount(amount)) {
            return failure(amount, balance, "Amount must be positive and finite.");
        }
        double newBalance = balance + amount;
        accountStore.setBalance(player.getUniqueId(), newBalance);
        return success(amount, newBalance);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return bankUnsupported();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return bankUnsupported();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        OfflinePlayer player = resolve(playerName);
        return player != null && createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        accountStore.balance(player.getUniqueId(), startingBalance);
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    private OfflinePlayer resolve(String playerName) {
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(playerName);
        return cached == null ? Bukkit.getOfflinePlayer(playerName) : cached;
    }

    private boolean isValidAmount(double amount) {
        if (amount < 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return false;
        }
        return java.math.BigDecimal.valueOf(amount).scale() <= fractionalDigits();
    }

    private EconomyResponse success(double amount, double balance) {
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null);
    }

    private EconomyResponse failure(double amount, double balance, String message) {
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, message);
    }

    private EconomyResponse bankUnsupported() {
        return failure(0.0, 0.0, "Hydroxide does not support bank accounts.");
    }

    public interface AccountStore {
        double balance(UUID playerId, double defaultBalance);

        void setBalance(UUID playerId, double balance);
    }
}
