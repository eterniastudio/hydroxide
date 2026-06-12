package net.axther.hydroxide.modules.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.axther.hydroxide.HydroxideContext;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ChatListener implements Listener {

    private final HydroxideContext context;

    public ChatListener(HydroxideContext context) {
        this.context = context;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            String template = context.plugin().getConfig().getString("chat.format",
                    "<dark_gray>[<#44CCFF>{world}</#44CCFF>] <white>{name}<dark_gray>: <white>{message}");
            String messageText = context.text().plain(message);
            Component renderedMessage = source.hasPermission("hydroxide.chat.color")
                    && context.plugin().getConfig().getBoolean("chat.allow-player-color-codes", true)
                    ? context.text().format(messageText)
                    : message;
            Component renderedDisplayName = context.services().nicknameService()
                    .flatMap(service -> service.formattedNickname(source.getUniqueId()))
                    .orElse(sourceDisplayName);

            String beforeName = template;
            String betweenNameAndMessage = "";
            String afterMessage = "";
            int nameToken = template.indexOf("{name}");
            int messageToken = template.indexOf("{message}");
            if (nameToken >= 0 && messageToken >= 0 && nameToken < messageToken) {
                beforeName = template.substring(0, nameToken);
                betweenNameAndMessage = template.substring(nameToken + "{name}".length(), messageToken);
                afterMessage = template.substring(messageToken + "{message}".length());
            } else {
                beforeName = template;
            }

            if (messageToken >= 0) {
                beforeName = applyPlaceholders(beforeName, source.getWorld().getName(), source.getName());
                betweenNameAndMessage = applyPlaceholders(betweenNameAndMessage, source.getWorld().getName(), source.getName());
                afterMessage = applyPlaceholders(afterMessage, source.getWorld().getName(), source.getName());
                return context.text().format(beforeName)
                        .append(renderedDisplayName)
                        .append(context.text().format(betweenNameAndMessage))
                        .append(renderedMessage)
                        .append(context.text().format(afterMessage));
            }

            beforeName = applyPlaceholders(beforeName, source.getWorld().getName(), source.getName());
            return context.text().format(beforeName).append(renderedMessage);
        });
    }

    private String applyPlaceholders(String input, String world, String name) {
        return input
                .replace("{world}", world)
                .replace("{name}", name);
    }
}
