package net.axther.hydroxide.modules.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.axther.hydroxide.HydroxideContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ChatListener implements Listener {

    private static final String FALLBACK_CHAT_FORMAT = "<dark_gray>[<#44CCFF>{world}</#44CCFF>] <white>{name}<dark_gray>: <white>{message}";

    private final HydroxideContext context;
    private final ChatColorSettings chatColors;
    private final ChatControlStore controls;
    private final MessageCommands messageCommands;

    public ChatListener(HydroxideContext context) {
        this(context, null);
    }

    ChatListener(HydroxideContext context, ChatColorSettings chatColors) {
        this(context, chatColors, null, null);
    }

    ChatListener(HydroxideContext context, ChatColorSettings chatColors, ChatControlStore controls, MessageCommands messageCommands) {
        this.context = context;
        this.chatColors = chatColors;
        this.controls = controls;
        this.messageCommands = messageCommands;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (handleFocusedPrivateChat(event)) {
            return;
        }
        if (blockGlobalMute(event)) {
            return;
        }
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            String template = context.messages().template("chat.format", FALLBACK_CHAT_FORMAT);
            String messageText = context.text().plain(message);
            boolean allowPlayerFormatting = source.hasPermission("hydroxide.chat.color")
                    && context.plugin().getConfig().getBoolean("chat.allow-player-color-codes", true);
            Optional<ColorPalette.Selection> selectedColor = chatColors == null
                    ? Optional.empty()
                    : chatColors.selected(source.getUniqueId());
            Component renderedMessage = ChatMessageColorizer.render(context.text(), message, messageText, allowPlayerFormatting, selectedColor);
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

    private boolean blockGlobalMute(AsyncChatEvent event) {
        if (controls == null || event.getPlayer().hasPermission("hydroxide.command.mutechat.bypass")) {
            return false;
        }
        Optional<GlobalChatMute> mute = controls.globalMute(Instant.now());
        if (mute.isEmpty()) {
            return false;
        }
        event.setCancelled(true);
        GlobalChatMute active = mute.orElseThrow();
        Bukkit.getScheduler().runTask(context.plugin(), () -> context.message(event.getPlayer(), "chat.mutechat.blocked", Map.of(
                "reason", active.reason(),
                "remaining", formatDuration(active.remaining(Instant.now())),
                "issuer", active.issuer()
        )));
        return true;
    }

    private boolean handleFocusedPrivateChat(AsyncChatEvent event) {
        if (controls == null || messageCommands == null) {
            return false;
        }
        Player player = event.getPlayer();
        Optional<UUID> targetId = controls.privateChatTarget(player.getUniqueId());
        if (targetId.isEmpty()) {
            return false;
        }

        event.setCancelled(true);
        String message = context.text().plain(event.message());
        UUID target = targetId.orElseThrow();
        Bukkit.getScheduler().runTask(context.plugin(), () -> deliverFocusedPrivateChat(player, target, message));
        return true;
    }

    private void deliverFocusedPrivateChat(Player player, UUID targetId, String message) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            controls.clearPrivateChatTarget(player.getUniqueId());
            context.message(player, "chat.private-chat.target-lost", Map.of("target", knownName(targetId)));
            return;
        }
        messageCommands.deliver(player, target, message);
    }

    private String knownName(UUID playerId) {
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name == null ? playerId.toString() : name;
    }

    private String applyPlaceholders(String input, String world, String name) {
        return input
                .replace("{world}", world)
                .replace("{name}", name);
    }

    private String formatDuration(Duration duration) {
        long seconds = Math.max(0L, duration.getSeconds());
        long days = seconds / 86_400L;
        seconds %= 86_400L;
        long hours = seconds / 3_600L;
        seconds %= 3_600L;
        long minutes = seconds / 60L;
        seconds %= 60L;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d");
        }
        if (hours > 0) {
            builder.append(hours).append("h");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m");
        }
        if (seconds > 0 || builder.isEmpty()) {
            builder.append(seconds).append("s");
        }
        return builder.toString();
    }
}
