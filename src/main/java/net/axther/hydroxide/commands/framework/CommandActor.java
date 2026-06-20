package net.axther.hydroxide.commands.framework;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface CommandActor {

    String name();

    boolean isPlayer();

    boolean hasPermission(String permission);

    void send(Component component);

    default Optional<CommandSender> sender() {
        return Optional.empty();
    }

    static CommandActor bukkit(CommandSender sender) {
        return new BukkitCommandActor(sender);
    }

    final class BukkitCommandActor implements CommandActor {
        private final CommandSender sender;

        private BukkitCommandActor(CommandSender sender) {
            this.sender = sender;
        }

        @Override
        public String name() {
            return sender.getName();
        }

        @Override
        public boolean isPlayer() {
            return sender instanceof Player;
        }

        @Override
        public boolean hasPermission(String permission) {
            return permission == null || permission.isBlank() || sender.hasPermission(permission);
        }

        @Override
        public void send(Component component) {
            sender.sendMessage(component);
        }

        @Override
        public Optional<CommandSender> sender() {
            return Optional.of(sender);
        }
    }
}
