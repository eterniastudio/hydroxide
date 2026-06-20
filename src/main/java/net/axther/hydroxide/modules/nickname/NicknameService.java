package net.axther.hydroxide.modules.nickname;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.axther.hydroxide.storage.PlayerDataStore;
import net.axther.hydroxide.text.TextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class NicknameService {

    private static final Pattern LEGACY_COLOR = Pattern.compile("(?i)&[0-9a-fk-or]");
    private static final Pattern HEX_COLOR = Pattern.compile("(?i)(?:&#[0-9a-f]{6}|&x(?:&[0-9a-f]){6}|<#[0-9a-f]{6}>)");
    private static final Pattern GRADIENT = Pattern.compile("(?i)<gradient(?::[^>]*)?>");
    private static final Pattern RAINBOW = Pattern.compile("(?i)<rainbow(?::[^>]*)?>");
    private static final Pattern MINI_COLOR = Pattern.compile("(?i)<(?:black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>");
    private static final Pattern PROFILE_SAFE = Pattern.compile("[^A-Za-z0-9_]");
    private static final int MAX_PROFILE_NAME_LENGTH = 16;

    private final TextFormatter formatter;
    private final Map<UUID, String> rawNicknames = new HashMap<>();
    private final Map<UUID, String> realNames = new HashMap<>();
    private final Map<String, UUID> lookupByNickname = new HashMap<>();
    private final Map<UUID, PlayerProfile> originalProfiles = new HashMap<>();
    private final Map<UUID, NameplateState> nameplates = new HashMap<>();

    public NicknameService(TextFormatter formatter) {
        this.formatter = formatter;
    }

    public void load(PlayerDataStore dataStore) {
        rawNicknames.clear();
        realNames.clear();
        lookupByNickname.clear();
        dataStore.nicknames().values().forEach(nickname ->
                cacheNickname(nickname.playerId(), nickname.playerName(), nickname.nickname()));
    }

    public void cacheNickname(UUID playerId, String realName, String rawNickname) {
        rawNicknames.put(playerId, rawNickname);
        realNames.put(playerId, realName);
        String plainNickname = formatter.plain(formatter.format(rawNickname));
        lookupByNickname.put(normalizeLookup(rawNickname), playerId);
        lookupByNickname.put(normalizeLookup(plainNickname), playerId);
        lookupByNickname.put(normalizeLookup(sanitizeProfileName(rawNickname)), playerId);
    }

    public void uncacheNickname(UUID playerId) {
        rawNicknames.remove(playerId);
        realNames.remove(playerId);
        lookupByNickname.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
    }

    public Optional<String> rawNickname(UUID playerId) {
        return Optional.ofNullable(rawNicknames.get(playerId));
    }

    public Optional<Component> formattedNickname(UUID playerId) {
        return rawNickname(playerId).map(formatter::format);
    }

    public Optional<String> strippedNickname(UUID playerId) {
        return rawNickname(playerId).map(raw -> formatter.plain(formatter.format(raw)));
    }

    public void cacheNameplate(UUID playerId, NameplateState state) {
        if (state.empty()) {
            nameplates.remove(playerId);
        } else {
            nameplates.put(playerId, state);
        }
    }

    public Optional<NameplateState> nameplate(UUID playerId) {
        return Optional.ofNullable(nameplates.get(playerId));
    }

    public String legacyFormattedNickname(UUID playerId, String fallbackName) {
        return rawNickname(playerId)
                .map(raw -> net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .serialize(formatter.format(raw)))
                .orElse(fallbackName);
    }

    public Optional<String> realName(String nickname) {
        UUID playerId = lookupByNickname.get(normalizeLookup(nickname));
        return playerId == null ? Optional.empty() : Optional.ofNullable(realNames.get(playerId));
    }

    public void apply(Player player, String rawNickname) {
        cacheNickname(player.getUniqueId(), player.getName(), rawNickname);

        Component nickname = formatter.format(rawNickname);
        originalProfiles.putIfAbsent(player.getUniqueId(), player.getPlayerProfile().clone());
        PlayerProfile currentProfile = player.getPlayerProfile();
        PlayerProfile profile = Bukkit.createProfileExact(player.getUniqueId(), sanitizeProfileName(rawNickname));
        profile.setTextures(currentProfile.getTextures());
        profile.setProperties(currentProfile.getProperties());
        player.setPlayerProfile(profile);

        player.displayName(nickname);
        player.playerListName(nickname);
        player.customName(nickname);
        player.setCustomNameVisible(true);
        refreshTeam(player);
    }

    public void applyNameplate(Player player, NameplateState state) {
        cacheNameplate(player.getUniqueId(), state);
        refreshTeam(player);
    }

    public void resetNameplate(Player player) {
        nameplates.remove(player.getUniqueId());
        refreshTeam(player);
    }

    public void reset(Player player) {
        uncacheNickname(player.getUniqueId());
        PlayerProfile originalProfile = originalProfiles.remove(player.getUniqueId());
        if (originalProfile != null) {
            player.setPlayerProfile(originalProfile);
        }
        Component realName = Component.text(player.getName());
        player.displayName(realName);
        player.playerListName(realName);
        player.customName(null);
        player.setCustomNameVisible(false);
        refreshTeam(player);
    }

    public void shutdown() {
        Set<UUID> playerIds = new java.util.HashSet<>(rawNicknames.keySet());
        playerIds.addAll(nameplates.keySet());
        for (UUID playerId : playerIds) {
            unregisterTeam(playerId);
        }
        rawNicknames.clear();
        realNames.clear();
        lookupByNickname.clear();
        originalProfiles.clear();
        nameplates.clear();
    }

    public static boolean requiresColorPermission(String nickname) {
        return LEGACY_COLOR.matcher(nickname).find()
                || HEX_COLOR.matcher(nickname).find()
                || MINI_COLOR.matcher(nickname).find();
    }

    public static boolean requiresHexPermission(String nickname) {
        return HEX_COLOR.matcher(nickname).find();
    }

    public static boolean requiresGradientPermission(String nickname) {
        return GRADIENT.matcher(nickname).find();
    }

    public static boolean requiresRainbowPermission(String nickname) {
        return RAINBOW.matcher(nickname).find();
    }

    public static String sanitizeProfileName(String rawNickname) {
        String strippedTags = rawNickname
                .replaceAll("(?i)<[^>]+>", "")
                .replaceAll("(?i)&x(?:&[0-9a-f]){6}", "")
                .replaceAll("(?i)&#[0-9a-f]{6}", "")
                .replaceAll("(?i)&[0-9a-fk-or]", "");
        String sanitized = PROFILE_SAFE.matcher(strippedTags).replaceAll("");
        if (sanitized.isBlank()) {
            return "Player";
        }
        return sanitized.length() > MAX_PROFILE_NAME_LENGTH
                ? sanitized.substring(0, MAX_PROFILE_NAME_LENGTH)
                : sanitized;
    }

    public static ValidationResult validateNickname(String rawNickname, NicknamePolicy policy) {
        TextFormatter formatter = new TextFormatter();
        String plainNickname = formatter.plain(formatter.format(rawNickname));
        if (plainNickname.isBlank()) {
            return ValidationResult.invalid("Nickname cannot be blank.");
        }
        if (plainNickname.length() > policy.maxLength()) {
            return ValidationResult.invalid("Nickname must be " + policy.maxLength() + " characters or fewer.");
        }
        Pattern allowedPattern;
        try {
            allowedPattern = Pattern.compile(policy.allowedPattern());
        } catch (PatternSyntaxException exception) {
            return ValidationResult.invalid("Nickname character filter is misconfigured.");
        }
        if (!allowedPattern.matcher(plainNickname).matches()) {
            return ValidationResult.invalid("Nickname contains blocked characters.");
        }
        String lowered = plainNickname.toLowerCase(Locale.ROOT);
        for (String blocked : policy.blockedWords()) {
            if (!blocked.isBlank() && lowered.contains(blocked.toLowerCase(Locale.ROOT))) {
                return ValidationResult.invalid("Nickname contains blocked language.");
            }
        }
        return ValidationResult.success();
    }

    public record NicknamePolicy(int maxLength, String allowedPattern, List<String> blockedWords) {
        public static NicknamePolicy defaults() {
            return new NicknamePolicy(16, "^[A-Za-z0-9_ .-]+$", List.of());
        }
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }

    public record NameplateState(String prefix, String suffix, NamedTextColor colorValue) {
        public NameplateState {
            prefix = prefix == null ? "" : prefix;
            suffix = suffix == null ? "" : suffix;
        }

        public Optional<NamedTextColor> color() {
            return Optional.ofNullable(colorValue);
        }

        public boolean empty() {
            return prefix.isBlank() && suffix.isBlank() && colorValue == null;
        }

        public NameplateState withPrefix(String value) {
            return new NameplateState(value, suffix, colorValue);
        }

        public NameplateState withSuffix(String value) {
            return new NameplateState(prefix, value, colorValue);
        }

        public NameplateState withColor(NamedTextColor value) {
            return new NameplateState(prefix, suffix, value);
        }

        public NameplateState withoutColor() {
            return new NameplateState(prefix, suffix, null);
        }

        public static NameplateState emptyState() {
            return new NameplateState("", "", null);
        }
    }

    private void refreshTeam(Player player) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            return;
        }
        refreshTeam(scoreboardManager.getMainScoreboard(), player);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            refreshTeam(viewer.getScoreboard(), player);
        }
    }

    private void refreshTeam(Scoreboard scoreboard, Player player) {
        UUID playerId = player.getUniqueId();
        NameplateState nameplate = nameplates.get(playerId);
        Optional<String> rawNickname = rawNickname(playerId);
        if ((nameplate == null || nameplate.empty()) && rawNickname.isEmpty()) {
            unregisterTeam(scoreboard, playerId);
            return;
        }

        Team team = scoreboard.getTeam(teamName(player.getUniqueId()));
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName(player.getUniqueId()));
        }
        team.prefix(teamPrefix(nameplate, rawNickname));
        team.suffix(nameplate == null ? Component.empty() : formatter.format(nameplate.suffix()));
        team.color(nameplate == null ? NamedTextColor.WHITE : nameplate.color().orElse(NamedTextColor.WHITE));
        team.addEntry(player.getName());
        team.addEntry(sanitizeProfileName(rawNicknames.getOrDefault(player.getUniqueId(), player.getName())));
    }

    private Component teamPrefix(NameplateState nameplate, Optional<String> rawNickname) {
        Component prefix = nameplate == null ? Component.empty() : formatter.format(nameplate.prefix());
        return rawNickname
                .map(raw -> prefix.append(formatter.format(raw)).append(Component.space()))
                .orElse(prefix);
    }

    private void unregisterTeam(UUID playerId) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            return;
        }
        unregisterTeam(scoreboardManager.getMainScoreboard(), playerId);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            unregisterTeam(viewer.getScoreboard(), playerId);
        }
    }

    private void unregisterTeam(Scoreboard scoreboard, UUID playerId) {
        Team team = scoreboard.getTeam(teamName(playerId));
        if (team != null) {
            team.unregister();
        }
    }

    private String teamName(UUID playerId) {
        return "hxN" + playerId.toString().replace("-", "").substring(0, 13);
    }

    private String normalizeLookup(String value) {
        return formatter.plain(formatter.format(value))
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "");
    }
}
