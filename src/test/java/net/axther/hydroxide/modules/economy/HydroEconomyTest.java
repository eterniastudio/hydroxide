package net.axther.hydroxide.modules.economy;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HydroEconomyTest {

    @Test
    void defaultsNewAccountsToStartingBalance() {
        FakeAccounts accounts = new FakeAccounts(250.0);
        HydroEconomy economy = new HydroEconomy(accounts, 250.0, "$", "dollars");
        OfflinePlayer player = offlinePlayer(UUID.randomUUID(), "Axther");

        assertEquals(250.0, economy.getBalance(player));
        assertTrue(economy.has(player, 250.0));
    }

    @Test
    void depositsAndWithdrawsSafely() {
        FakeAccounts accounts = new FakeAccounts(100.0);
        HydroEconomy economy = new HydroEconomy(accounts, 100.0, "$", "dollars");
        OfflinePlayer player = offlinePlayer(UUID.randomUUID(), "Axther");

        EconomyResponse deposit = economy.depositPlayer(player, 50.0);
        EconomyResponse withdraw = economy.withdrawPlayer(player, 25.0);

        assertEquals(EconomyResponse.ResponseType.SUCCESS, deposit.type);
        assertEquals(EconomyResponse.ResponseType.SUCCESS, withdraw.type);
        assertEquals(125.0, economy.getBalance(player));
    }

    @Test
    void rejectsNegativeAndOverdraftWithdrawals() {
        FakeAccounts accounts = new FakeAccounts(10.0);
        HydroEconomy economy = new HydroEconomy(accounts, 10.0, "$", "dollars");
        OfflinePlayer player = offlinePlayer(UUID.randomUUID(), "Axther");

        assertEquals(EconomyResponse.ResponseType.FAILURE, economy.depositPlayer(player, -1.0).type);
        assertEquals(EconomyResponse.ResponseType.FAILURE, economy.withdrawPlayer(player, -1.0).type);
        assertEquals(EconomyResponse.ResponseType.FAILURE, economy.withdrawPlayer(player, 20.0).type);
        assertEquals(10.0, economy.getBalance(player));
    }

    @Test
    void rejectsNanInfiniteAndMoreThanTwoDecimalPlaces() {
        FakeAccounts accounts = new FakeAccounts(10.0);
        HydroEconomy economy = new HydroEconomy(accounts, 10.0, "$", "dollars");
        OfflinePlayer player = offlinePlayer(UUID.randomUUID(), "Axther");

        assertEquals(EconomyResponse.ResponseType.FAILURE, economy.depositPlayer(player, Double.NaN).type);
        assertEquals(EconomyResponse.ResponseType.FAILURE, economy.depositPlayer(player, Double.POSITIVE_INFINITY).type);
        assertEquals(EconomyResponse.ResponseType.FAILURE, economy.depositPlayer(player, 1.001).type);
        assertEquals(EconomyResponse.ResponseType.SUCCESS, economy.depositPlayer(player, 1.01).type);
        assertEquals(11.01, economy.getBalance(player));
    }

    private static final class FakeAccounts implements HydroEconomy.AccountStore {
        private final double startingBalance;
        private final Map<UUID, Double> balances = new HashMap<>();

        private FakeAccounts(double startingBalance) {
            this.startingBalance = startingBalance;
        }

        @Override
        public double balance(UUID playerId, double defaultBalance) {
            return balances.computeIfAbsent(playerId, ignored -> startingBalance);
        }

        @Override
        public void setBalance(UUID playerId, double balance) {
            balances.put(playerId, balance);
        }
    }

    private static OfflinePlayer offlinePlayer(UUID uniqueId, String name) {
        return (OfflinePlayer) Proxy.newProxyInstance(
                HydroEconomyTest.class.getClassLoader(),
                new Class<?>[]{OfflinePlayer.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uniqueId;
                    case "getName" -> name;
                    case "hasPlayedBefore" -> true;
                    case "isOnline", "isBanned", "isWhitelisted" -> false;
                    case "toString" -> "OfflinePlayer[" + name + "]";
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class || type == long.class || type == short.class || type == byte.class) {
            return 0;
        }
        if (type == double.class || type == float.class) {
            return 0.0;
        }
        return null;
    }
}
