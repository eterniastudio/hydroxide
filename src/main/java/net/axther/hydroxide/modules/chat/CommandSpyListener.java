package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.HydroxideContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CommandSpyListener implements Listener {

    private final HydroxideContext context;
    private final ChatControlStore controls;

    CommandSpyListener(HydroxideContext context, ChatControlStore controls) {
        this.context = context;
        this.controls = controls;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        List<String> excludedPrefixes = excludedPrefixes();
        if (CommandSpyFormatter.shouldSkip(event.getMessage(), excludedPrefixes)) {
            return;
        }

        Component component = context.messages().component("chat.commandspy.format", Map.of(
                "player", player.getName(),
                "command", CommandSpyFormatter.displayCommand(event.getMessage())
        ));
        for (UUID spyId : controls.commandSpies()) {
            if (spyId.equals(player.getUniqueId())) {
                continue;
            }
            Player staff = Bukkit.getPlayer(spyId);
            if (staff != null && staff.hasPermission("hydroxide.command.commandspy")) {
                staff.sendMessage(component);
            }
        }
    }

    private List<String> excludedPrefixes() {
        List<String> configured = context.plugin().getConfig().getStringList("chat.command-spy.excluded-prefixes");
        return configured.isEmpty() ? CommandSpyFormatter.defaultExcludedPrefixes() : configured;
    }
}
