package net.axther.hydroxide;

import net.axther.hydroxide.commands.CommandRegistrar;
import net.axther.hydroxide.modules.ModuleManager;
import net.axther.hydroxide.storage.NamedLocationStore;
import net.axther.hydroxide.storage.PlayerDataStore;
import net.axther.hydroxide.teleport.BackLocationService;
import net.axther.hydroxide.teleport.TeleportRequestService;
import net.axther.hydroxide.text.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public record HydroxideContext(
        Hydroxide plugin,
        TextFormatter text,
        CommandRegistrar commands,
        ModuleManager modules,
        PlayerDataStore playerData,
        NamedLocationStore warps,
        NamedLocationStore spawns,
        BackLocationService backLocations,
        TeleportRequestService teleportRequests,
        HydroxideServices services
) {

    public Component prefix() {
        return text.format(plugin.getConfig().getString("messages.prefix",
                "<#44CCFF><bold>Hydroxide</bold> <dark_gray>> <gray>"));
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(prefix().append(text.format(message)));
    }

    public boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        send(sender, plugin.getConfig().getString("messages.no-permission", "<red>You do not have permission."));
        return false;
    }
}
