package net.axther.hydroxide.api.event;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.math.BigDecimal;

public final class EconomyTransactionEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final OfflinePlayer initiator;
    private final OfflinePlayer source;
    private final OfflinePlayer target;
    private final TransactionType transactionType;
    private final String reason;
    private double amount;
    private boolean cancelled;

    public EconomyTransactionEvent(
            OfflinePlayer initiator,
            OfflinePlayer source,
            OfflinePlayer target,
            TransactionType transactionType,
            double amount,
            String reason
    ) {
        this.initiator = initiator;
        this.source = source;
        this.target = target;
        this.transactionType = transactionType;
        this.reason = reason;
        setAmount(amount);
    }

    public OfflinePlayer initiator() {
        return initiator;
    }

    public OfflinePlayer source() {
        return source;
    }

    public OfflinePlayer target() {
        return target;
    }

    public TransactionType transactionType() {
        return transactionType;
    }

    public double amount() {
        return amount;
    }

    public void setAmount(double amount) {
        if (!validAmount(amount)) {
            throw new IllegalArgumentException("Amount must be finite, non-negative, and limited to two decimals.");
        }
        this.amount = amount;
    }

    public String reason() {
        return reason;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    private static boolean validAmount(double amount) {
        return amount >= 0.0
                && !Double.isNaN(amount)
                && !Double.isInfinite(amount)
                && BigDecimal.valueOf(amount).scale() <= 2;
    }

    public enum TransactionType {
        PAY,
        ADMIN_GIVE,
        ADMIN_TAKE,
        ADMIN_SET,
        SHOP_BUY,
        SHOP_SELL,
        CHEQUE_CREATE,
        CHEQUE_REDEEM
    }
}
