package net.axther.hydroxide.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerNicknameChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CommandSender actor;
    private final String oldNickname;
    private final String newNickname;
    private boolean cancelled;

    public PlayerNicknameChangeEvent(Player player, CommandSender actor, String oldNickname, String newNickname) {
        this.player = player;
        this.actor = actor;
        this.oldNickname = oldNickname;
        this.newNickname = newNickname;
    }

    public Player player() {
        return player;
    }

    public CommandSender actor() {
        return actor;
    }

    public String oldNickname() {
        return oldNickname;
    }

    public String newNickname() {
        return newNickname;
    }

    public boolean clearing() {
        return newNickname == null || newNickname.isBlank();
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
}
