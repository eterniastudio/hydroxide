package net.axther.hydroxide.integrations.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.options.PlayerOption;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class HydroxidePlaceholderExpansion extends PlaceholderExpansion {

    private final HydroxideContext context;

    public HydroxidePlaceholderExpansion(HydroxideContext context) {
        this.context = context;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hydroxide";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", context.plugin().getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return context.plugin().getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        String lowered = params.toLowerCase(java.util.Locale.ROOT);
        if (lowered.startsWith("stat_")) {
            String stat = params.substring("stat_".length());
            return context.services().statsService()
                    .map(service -> String.valueOf(service.value(player.getUniqueId(), stat)))
                    .orElse("0");
        }
        if (lowered.startsWith("option_")) {
            PlayerOption option = PlayerOption.fromKey(params.substring("option_".length()));
            if (option == null) {
                return "";
            }
            return context.services().playerOptionsService()
                    .map(service -> service.enabled(player.getUniqueId(), option) ? "true" : "false")
                    .orElse(String.valueOf(option.defaultValue()));
        }
        if (lowered.startsWith("user_metaint_")) {
            String key = params.substring("user_metaint_".length());
            return context.services().userMetaService()
                    .flatMap(service -> service.integerValue(player.getUniqueId(), key))
                    .orElse("");
        }
        if (lowered.startsWith("user_meta_")) {
            String key = params.substring("user_meta_".length());
            return context.services().userMetaService()
                    .flatMap(service -> service.value(player.getUniqueId(), key))
                    .orElse("");
        }

        return switch (lowered) {
            case "nickname" -> context.services().nicknameService()
                    .map(service -> service.legacyFormattedNickname(player.getUniqueId(), fallbackName(player)))
                    .orElse(fallbackName(player));
            case "nickname_stripped" -> context.services().nicknameService()
                    .flatMap(service -> service.strippedNickname(player.getUniqueId()))
                    .orElse(fallbackName(player));
            case "balance" -> context.services().economy()
                    .map(economy -> economy.format(economy.getBalance(player)))
                    .orElse("");
            case "build_mode" -> context.services().builderService()
                    .map(service -> player.isOnline() && player.getPlayer() != null && service.buildMode(player.getPlayer()) ? "true" : "false")
                    .orElse("false");
            default -> null;
        };
    }

    private String fallbackName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }
}
