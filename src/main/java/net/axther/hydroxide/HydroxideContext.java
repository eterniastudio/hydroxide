package net.axther.hydroxide;

import net.axther.hydroxide.commands.CommandRegistrar;
import net.axther.hydroxide.messages.MessageService;
import net.axther.hydroxide.modules.ModuleManager;
import net.axther.hydroxide.storage.NamedLocationStore;
import net.axther.hydroxide.storage.PlayerDataStore;
import net.axther.hydroxide.teleport.BackLocationService;
import net.axther.hydroxide.teleport.TeleportRequestService;
import net.axther.hydroxide.text.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.Map;

public record HydroxideContext(
        Hydroxide plugin,
        TextFormatter text,
        MessageService messages,
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
        return messages.prefix();
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(prefix().append(text.format(message)));
    }

    public void message(CommandSender sender, String key, Map<String, ?> placeholders) {
        messages.send(sender, key, placeholders);
    }

    public boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        message(sender, "validation.no-permission", Map.of("permission", permission));
        return false;
    }
}
