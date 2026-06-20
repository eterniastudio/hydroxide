package net.axther.hydroxide.modules.moderation;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.registry.ModernRegistryKeys;
import net.axther.hydroxide.storage.YamlStore;
import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.ban.BanListType;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModerationCommands implements Listener {

    private static final List<String> GAMEMODES = List.of("survival", "creative", "adventure", "spectator");
    private static final int BANLIST_PAGE_SIZE = 10;
    private static final double DEFAULT_MAX_HEALTH = 20.0D;
    private static final double MIN_MAX_HEALTH = 1.0D;
    private static final double MAX_MAX_HEALTH = 2048.0D;
    private static final double MIN_SCALE = 0.0625D;
    private static final double MAX_SCALE = 16.0D;
    private static final String GLOW_TEAM_PREFIX = "hxg_";
    private final HydroxideContext context;
    private final Set<UUID> godPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> temporaryFlyTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> temporaryFlyOriginalState = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> temporaryFlyExpirations = new ConcurrentHashMap<>();
    private final Set<UUID> temporaryFlyIndefinite = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> temporaryGodTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> temporaryGodOriginalState = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> temporaryGodExpirations = new ConcurrentHashMap<>();
    private final Set<UUID> temporaryGodIndefinite = ConcurrentHashMap.newKeySet();
    private final Set<UUID> noTargetPlayers = new HashSet<>();
    private final Set<UUID> cuffedPlayers = ConcurrentHashMap.newKeySet();
    private final YamlStore muteYaml;
    private final YamlStore warningYaml;
    private MuteStore muteStore;
    private WarningStore warningStore;

    public ModerationCommands(HydroxideContext context) {
        this.context = context;
        this.muteYaml = new YamlStore(new File(context.plugin().getDataFolder(), "mutes.yml"));
        this.muteStore = MuteStore.from(muteYaml.load());
        this.warningYaml = new YamlStore(new File(context.plugin().getDataFolder(), "warnings.yml"));
        this.warningStore = WarningStore.from(warningYaml.load());
    }

    public CommandService command(String name) {
        return switch (name) {
            case "fly" -> service("fly", "hydroxide.command.fly", "/{label} [player]",
                    ctx -> fly(ctx.sender(), ctx.arguments().toArray(String[]::new)), ctx -> onlinePlayerNames(ctx.argument(0)));
            case "tfly" -> service("tfly", "hydroxide.command.tfly", "/{label} <player> [timeInSec|+seconds|0] [-s]",
                    ctx -> temporaryMode(ctx.sender(), ctx.label(), ctx.arguments(), TemporaryMode.FLY), this::temporaryModeCompletions);
            case "god" -> service("god", "hydroxide.command.god", "/{label} [player]",
                    ctx -> god(ctx.sender(), ctx.arguments().toArray(String[]::new)), ctx -> onlinePlayerNames(ctx.argument(0)));
            case "tgod" -> service("tgod", "hydroxide.command.tgod", "/{label} <player> [timeInSec|+seconds|0] [-s]",
                    ctx -> temporaryMode(ctx.sender(), ctx.label(), ctx.arguments(), TemporaryMode.GOD), this::temporaryModeCompletions);
            case "heal" -> service("heal", "hydroxide.command.heal", "/{label} [player|all] [amount|percent]",
                    ctx -> heal(ctx.sender(), ctx.label(), ctx.arguments()), this::healCompletions);
            case "feed" -> service("feed", "hydroxide.command.feed", "/{label} [player|all] [-s]",
                    ctx -> feed(ctx.sender(), ctx.label(), ctx.arguments()), this::feedCompletions);
            case "hunger" -> service("hunger", "hydroxide.command.hunger", "/{label} <player|all> <0-20>",
                    ctx -> hunger(ctx.sender(), ctx.label(), ctx.arguments()), this::hungerCompletions);
            case "saturation" -> service("saturation", "hydroxide.command.saturation", "/{label} <player|all> <0-20>",
                    ctx -> saturation(ctx.sender(), ctx.label(), ctx.arguments()), this::saturationCompletions);
            case "maxhp" -> service("maxhp", "hydroxide.command.maxhp", "/{label} <set|add|take|clear> [player] [amount]",
                    ctx -> maxHealth(ctx.sender(), ctx.label(), ctx.arguments()), this::maxHealthCompletions);
            case "scale" -> service("scale", "hydroxide.command.scale", "/{label} <set|add|take|clear> [player] [amount] [-s]",
                    ctx -> scale(ctx.sender(), ctx.label(), ctx.arguments()), this::scaleCompletions);
            case "glow" -> service("glow", "hydroxide.command.glow", "/{label} [player] [on|off|toggle|color]",
                    ctx -> glow(ctx.sender(), ctx.label(), ctx.arguments()), this::glowCompletions);
            case "notarget" -> service("notarget", "hydroxide.command.notarget", "/{label} [player] [true|false|toggle]",
                    ctx -> noTarget(ctx.sender(), ctx.label(), ctx.arguments()), this::noTargetCompletions);
            case "playercollision" -> service("playercollision", "hydroxide.command.playercollision", "/{label} [player] [true|false|toggle] [-s]",
                    ctx -> playerCollision(ctx.sender(), ctx.label(), ctx.arguments()), this::playerCollisionCompletions);
            case "cuff" -> service("cuff", "hydroxide.command.cuff", "/{label} <player> [true|false|toggle] [-s]",
                    ctx -> cuff(ctx.sender(), ctx.label(), ctx.arguments()), this::cuffCompletions);
            case "speed" -> service("speed", "hydroxide.command.speed", "/{label} <1-10> [walk|fly] [player]",
                    ctx -> speed(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::speedCompletions);
            case "gamemode" -> service("gamemode", "hydroxide.command.gamemode", "/{label} <mode> [player]",
                    ctx -> gameMode(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::gameModeCompletions);
            case "effect" -> service("effect", "hydroxide.command.effect", "/{label} <player|all> <effect|clear> [seconds] [amplifier] [-visual]",
                    ctx -> effect(ctx.sender(), ctx.label(), ctx.arguments()), this::effectCompletions);
            case "air" -> service("air", "hydroxide.command.air", "/{label} <player|all> <ticks|max>",
                    ctx -> air(ctx.sender(), ctx.label(), ctx.arguments()), this::airCompletions);
            case "falldistance" -> service("falldistance", "hydroxide.command.falldistance", "/{label} <player|all> <distance>",
                    ctx -> fallDistance(ctx.sender(), ctx.label(), ctx.arguments()), this::fallDistanceCompletions);
            case "kick" -> service("kick", "hydroxide.command.kick", "/{label} <player|all> [reason] [-s]",
                    ctx -> kick(ctx.sender(), ctx.label(), ctx.arguments()),
                    this::kickCompletions);
            case "kickall" -> service("kickall", "hydroxide.command.kick", "/{label} [reason] [-s]",
                    ctx -> kickAll(ctx.sender(), ctx.label(), ctx.arguments()),
                    this::kickAllCompletions);
            case "ban" -> service("ban", "hydroxide.command.ban", "/{label} <player> [reason]",
                    ctx -> ban(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> onlinePlayerNames(ctx.argument(0)));
            case "banlist" -> service("banlist", "hydroxide.command.banlist", "/{label} [page]",
                    ctx -> banList(ctx.sender(), ctx.label(), ctx.arguments()), this::banListCompletions);
            case "checkban" -> service("checkban", "hydroxide.command.checkban", "/{label} <player>",
                    ctx -> checkBan(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> ctx.arguments().size() == 1 ? bannedTargetNames(ctx.argument(0)) : List.of());
            case "tempban" -> service("tempban", "hydroxide.command.tempban", "/{label} <player> <duration> [reason]",
                    ctx -> tempBan(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> ctx.arguments().size() == 1 ? onlinePlayerNames(ctx.argument(0)) : List.of());
            case "unban" -> service("unban", "hydroxide.command.unban", "/{label} <player>",
                    ctx -> unban(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), null);
            case "ipban" -> service("ipban", "hydroxide.command.ipban", "/{label} <ip|player> [reason] [-s]",
                    ctx -> ipBan(ctx.sender(), ctx.label(), ctx.arguments()), this::ipBanCompletions);
            case "ipbanlist" -> service("ipbanlist", "hydroxide.command.ipbanlist", "/{label} [page]",
                    ctx -> ipBanList(ctx.sender(), ctx.label(), ctx.arguments()), this::ipBanListCompletions);
            case "unbanip" -> service("unbanip", "hydroxide.command.unbanip", "/{label} <ip>",
                    ctx -> unbanIp(ctx.sender(), ctx.label(), ctx.arguments()), this::unbanIpCompletions);
            case "mute" -> service("mute", "hydroxide.command.mute", "/{label} <player> [reason]",
                    ctx -> mute(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> onlinePlayerNames(ctx.argument(0)));
            case "tempmute" -> service("tempmute", "hydroxide.command.tempmute", "/{label} <player> <duration> [reason]",
                    ctx -> tempMute(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> ctx.arguments().size() == 1 ? onlinePlayerNames(ctx.argument(0)) : List.of());
            case "unmute" -> service("unmute", "hydroxide.command.unmute", "/{label} <player>",
                    ctx -> unmute(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> onlinePlayerNames(ctx.argument(0)));
            case "warn" -> service("warn", "hydroxide.command.warn", "/{label} <player> <reason>",
                    ctx -> warn(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> onlinePlayerNames(ctx.argument(0)));
            case "warnings" -> service("warnings", "hydroxide.command.warnings", "/{label} <player>",
                    ctx -> warnings(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> onlinePlayerNames(ctx.argument(0)));
            case "clearwarnings" -> service("clearwarnings", "hydroxide.command.clearwarnings", "/{label} <player>",
                    ctx -> clearWarnings(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> onlinePlayerNames(ctx.argument(0)));
            case "editwarnings" -> service("editwarnings", "hydroxide.command.editwarnings", "/{label} <player|clearall> [clear]",
                    ctx -> editWarnings(ctx.sender(), ctx.label(), ctx.arguments()), this::editWarningsCompletions);
            default -> throw new IllegalArgumentException("Unknown moderation command: " + name);
        };
    }

    private CommandService service(String name, String permission, String usage,
                                   HydroCommand.HydroCommandExecutor executor,
                                   HydroCommand.HydroTabCompleter completer) {
        return new CommandService(HydroCommand.builder(name)
                .permission(permission)
                .usage(usage)
                .executor(executor)
                .completer(completer)
                .build(), context.messages());
    }

    private List<String> gameModeCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ModerationAliasParser.gameModeFromLabel(ctx.label()).isPresent()) {
            return ctx.arguments().size() == 1 ? onlinePlayerNames(ctx.argument(0)) : List.of();
        }
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), GAMEMODES);
        }
        if (ctx.arguments().size() == 2) {
            return onlinePlayerNames(ctx.argument(1));
        }
        return List.of();
    }

    private List<String> speedCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ModerationAliasParser.speedTypeFromLabel(ctx.label()).isPresent()) {
            if (ctx.arguments().size() == 1) {
                return CompletionUtils.integerRange(ctx.argument(0), 1, 10);
            }
            if (ctx.arguments().size() == 2) {
                return onlinePlayerNames(ctx.argument(1));
            }
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            return CompletionUtils.integerRange(ctx.argument(0), 1, 10);
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("walk", "fly"));
        }
        if (ctx.arguments().size() == 3) {
            return onlinePlayerNames(ctx.argument(2));
        }
        return List.of();
    }

    private List<String> healCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> values = new ArrayList<>(List.of("all", "10", "25%", "50%", "100%"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("5", "10", "25%", "50%", "100%"));
        }
        return List.of();
    }

    private List<String> feedCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(targetCompletions(ctx.argument(0)));
            values.addAll(CommandUtils.matching(ctx.argument(0), List.of("-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("-s"));
        }
        return List.of();
    }

    private List<String> temporaryModeCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return onlinePlayerNames(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("0", "30", "60", "+30", "-s"));
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("-s"));
        }
        return List.of();
    }

    private List<String> kickCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return targetCompletions(ctx.argument(0));
        }
        if (!ctx.arguments().contains("-s")) {
            return CommandUtils.matching(ctx.argument(ctx.arguments().size() - 1), List.of("-s"));
        }
        return List.of();
    }

    private List<String> kickAllCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (!ctx.arguments().contains("-s")) {
            return CommandUtils.matching(ctx.argument(ctx.arguments().size() - 1), List.of("-s"));
        }
        return List.of();
    }

    private List<String> editWarningsCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> values = new ArrayList<>(List.of("clearall"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("clear"));
        }
        return List.of();
    }

    private List<String> hungerCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return targetCompletions(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            return CompletionUtils.integerRange(ctx.argument(1), 0, 20);
        }
        return List.of();
    }

    private List<String> saturationCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return targetCompletions(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("0", "5", "10", "20"));
        }
        return List.of();
    }

    private List<String> maxHealthCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), List.of("set", "add", "take", "clear"));
        }
        if (ctx.arguments().size() == 2 && List.of("clear", "reset").contains(ctx.argument(0).toLowerCase(Locale.ROOT))) {
            return onlinePlayerNames(ctx.argument(1));
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new ArrayList<>(List.of("10", "20", "40"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(1), values);
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("10", "20", "40"));
        }
        return List.of();
    }

    private List<String> scaleCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), List.of("set", "add", "take", "clear"));
        }
        if (ctx.arguments().size() == 2 && List.of("clear", "reset").contains(ctx.argument(0).toLowerCase(Locale.ROOT))) {
            List<String> values = new ArrayList<>(onlinePlayerNames(ctx.argument(1)));
            values.addAll(CommandUtils.matching(ctx.argument(1), List.of("-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new ArrayList<>(List.of("0.5", "1", "1.5", "2", "-s"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(1), values);
        }
        if (ctx.arguments().size() == 3) {
            List<String> values = new ArrayList<>(List.of("0.5", "1", "1.5", "2"));
            if (ctx.arguments().stream().noneMatch("-s"::equalsIgnoreCase)) {
                values.add("-s");
            }
            return CommandUtils.matching(ctx.argument(2), values);
        }
        if (ctx.arguments().size() == 4 && ctx.arguments().stream().noneMatch("-s"::equalsIgnoreCase)) {
            return CommandUtils.matching(ctx.argument(3), List.of("-s"));
        }
        return List.of();
    }

    private List<String> glowCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> values = new ArrayList<>(ModerationGlowCommandParser.stateAndColorKeys());
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), ModerationGlowCommandParser.stateAndColorKeys());
        }
        return List.of();
    }

    private List<String> noTargetCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> values = new ArrayList<>(List.of("true", "false", "toggle"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("true", "false", "toggle"));
        }
        return List.of();
    }

    private List<String> playerCollisionCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> values = new ArrayList<>(List.of("true", "false", "toggle", "-s"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("true", "false", "toggle", "-s"));
        }
        if (ctx.arguments().size() == 3 && ctx.arguments().stream().noneMatch("-s"::equalsIgnoreCase)) {
            return CommandUtils.matching(ctx.argument(2), List.of("-s"));
        }
        return List.of();
    }

    private List<String> cuffCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return onlinePlayerNames(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("true", "false", "toggle", "-s"));
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("-s"));
        }
        return List.of();
    }

    private List<String> effectCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return targetCompletions(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new ArrayList<>(List.of("clear", "remove"));
            values.addAll(effectKeys());
            return CommandUtils.matching(ctx.argument(1), values);
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("30", "60", "120", "5m", "10m"));
        }
        if (ctx.arguments().size() == 4) {
            return CompletionUtils.integerRange(ctx.argument(3), 0, 5);
        }
        if (ctx.arguments().size() == 5) {
            return CommandUtils.matching(ctx.argument(4), List.of("-visual"));
        }
        return List.of();
    }

    private List<String> airCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return targetCompletions(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("max", "300", "600"));
        }
        return List.of();
    }

    private List<String> banListCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            int pages = Math.max(1, (bans().getEntries().size() + BANLIST_PAGE_SIZE - 1) / BANLIST_PAGE_SIZE);
            return CompletionUtils.integerRange(ctx.argument(0), 1, Math.min(pages, 10));
        }
        return List.of();
    }

    private List<String> ipBanCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return onlinePlayerNames(ctx.argument(0));
        }
        if (!ctx.arguments().contains("-s")) {
            return CommandUtils.matching(ctx.argument(ctx.arguments().size() - 1), List.of("-s"));
        }
        return List.of();
    }

    private List<String> ipBanListCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            int pages = Math.max(1, (ipBans().getEntries().size() + BANLIST_PAGE_SIZE - 1) / BANLIST_PAGE_SIZE);
            return CompletionUtils.integerRange(ctx.argument(0), 1, Math.min(pages, 10));
        }
        return List.of();
    }

    private List<String> unbanIpCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), ipBans().getEntries().stream()
                    .map(this::ipBanTargetName)
                    .toList());
        }
        return List.of();
    }

    private List<String> fallDistanceCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return targetCompletions(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("0", "3", "10", "20"));
        }
        return List.of();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && godPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        boolean targetIsPlayer = event.getTarget() instanceof Player;
        boolean protectedTarget = targetIsPlayer && noTargetPlayers.contains(event.getTarget().getUniqueId());
        if (NoTargetPolicy.shouldCancel(targetIsPlayer, protectedTarget)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCuffedMove(PlayerMoveEvent event) {
        if (CuffedActionPolicy.shouldCancel(cuffedPlayers.contains(event.getPlayer().getUniqueId()), CuffedActionPolicy.Action.MOVE)) {
            event.setTo(event.getFrom());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCuffedInteract(PlayerInteractEvent event) {
        cancelCuffed(event.getPlayer(), CuffedActionPolicy.Action.INTERACT, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCuffedBlockBreak(BlockBreakEvent event) {
        cancelCuffed(event.getPlayer(), CuffedActionPolicy.Action.BLOCK_BREAK, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCuffedBlockPlace(BlockPlaceEvent event) {
        cancelCuffed(event.getPlayer(), CuffedActionPolicy.Action.BLOCK_PLACE, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCuffedDrop(PlayerDropItemEvent event) {
        cancelCuffed(event.getPlayer(), CuffedActionPolicy.Action.DROP, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCuffedInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            cancelCuffed(player, CuffedActionPolicy.Action.INVENTORY, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCuffedInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            cancelCuffed(player, CuffedActionPolicy.Action.INVENTORY, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCuffedCommand(PlayerCommandPreprocessEvent event) {
        if (CuffedActionPolicy.shouldCancelCommand(cuffedPlayers.contains(event.getPlayer().getUniqueId()), event.getMessage())) {
            event.setCancelled(true);
            context.message(event.getPlayer(), "moderation.cuff.blocked", Map.of());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clearTemporaryFly(event.getPlayer(), false);
        clearTemporaryGod(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (CuffedActionPolicy.shouldCancel(cuffedPlayers.contains(event.getPlayer().getUniqueId()), CuffedActionPolicy.Action.CHAT)) {
            event.setCancelled(true);
            context.message(event.getPlayer(), "moderation.cuff.blocked", Map.of());
            return;
        }
        Instant now = Instant.now();
        if (muteStore.pruneExpired(now)) {
            saveMutes();
        }
        muteStore.active(event.getPlayer().getUniqueId(), now).ifPresent(mute -> {
            event.setCancelled(true);
            context.message(event.getPlayer(), "moderation.mute.blocked", Map.of(
                    "reason", mute.reason(),
                    "remaining", mute.remaining(now)
            ));
        });
    }

    private boolean fly(CommandSender sender, String[] args) {
        Player target = targetOrSelf(sender, args, 0);
        if (target == null) {
            return true;
        }
        target.setAllowFlight(!target.getAllowFlight());
        context.message(sender, "moderation.fly.updated", statePlaceholders(target, target.getAllowFlight()));
        if (!sender.equals(target)) {
            context.message(target, "moderation.fly.changed", statePlaceholders(target, target.getAllowFlight()));
        }
        return true;
    }

    private boolean warn(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.message(sender, "moderation.warn.usage", Map.of("label", label));
            return true;
        }
        MuteTarget target = muteTarget(args[0]);
        String reason = CommandUtils.joinArgs(args, 1);
        WarningRecord warning = new WarningRecord(target.playerId(), target.name(), sender.getName(), reason, Instant.now());
        warningStore.add(warning);
        saveWarnings();
        int count = warningStore.warnings(target.playerId()).size();
        context.message(sender, "moderation.warn.success", Map.of("target", target.name(), "reason", reason, "count", count));
        target.online().ifPresent(player -> context.message(player, "moderation.warn.notice", Map.of(
                "player", sender.getName(),
                "reason", reason,
                "count", count
        )));
        return true;
    }

    private boolean warnings(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "moderation.warnings.usage", Map.of("label", label));
            return true;
        }
        MuteTarget target = muteTarget(args[0]);
        List<WarningRecord> warnings = warningStore.warnings(target.playerId());
        context.message(sender, warnings.isEmpty() ? "moderation.warnings.empty" : "moderation.warnings.header",
                Map.of("target", target.name(), "count", warnings.size()));
        for (int i = 0; i < warnings.size(); i++) {
            WarningRecord warning = warnings.get(i);
            context.message(sender, "moderation.warnings.entry", Map.of(
                    "index", i + 1,
                    "issuer", warning.issuer(),
                    "reason", warning.reason(),
                    "created_at", warning.createdAt().toString()
            ));
        }
        return true;
    }

    private boolean clearWarnings(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "moderation.clearwarnings.usage", Map.of("label", label));
            return true;
        }
        MuteTarget target = muteTarget(args[0]);
        int removed = warningStore.clear(target.playerId());
        saveWarnings();
        context.message(sender, "moderation.clearwarnings.success", Map.of("target", target.name(), "count", removed));
        return true;
    }

    private boolean editWarnings(CommandSender sender, String label, List<String> args) {
        Optional<ModerationEditWarningsCommandParser.Request> parsed = ModerationEditWarningsCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.editwarnings.usage", Map.of("label", label));
            return true;
        }
        ModerationEditWarningsCommandParser.Request request = parsed.orElseThrow();
        if (request.action() == ModerationEditWarningsCommandParser.Action.CLEAR_ALL) {
            int removed = warningStore.clearAll();
            saveWarnings();
            context.message(sender, "moderation.editwarnings.cleared-all", Map.of("count", removed));
            return true;
        }

        MuteTarget target = muteTarget(request.targetName().orElseThrow());
        int removed = warningStore.clear(target.playerId());
        saveWarnings();
        context.message(sender, "moderation.editwarnings.cleared-player", Map.of(
                "target", target.name(),
                "count", removed
        ));
        return true;
    }

    private boolean god(CommandSender sender, String[] args) {
        Player target = targetOrSelf(sender, args, 0);
        if (target == null) {
            return true;
        }
        boolean enabled = !godPlayers.remove(target.getUniqueId());
        if (enabled) {
            godPlayers.add(target.getUniqueId());
        }
        context.message(sender, "moderation.god.updated", statePlaceholders(target, enabled));
        if (!sender.equals(target)) {
            context.message(target, "moderation.god.changed", statePlaceholders(target, enabled));
        }
        return true;
    }

    private boolean temporaryMode(CommandSender sender, String label, List<String> args, TemporaryMode mode) {
        Optional<TemporaryModeCommandParser.Request> parsed = TemporaryModeCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.temporary.usage", Map.of("label", label));
            return true;
        }
        TemporaryModeCommandParser.Request request = parsed.orElseThrow();
        Player target = CommandUtils.onlinePlayer(request.targetName()).orElse(null);
        if (target == null) {
            context.message(sender, "moderation.player-offline", Map.of("target", request.targetName()));
            return true;
        }
        if (request.durationSeconds().isEmpty()) {
            context.message(sender, "moderation.temporary.status", Map.of(
                    "mode", mode.displayName(),
                    "target", target.getName(),
                    "status", temporaryStatus(target.getUniqueId(), mode)
            ));
            return true;
        }

        String duration = switch (mode) {
            case FLY -> enableTemporaryFly(target, request);
            case GOD -> enableTemporaryGod(target, request);
        };
        if (!request.silent()) {
            Map<String, Object> placeholders = Map.of(
                    "mode", mode.displayName(),
                    "target", target.getName(),
                    "duration", duration
            );
            context.message(sender, "moderation.temporary.enabled", placeholders);
            if (!sender.equals(target)) {
                context.message(target, "moderation.temporary.enabled", placeholders);
            }
        }
        return true;
    }

    private String enableTemporaryFly(Player target, TemporaryModeCommandParser.Request request) {
        UUID playerId = target.getUniqueId();
        temporaryFlyOriginalState.putIfAbsent(playerId, target.getAllowFlight());
        target.setAllowFlight(true);
        return scheduleTemporary(target, request, TemporaryMode.FLY);
    }

    private String enableTemporaryGod(Player target, TemporaryModeCommandParser.Request request) {
        UUID playerId = target.getUniqueId();
        temporaryGodOriginalState.putIfAbsent(playerId, godPlayers.contains(playerId));
        godPlayers.add(playerId);
        return scheduleTemporary(target, request, TemporaryMode.GOD);
    }

    private String scheduleTemporary(Player target, TemporaryModeCommandParser.Request request, TemporaryMode mode) {
        UUID playerId = target.getUniqueId();
        cancelTemporaryTask(playerId, mode);
        if (request.indefinite()) {
            expirations(mode).remove(playerId);
            indefinite(mode).add(playerId);
            return context.messages().template("moderation.temporary.until-relog", "until relog");
        }
        long seconds = request.durationSeconds().orElseThrow();
        Instant now = Instant.now();
        Instant base = request.additive()
                ? expirations(mode).getOrDefault(playerId, now)
                : now;
        if (base.isBefore(now)) {
            base = now;
        }
        Instant expiresAt = base.plusSeconds(seconds);
        indefinite(mode).remove(playerId);
        expirations(mode).put(playerId, expiresAt);
        long delayTicks = Math.max(1L, seconds * 20L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(context.plugin(), () -> expireTemporary(playerId, expiresAt, mode), delayTicks);
        tasks(mode).put(playerId, task);
        return seconds + "s";
    }

    private void expireTemporary(UUID playerId, Instant expiresAt, TemporaryMode mode) {
        Instant current = expirations(mode).get(playerId);
        if (!expiresAt.equals(current)) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            clearTemporaryState(playerId, mode);
            return;
        }
        switch (mode) {
            case FLY -> clearTemporaryFly(player, true);
            case GOD -> clearTemporaryGod(player, true);
        }
    }

    private void clearTemporaryFly(Player player, boolean notify) {
        UUID playerId = player.getUniqueId();
        clearTemporaryState(playerId, TemporaryMode.FLY);
        Boolean original = temporaryFlyOriginalState.remove(playerId);
        if (original != null && !original) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        if (notify) {
            context.message(player, "moderation.temporary.expired", Map.of("mode", TemporaryMode.FLY.displayName()));
        }
    }

    private void clearTemporaryGod(Player player, boolean notify) {
        UUID playerId = player.getUniqueId();
        clearTemporaryState(playerId, TemporaryMode.GOD);
        Boolean original = temporaryGodOriginalState.remove(playerId);
        if (original != null && !original) {
            godPlayers.remove(playerId);
        }
        if (notify) {
            context.message(player, "moderation.temporary.expired", Map.of("mode", TemporaryMode.GOD.displayName()));
        }
    }

    private void clearTemporaryState(UUID playerId, TemporaryMode mode) {
        cancelTemporaryTask(playerId, mode);
        expirations(mode).remove(playerId);
        indefinite(mode).remove(playerId);
    }

    private void cancelTemporaryTask(UUID playerId, TemporaryMode mode) {
        BukkitTask task = tasks(mode).remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private String temporaryStatus(UUID playerId, TemporaryMode mode) {
        if (indefinite(mode).contains(playerId)) {
            return context.messages().template("moderation.temporary.until-relog", "until relog");
        }
        Instant expiresAt = expirations(mode).get(playerId);
        if (expiresAt == null) {
            return context.messages().template("moderation.temporary.inactive", "inactive");
        }
        long remaining = Math.max(0L, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        return remaining + "s remaining";
    }

    private Map<UUID, BukkitTask> tasks(TemporaryMode mode) {
        return mode == TemporaryMode.FLY ? temporaryFlyTasks : temporaryGodTasks;
    }

    private Map<UUID, Instant> expirations(TemporaryMode mode) {
        return mode == TemporaryMode.FLY ? temporaryFlyExpirations : temporaryGodExpirations;
    }

    private Set<UUID> indefinite(TemporaryMode mode) {
        return mode == TemporaryMode.FLY ? temporaryFlyIndefinite : temporaryGodIndefinite;
    }

    private boolean heal(CommandSender sender, String label, List<String> args) {
        Optional<ModerationHealCommandParser.Request> parsed = ModerationHealCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, args.size() > 1 ? "moderation.heal.invalid" : "moderation.heal.usage",
                    Map.of("label", label));
            return true;
        }

        ModerationHealCommandParser.Request request = parsed.get();
        List<Player> targets = healTargets(sender, request.target());
        if (targets.isEmpty()) {
            return true;
        }
        targets.forEach(target -> heal(target, request.amount()));
        if (targets.size() == 1 && request.amount().type() == ModerationHealCommandParser.Type.FULL) {
            context.message(sender, "moderation.heal.done", Map.of("target", targets.getFirst().getName()));
        } else {
            context.message(sender, "moderation.heal.updated", Map.of(
                    "targets", targets.size(),
                    "amount", request.amount().label()
            ));
        }
        return true;
    }

    private List<Player> healTargets(CommandSender sender, ModerationHealCommandParser.Target target) {
        if (target.all()) {
            return List.copyOf(Bukkit.getOnlinePlayers());
        }
        if (target.name().isPresent()) {
            return targets(sender, target.name().orElseThrow(), false);
        }
        if (sender instanceof Player player) {
            return List.of(player);
        }
        context.message(sender, "moderation.console-target-required", Map.of());
        return List.of();
    }

    private void heal(Player target, ModerationHealCommandParser.HealAmount amount) {
        double maxHealth = maxHealth(target);
        double newHealth = switch (amount.type()) {
            case FULL -> maxHealth;
            case ABSOLUTE -> target.getHealth() + amount.value();
            case PERCENT -> target.getHealth() + (maxHealth * (amount.value() / 100.0D));
        };
        target.setHealth(Math.max(0.0D, Math.min(maxHealth, newHealth)));
        target.setFireTicks(0);
    }

    private double maxHealth(Player target) {
        AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 20.0D : Math.max(1.0D, maxHealth.getValue());
    }

    private boolean feed(CommandSender sender, String label, List<String> args) {
        Optional<ModerationFeedCommandParser.Request> parsed = ModerationFeedCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.feed.usage", Map.of("label", label));
            return true;
        }

        ModerationFeedCommandParser.Request request = parsed.get();
        List<Player> targets = feedTargets(sender, request.target());
        if (targets.isEmpty()) {
            return true;
        }
        targets.forEach(this::feed);
        if (request.silent()) {
            return true;
        }
        if (targets.size() == 1) {
            context.message(sender, "moderation.feed.done", Map.of("target", targets.getFirst().getName()));
        } else {
            context.message(sender, "moderation.feed.updated", Map.of("targets", targets.size()));
        }
        return true;
    }

    private List<Player> feedTargets(CommandSender sender, ModerationFeedCommandParser.Target target) {
        if (target.all()) {
            return List.copyOf(Bukkit.getOnlinePlayers());
        }
        if (target.name().isPresent()) {
            return targets(sender, target.name().orElseThrow(), false);
        }
        if (sender instanceof Player player) {
            return List.of(player);
        }
        context.message(sender, "moderation.console-target-required", Map.of());
        return List.of();
    }

    private void feed(Player target) {
        target.setFoodLevel(20);
        target.setSaturation(20.0f);
    }

    private boolean hunger(CommandSender sender, String label, List<String> args) {
        if (args.size() == 2) {
            try {
                int amount = Integer.parseInt(args.get(1));
                if (amount < 0 || amount > 20) {
                    context.message(sender, "moderation.hunger.invalid", Map.of("amount", args.get(1)));
                    return true;
                }
            } catch (NumberFormatException exception) {
                context.message(sender, "moderation.hunger.invalid", Map.of("amount", args.get(1)));
                return true;
            }
        }
        Optional<ModerationHungerCommandParser.Request> parsed = ModerationHungerCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.hunger.usage", Map.of("label", label));
            return true;
        }
        ModerationHungerCommandParser.Request request = parsed.get();
        List<Player> targets = targets(sender, request.target().name(), request.target().all());
        if (targets.isEmpty()) {
            return true;
        }
        targets.forEach(target -> {
            target.setFoodLevel(request.amount());
            target.setSaturation(Math.min(target.getSaturation(), request.amount()));
        });
        context.message(sender, "moderation.hunger.updated", Map.of(
                "targets", targets.size(),
                "amount", request.amount()
        ));
        return true;
    }

    private boolean saturation(CommandSender sender, String label, List<String> args) {
        if (args.size() == 2) {
            try {
                float amount = Float.parseFloat(args.get(1));
                if (!Float.isFinite(amount) || amount < 0.0F || amount > 20.0F) {
                    context.message(sender, "moderation.saturation.invalid", Map.of("amount", args.get(1)));
                    return true;
                }
            } catch (NumberFormatException exception) {
                context.message(sender, "moderation.saturation.invalid", Map.of("amount", args.get(1)));
                return true;
            }
        }
        Optional<ModerationSaturationCommandParser.Request> parsed = ModerationSaturationCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.saturation.usage", Map.of("label", label));
            return true;
        }
        ModerationSaturationCommandParser.Request request = parsed.get();
        List<Player> targets = targets(sender, request.target().name(), request.target().all());
        if (targets.isEmpty()) {
            return true;
        }
        targets.forEach(target -> target.setSaturation(request.amount()));
        context.message(sender, "moderation.saturation.updated", Map.of(
                "targets", targets.size(),
                "amount", String.format(Locale.ROOT, "%.2f", request.amount())
        ));
        return true;
    }

    private boolean maxHealth(CommandSender sender, String label, List<String> args) {
        Optional<ModerationMaxHealthCommandParser.Request> parsed = ModerationMaxHealthCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.maxhp.usage", Map.of("label", label));
            return true;
        }
        ModerationMaxHealthCommandParser.Request request = parsed.get();
        Player target = maxHealthTarget(sender, request);
        if (target == null) {
            return true;
        }
        AttributeInstance attribute = target.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            context.message(sender, "moderation.maxhp.invalid", Map.of());
            return true;
        }

        double current = attribute.getBaseValue();
        double next = switch (request.action()) {
            case SET -> request.amount().orElseThrow();
            case ADD -> current + request.amount().orElseThrow();
            case TAKE -> current - request.amount().orElseThrow();
            case CLEAR -> DEFAULT_MAX_HEALTH;
        };
        if (!Double.isFinite(next) || next < MIN_MAX_HEALTH || next > MAX_MAX_HEALTH) {
            context.message(sender, "moderation.maxhp.invalid", Map.of());
            return true;
        }

        attribute.setBaseValue(next);
        target.setHealth(Math.min(target.getHealth(), next));
        context.message(sender, "moderation.maxhp.updated", Map.of(
                "target", target.getName(),
                "amount", String.format(Locale.ROOT, "%.2f", next)
        ));
        return true;
    }

    private Player maxHealthTarget(CommandSender sender, ModerationMaxHealthCommandParser.Request request) {
        if (request.targetName().isPresent()) {
            return targets(sender, request.targetName().orElseThrow(), false).stream().findFirst().orElse(null);
        }
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "moderation.console-target-required", Map.of());
        return null;
    }

    private boolean scale(CommandSender sender, String label, List<String> args) {
        Optional<ModerationScaleCommandParser.Request> parsed = ModerationScaleCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.scale.usage", Map.of("label", label));
            return true;
        }
        ModerationScaleCommandParser.Request request = parsed.get();
        Player target = scaleTarget(sender, request);
        if (target == null) {
            return true;
        }
        AttributeInstance attribute = target.getAttribute(Attribute.SCALE);
        if (attribute == null) {
            context.message(sender, "moderation.scale.invalid", scaleBounds());
            return true;
        }

        double current = attribute.getBaseValue();
        double next = switch (request.action()) {
            case SET -> request.amount().orElseThrow();
            case ADD -> current + request.amount().orElseThrow();
            case TAKE -> current - request.amount().orElseThrow();
            case CLEAR -> attribute.getDefaultValue();
        };
        if (!Double.isFinite(next) || next < MIN_SCALE || next > MAX_SCALE) {
            context.message(sender, "moderation.scale.invalid", scaleBounds());
            return true;
        }

        attribute.setBaseValue(next);
        if (!request.silent()) {
            Map<String, Object> placeholders = Map.of(
                    "target", target.getName(),
                    "amount", String.format(Locale.ROOT, "%.2f", next)
            );
            context.message(sender, "moderation.scale.updated", placeholders);
            if (!sender.equals(target)) {
                context.message(target, "moderation.scale.updated", placeholders);
            }
        }
        return true;
    }

    private Player scaleTarget(CommandSender sender, ModerationScaleCommandParser.Request request) {
        if (request.targetName().isPresent()) {
            return targets(sender, request.targetName().orElseThrow(), false).stream().findFirst().orElse(null);
        }
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "moderation.console-target-required", Map.of());
        return null;
    }

    private Map<String, Object> scaleBounds() {
        return Map.of(
                "min", String.format(Locale.ROOT, "%.4f", MIN_SCALE),
                "max", String.format(Locale.ROOT, "%.2f", MAX_SCALE)
        );
    }

    private boolean glow(CommandSender sender, String label, List<String> args) {
        Optional<ModerationGlowCommandParser.Request> parsed = ModerationGlowCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.glow.usage", Map.of("label", label));
            return true;
        }
        ModerationGlowCommandParser.Request request = parsed.orElseThrow();
        Player target = glowTarget(sender, request);
        if (target == null) {
            return true;
        }
        if (!senderIsTarget(sender, target) && request.targetName().isPresent()
                && !sender.hasPermission("hydroxide.command.glow.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.glow.others"));
            return true;
        }

        boolean enabled = target.isGlowing();
        String color = "default";
        switch (request.action()) {
            case ENABLE -> {
                enabled = true;
                clearHydroxideGlowTeams(target);
            }
            case DISABLE -> {
                enabled = false;
                clearHydroxideGlowTeams(target);
            }
            case TOGGLE -> {
                enabled = !target.isGlowing();
                if (!enabled) {
                    clearHydroxideGlowTeams(target);
                }
            }
            case COLOR -> {
                NamedTextColor requestedColor = request.color().orElseThrow();
                color = colorName(requestedColor);
                if (!hasGlowColorPermission(sender, color)) {
                    context.message(sender, "moderation.glow.color-denied", Map.of("color", color));
                    return true;
                }
                enabled = true;
                if (!applyGlowTeam(target, requestedColor)) {
                    color = "existing-team";
                }
            }
        }

        target.setGlowing(enabled);
        Map<String, Object> placeholders = Map.of(
                "target", target.getName(),
                "state", enabled ? "enabled" : "disabled",
                "state_color", enabled ? "<green>" : "<red>",
                "color", color
        );
        context.message(sender, "moderation.glow.updated", placeholders);
        if (!sender.equals(target)) {
            context.message(target, "moderation.glow.updated", placeholders);
        }
        return true;
    }

    private Player glowTarget(CommandSender sender, ModerationGlowCommandParser.Request request) {
        if (request.targetName().isPresent()) {
            return targets(sender, request.targetName().orElseThrow(), false).stream().findFirst().orElse(null);
        }
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "moderation.console-target-required", Map.of());
        return null;
    }

    private boolean noTarget(CommandSender sender, String label, List<String> args) {
        Optional<ModerationNoTargetCommandParser.Request> parsed = ModerationNoTargetCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.notarget.usage", Map.of("label", label));
            return true;
        }
        Player target = noTargetTarget(sender, parsed.get());
        if (target == null) {
            return true;
        }

        boolean enabled = switch (parsed.get().state()) {
            case ENABLED -> true;
            case DISABLED -> false;
            case TOGGLE -> !noTargetPlayers.contains(target.getUniqueId());
        };
        if (enabled) {
            noTargetPlayers.add(target.getUniqueId());
        } else {
            noTargetPlayers.remove(target.getUniqueId());
        }

        Map<String, Object> placeholders = Map.of(
                "target", target.getName(),
                "state", enabled ? "enabled" : "disabled"
        );
        context.message(sender, "moderation.notarget.updated", placeholders);
        if (!sender.equals(target)) {
            context.message(target, "moderation.notarget.updated", placeholders);
        }
        return true;
    }

    private Player noTargetTarget(CommandSender sender, ModerationNoTargetCommandParser.Request request) {
        if (request.targetName().isPresent()) {
            return targets(sender, request.targetName().orElseThrow(), false).stream().findFirst().orElse(null);
        }
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "moderation.console-target-required", Map.of());
        return null;
    }

    private boolean playerCollision(CommandSender sender, String label, List<String> args) {
        Optional<ModerationPlayerCollisionCommandParser.Request> parsed = ModerationPlayerCollisionCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.playercollision.usage", Map.of("label", label));
            return true;
        }
        ModerationPlayerCollisionCommandParser.Request request = parsed.orElseThrow();
        Player target;
        if (request.targetName().isPresent()) {
            target = targets(sender, request.targetName().orElseThrow(), false).stream().findFirst().orElse(null);
            if (target == null) {
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            context.message(sender, "moderation.console-target-required", Map.of());
            return true;
        }

        boolean enabled = switch (request.state()) {
            case ENABLED -> true;
            case DISABLED -> false;
            case TOGGLE -> !target.isCollidable();
        };
        target.setCollidable(enabled);
        if (!request.silent()) {
            Map<String, Object> placeholders = Map.of(
                    "target", target.getName(),
                    "state", enabled ? "enabled" : "disabled"
            );
            context.message(sender, "moderation.playercollision.updated", placeholders);
            if (!sender.equals(target)) {
                context.message(target, "moderation.playercollision.updated", placeholders);
            }
        }
        return true;
    }

    private boolean cuff(CommandSender sender, String label, List<String> args) {
        Optional<ModerationCuffCommandParser.Request> parsed = ModerationCuffCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.cuff.usage", Map.of("label", label));
            return true;
        }
        ModerationCuffCommandParser.Request request = parsed.get();
        Player target = targets(sender, request.targetName(), false).stream().findFirst().orElse(null);
        if (target == null) {
            return true;
        }

        boolean enabled = switch (request.state()) {
            case ENABLED -> true;
            case DISABLED -> false;
            case TOGGLE -> !cuffedPlayers.contains(target.getUniqueId());
        };
        if (enabled) {
            cuffedPlayers.add(target.getUniqueId());
        } else {
            cuffedPlayers.remove(target.getUniqueId());
        }

        Map<String, Object> placeholders = Map.of(
                "target", target.getName(),
                "state", enabled ? "enabled" : "disabled"
        );
        if (!request.silent()) {
            context.message(sender, "moderation.cuff.updated", placeholders);
            if (!sender.equals(target)) {
                context.message(target, "moderation.cuff.updated", placeholders);
            }
        }
        return true;
    }

    private boolean speed(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "moderation.speed.usage", Map.of("label", label));
            return true;
        }
        Optional<String> fixedType = ModerationAliasParser.speedTypeFromLabel(label);
        float value;
        try {
            value = Math.max(1, Math.min(10, Float.parseFloat(args[0]))) / 10.0f;
        } catch (NumberFormatException exception) {
            context.message(sender, "moderation.speed.invalid", Map.of("value", args[0]));
            return true;
        }
        int targetIndex = fixedType.isPresent() ? 1 : 2;
        Player target = args.length > targetIndex ? CommandUtils.onlinePlayer(args[targetIndex]).orElse(null) : self(sender);
        if (target == null) {
            if (args.length > targetIndex) {
                context.message(sender, "moderation.player-offline", Map.of("target", args[targetIndex]));
            }
            return true;
        }
        String type = fixedType.orElseGet(() -> args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : (target.isFlying() ? "fly" : "walk"));
        if (type.equals("fly")) {
            target.setFlySpeed(value);
        } else {
            target.setWalkSpeed(value);
        }
        context.message(sender, "moderation.speed.updated", Map.of(
                "type", type,
                "target", target.getName(),
                "speed", args[0]
        ));
        return true;
    }

    private boolean gameMode(CommandSender sender, String label, String[] args) {
        Optional<GameMode> shortcutMode = ModerationAliasParser.gameModeFromLabel(label);
        if (shortcutMode.isPresent()) {
            Player target = targetOrSelf(sender, args, 0);
            if (target == null) {
                return true;
            }
            target.setGameMode(shortcutMode.get());
            context.message(sender, "moderation.gamemode.updated", Map.of(
                    "target", target.getName(),
                    "mode", shortcutMode.get().name().toLowerCase(Locale.ROOT)
            ));
            return true;
        }
        if (args.length == 0) {
            context.message(sender, "moderation.gamemode.usage", Map.of("label", label));
            return true;
        }
        GameMode mode = CommandUtils.gameMode(args[0]).orElse(null);
        if (mode == null) {
            context.message(sender, "moderation.gamemode.unknown", Map.of("mode", args[0]));
            return true;
        }
        Player target = targetOrSelf(sender, args, 1);
        if (target == null) {
            return true;
        }
        target.setGameMode(mode);
        context.message(sender, "moderation.gamemode.updated", Map.of(
                "target", target.getName(),
                "mode", mode.name().toLowerCase(Locale.ROOT)
        ));
        return true;
    }

    private boolean effect(CommandSender sender, String label, List<String> args) {
        Optional<ModerationEffectCommandParser.Request> parsed = ModerationEffectCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.effect.usage", Map.of("label", label));
            return true;
        }
        ModerationEffectCommandParser.Request request = parsed.get();
        List<Player> targets = targets(sender, request.target().name(), request.target().all());
        if (targets.isEmpty()) {
            return true;
        }
        if (request.action() == ModerationEffectCommandParser.Action.CLEAR) {
            int removed = 0;
            for (Player target : targets) {
                List<PotionEffect> activeEffects = List.copyOf(target.getActivePotionEffects());
                removed += activeEffects.size();
                activeEffects.forEach(effect -> target.removePotionEffect(effect.getType()));
            }
            context.message(sender, removed == 0 ? "moderation.effect.none" : "moderation.effect.cleared", Map.of(
                    "count", removed,
                    "targets", targets.size()
            ));
            return true;
        }
        String requestedEffect = request.effect().orElse("");
        PotionEffectType effectType = potionEffect(requestedEffect);
        if (effectType == null) {
            context.message(sender, "moderation.effect.unknown", Map.of("effect", requestedEffect));
            return true;
        }
        int ticks = Math.toIntExact(request.duration().toSeconds() * 20L);
        PotionEffect potionEffect = new PotionEffect(effectType, ticks, request.amplifier(), false, request.particles(), true);
        targets.forEach(target -> target.addPotionEffect(potionEffect));
        context.message(sender, "moderation.effect.applied", Map.of(
                "effect", key(effectType),
                "targets", targets.size(),
                "duration", request.duration().toSeconds(),
                "amplifier", request.amplifier()
        ));
        return true;
    }

    private boolean air(CommandSender sender, String label, List<String> args) {
        Optional<ModerationAirCommandParser.Request> parsed = ModerationAirCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.air.usage", Map.of("label", label));
            return true;
        }
        ModerationAirCommandParser.Request request = parsed.get();
        List<Player> targets = targets(sender, request.target().name(), request.target().all());
        if (targets.isEmpty()) {
            return true;
        }
        for (Player target : targets) {
            int amount = request.useMaximum()
                    ? target.getMaximumAir()
                    : Math.min(request.amountTicks().orElse(0), target.getMaximumAir());
            target.setRemainingAir(amount);
        }
        context.message(sender, "moderation.air.updated", Map.of(
                "targets", targets.size(),
                "amount", request.useMaximum() ? "max" : request.amountTicks().orElse(0)
        ));
        return true;
    }

    private boolean fallDistance(CommandSender sender, String label, List<String> args) {
        if (args.size() == 2) {
            try {
                float distance = Float.parseFloat(args.get(1));
                if (!Float.isFinite(distance) || distance < 0.0F || distance > 10_000.0F) {
                    context.message(sender, "moderation.falldistance.invalid", Map.of("distance", args.get(1)));
                    return true;
                }
            } catch (NumberFormatException exception) {
                context.message(sender, "moderation.falldistance.invalid", Map.of("distance", args.get(1)));
                return true;
            }
        }
        Optional<ModerationFallDistanceCommandParser.Request> parsed = ModerationFallDistanceCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.falldistance.usage", Map.of("label", label));
            return true;
        }
        ModerationFallDistanceCommandParser.Request request = parsed.get();
        List<Player> targets = targets(sender, request.target().name(), request.target().all());
        if (targets.isEmpty()) {
            return true;
        }
        targets.forEach(target -> target.setFallDistance(request.distance()));
        context.message(sender, "moderation.falldistance.updated", Map.of(
                "targets", targets.size(),
                "distance", String.format(Locale.ROOT, "%.2f", request.distance())
        ));
        return true;
    }

    private boolean kick(CommandSender sender, String label, List<String> args) {
        Optional<ModerationKickCommandParser.Request> parsed = ModerationKickCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.kick.usage", Map.of("label", label));
            return true;
        }

        ModerationKickCommandParser.Request request = parsed.get();
        List<Player> targets = targets(sender, request.target().name(), request.target().all());
        if (targets.isEmpty()) {
            return true;
        }
        KickTargetPolicy.Selection<Player> kickTargets = KickTargetPolicy.selectKickable(
                targets,
                target -> target.hasPermission(KickTargetPolicy.BYPASS_PERMISSION)
        );
        if (!kickTargets.bypassed().isEmpty() && !request.silent()) {
            context.message(sender, "moderation.kick.bypassed", Map.of(
                    "targets", String.join(", ", kickTargets.bypassed().stream().map(Player::getName).toList())
            ));
        }
        targets = kickTargets.kickable();
        if (targets.isEmpty()) {
            return true;
        }

        String reason = request.reason().orElseGet(() ->
                context.messages().template("moderation.kick.default-reason", "Kicked from server"));
        for (Player target : targets) {
            target.kick(context.messages().component("moderation.kick.target", Map.of(
                    "player", sender.getName(),
                    "reason", reason
            )));
        }
        if (request.silent()) {
            return true;
        }
        if (request.target().all()) {
            context.message(sender, "moderation.kick.success-all", Map.of(
                    "targets", targets.size(),
                    "reason", reason
            ));
            return true;
        }
        context.message(sender, "moderation.kick.success", Map.of(
                "target", targets.getFirst().getName(),
                "reason", reason
        ));
        return true;
    }

    private boolean kickAll(CommandSender sender, String label, List<String> args) {
        List<String> routed = new ArrayList<>();
        routed.add("all");
        routed.addAll(args);
        return kick(sender, label, routed);
    }

    private boolean ban(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "moderation.ban.usage", Map.of("label", label));
            return true;
        }
        String reason = args.length > 1
                ? CommandUtils.joinArgs(args, 1)
                : context.messages().template("moderation.ban.default-reason", "Banned from server.");
        PlayerProfile profile = profile(args[0]);
        bans().addBan(profile, reason, (java.time.Instant) null, sender.getName());
        kickIfOnline(args[0], "moderation.ban.target", sender.getName(), reason);
        context.message(sender, "moderation.ban.success", Map.of("target", args[0], "reason", reason));
        return true;
    }

    private boolean banList(CommandSender sender, String label, List<String> args) {
        Optional<ModerationBanListCommandParser.Request> parsed = ModerationBanListCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.banlist.usage", Map.of("label", label));
            return true;
        }
        List<BanEntry<? super PlayerProfile>> entries = new ArrayList<>(bans().getEntries());
        entries.sort(Comparator.comparing(entry -> banTargetName(entry).toLowerCase(Locale.ROOT)));
        if (entries.isEmpty()) {
            context.message(sender, "moderation.banlist.empty", Map.of());
            return true;
        }

        int pages = Math.max(1, (entries.size() + BANLIST_PAGE_SIZE - 1) / BANLIST_PAGE_SIZE);
        int page = Math.min(parsed.get().page(), pages);
        int from = (page - 1) * BANLIST_PAGE_SIZE;
        int to = Math.min(entries.size(), from + BANLIST_PAGE_SIZE);

        context.message(sender, "moderation.banlist.header", Map.of(
                "page", page,
                "pages", pages,
                "count", entries.size()
        ));
        for (int index = from; index < to; index++) {
            BanEntry<? super PlayerProfile> entry = entries.get(index);
            Map<String, Object> placeholders = new java.util.HashMap<>(banPlaceholders(banTargetName(entry), entry));
            placeholders.put("index", index + 1);
            context.message(sender, "moderation.banlist.entry", placeholders);
        }
        return true;
    }

    private boolean checkBan(CommandSender sender, String label, String[] args) {
        if (args.length != 1) {
            context.message(sender, "moderation.checkban.usage", Map.of("label", label));
            return true;
        }
        BanEntry<? super PlayerProfile> entry = bans().getBanEntry(profile(args[0]));
        if (entry == null) {
            context.message(sender, "moderation.checkban.not-banned", Map.of("target", args[0]));
            return true;
        }
        context.message(sender, "moderation.checkban.banned", banPlaceholders(args[0], entry));
        return true;
    }

    private boolean tempBan(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.message(sender, "moderation.tempban.usage", Map.of("label", label));
            return true;
        }
        Optional<java.time.Duration> duration = ModerationDurationParser.parse(args[1]);
        if (duration.isEmpty()) {
            context.message(sender, "moderation.tempban.invalid-duration", Map.of("duration", args[1]));
            return true;
        }
        String reason = args.length > 2
                ? CommandUtils.joinArgs(args, 2)
                : context.messages().template("moderation.tempban.default-reason", "Temporarily banned from server.");
        PlayerProfile profile = profile(args[0]);
        bans().addBan(profile, reason, duration.get(), sender.getName());
        kickIfOnline(args[0], "moderation.tempban.target", sender.getName(), reason);
        context.message(sender, "moderation.tempban.success", Map.of(
                "target", args[0],
                "duration", args[1],
                "reason", reason
        ));
        return true;
    }

    private boolean unban(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "moderation.unban.usage", Map.of("label", label));
            return true;
        }
        bans().pardon(profile(args[0]));
        context.message(sender, "moderation.unban.success", Map.of("target", args[0]));
        return true;
    }

    private boolean ipBan(CommandSender sender, String label, List<String> args) {
        Optional<ModerationIpBanCommandParser.Request> parsed = ModerationIpBanCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.ipban.usage", Map.of("label", label));
            return true;
        }
        ModerationIpBanCommandParser.Request request = parsed.get();
        Optional<IpBanTarget> resolved = ipBanTarget(request.target());
        if (resolved.isEmpty()) {
            context.message(sender, "moderation.ipban.invalid-target", Map.of("target", request.target()));
            return true;
        }

        IpBanTarget target = resolved.get();
        if (target.online().filter(player -> player.hasPermission("hydroxide.command.ipban.bypass")).isPresent()) {
            if (!request.silent()) {
                context.message(sender, "moderation.ipban.bypassed", Map.of("target", target.label()));
            }
            return true;
        }

        String reason = request.reason().orElseGet(() ->
                context.messages().template("moderation.ipban.default-reason", "IP banned from server."));
        ipBans().addBan(target.address(), reason, (Instant) null, sender.getName());
        if (target.banProfile()) {
            bans().addBan(profile(target.label()), reason, (Instant) null, sender.getName());
        }
        kickOnlineByAddress(target.address(), sender.getName(), reason);
        if (!request.silent()) {
            context.message(sender, "moderation.ipban.success", Map.of(
                    "target", target.label(),
                    "ip", target.address().getHostAddress(),
                    "reason", reason
            ));
        }
        return true;
    }

    private boolean ipBanList(CommandSender sender, String label, List<String> args) {
        Optional<ModerationBanListCommandParser.Request> parsed = ModerationBanListCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "moderation.ipbanlist.usage", Map.of("label", label));
            return true;
        }
        List<BanEntry<? super InetAddress>> entries = new ArrayList<>(ipBans().getEntries());
        entries.sort(Comparator.comparing(entry -> ipBanTargetName(entry).toLowerCase(Locale.ROOT)));
        if (entries.isEmpty()) {
            context.message(sender, "moderation.ipbanlist.empty", Map.of());
            return true;
        }

        int pages = Math.max(1, (entries.size() + BANLIST_PAGE_SIZE - 1) / BANLIST_PAGE_SIZE);
        int page = Math.min(parsed.get().page(), pages);
        int from = (page - 1) * BANLIST_PAGE_SIZE;
        int to = Math.min(entries.size(), from + BANLIST_PAGE_SIZE);

        context.message(sender, "moderation.ipbanlist.header", Map.of(
                "page", page,
                "pages", pages,
                "count", entries.size()
        ));
        for (int index = from; index < to; index++) {
            BanEntry<? super InetAddress> entry = entries.get(index);
            Map<String, Object> placeholders = new java.util.HashMap<>(banPlaceholders(ipBanTargetName(entry), entry));
            placeholders.put("index", index + 1);
            context.message(sender, "moderation.ipbanlist.entry", placeholders);
        }
        return true;
    }

    private boolean unbanIp(CommandSender sender, String label, List<String> args) {
        if (args.size() != 1) {
            context.message(sender, "moderation.unbanip.usage", Map.of("label", label));
            return true;
        }
        Optional<InetAddress> address = parseIpLiteral(args.getFirst());
        if (address.isEmpty()) {
            context.message(sender, "moderation.ipban.invalid-target", Map.of("target", args.getFirst()));
            return true;
        }
        ipBans().pardon(address.get());
        context.message(sender, "moderation.unbanip.success", Map.of("target", address.get().getHostAddress()));
        return true;
    }

    private boolean mute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "moderation.mute.usage", Map.of("label", label));
            return true;
        }
        MuteTarget target = muteTarget(args[0]);
        String reason = args.length > 1
                ? CommandUtils.joinArgs(args, 1)
                : context.messages().template("moderation.mute.default-reason", "Muted.");
        muteStore.mute(new MuteRecord(target.playerId(), target.name(), sender.getName(), reason, Instant.now(), null));
        saveMutes();
        context.message(sender, "moderation.mute.success", Map.of("target", target.name(), "reason", reason));
        target.online().ifPresent(player -> context.message(player, "moderation.mute.notice", Map.of("player", sender.getName(), "reason", reason)));
        return true;
    }

    private boolean tempMute(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.message(sender, "moderation.tempmute.usage", Map.of("label", label));
            return true;
        }
        MuteTarget target = muteTarget(args[0]);
        Optional<java.time.Duration> duration = ModerationDurationParser.parse(args[1]);
        if (duration.isEmpty()) {
            context.message(sender, "moderation.tempmute.invalid-duration", Map.of("duration", args[1]));
            return true;
        }
        String reason = args.length > 2
                ? CommandUtils.joinArgs(args, 2)
                : context.messages().template("moderation.tempmute.default-reason", "Temporarily muted.");
        Instant now = Instant.now();
        muteStore.mute(new MuteRecord(target.playerId(), target.name(), sender.getName(), reason, now, now.plus(duration.get())));
        saveMutes();
        context.message(sender, "moderation.tempmute.success", Map.of(
                "target", target.name(),
                "duration", args[1],
                "reason", reason
        ));
        target.online().ifPresent(player -> context.message(player, "moderation.tempmute.notice", Map.of(
                "player", sender.getName(),
                "duration", args[1],
                "reason", reason
        )));
        return true;
    }

    private boolean unmute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "moderation.unmute.usage", Map.of("label", label));
            return true;
        }
        MuteTarget target = muteTarget(args[0]);
        boolean removed = muteStore.unmute(target.playerId());
        saveMutes();
        context.message(sender, removed ? "moderation.unmute.success" : "moderation.unmute.not-muted",
                Map.of("target", target.name()));
        if (removed) {
            target.online().ifPresent(player -> context.message(player, "moderation.unmute.notice", Map.of("player", sender.getName())));
        }
        return true;
    }

    private ProfileBanList bans() {
        return Bukkit.getBanList(BanListType.PROFILE);
    }

    private IpBanList ipBans() {
        return Bukkit.getBanList(BanListType.IP);
    }

    private PlayerProfile profile(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online == null ? Bukkit.createProfile(name) : online.getPlayerProfile();
    }

    private void kickIfOnline(String targetName, String key, String actor, String reason) {
        Player online = CommandUtils.onlinePlayer(targetName).orElse(null);
        if (online != null) {
            online.kick(context.messages().component(key, Map.of("player", actor, "reason", reason)));
        }
    }

    private void kickOnlineByAddress(InetAddress address, String actor, String reason) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InetSocketAddress playerAddress = player.getAddress();
            if (playerAddress != null && address.equals(playerAddress.getAddress())) {
                player.kick(context.messages().component("moderation.ipban.target", Map.of(
                        "player", actor,
                        "reason", reason
                )));
            }
        }
    }

    private void saveMutes() {
        YamlConfiguration yaml = new YamlConfiguration();
        muteStore.writeTo(yaml);
        muteYaml.save(yaml);
    }

    private void saveWarnings() {
        YamlConfiguration yaml = new YamlConfiguration();
        warningStore.writeTo(yaml);
        warningYaml.save(yaml);
    }

    private MuteTarget muteTarget(String name) {
        Player online = CommandUtils.onlinePlayer(name).orElse(null);
        if (online != null) {
            return new MuteTarget(online.getUniqueId(), online.getName(), Optional.of(online));
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return new MuteTarget(offline.getUniqueId(), offline.getName() == null ? name : offline.getName(), Optional.empty());
    }

    private Player targetOrSelf(CommandSender sender, String[] args, int targetIndex) {
        if (args.length > targetIndex) {
            Player target = CommandUtils.onlinePlayer(args[targetIndex]).orElse(null);
            if (target == null) {
                context.message(sender, "moderation.player-offline", Map.of("target", args[targetIndex]));
            }
            return target;
        }
        return self(sender);
    }

    private Player self(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "moderation.console-target-required", Map.of());
        return null;
    }

    private List<String> onlinePlayerNames(String prefix) {
        return CommandUtils.matching(prefix, Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
    }

    private List<String> bannedTargetNames(String prefix) {
        return CommandUtils.matching(prefix, bans().getEntries().stream()
                .map(this::banTargetName)
                .toList());
    }

    private List<String> targetCompletions(String prefix) {
        List<String> values = new ArrayList<>(List.of("all"));
        values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        return CommandUtils.matching(prefix, values);
    }

    private List<Player> targets(CommandSender sender, String targetName, boolean all) {
        if (all) {
            return List.copyOf(Bukkit.getOnlinePlayers());
        }
        Player target = CommandUtils.onlinePlayer(targetName).orElse(null);
        if (target == null) {
            context.message(sender, "moderation.player-offline", Map.of("target", targetName));
            return List.of();
        }
        return List.of(target);
    }

    private boolean senderIsTarget(CommandSender sender, Player target) {
        return sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
    }

    private boolean hasGlowColorPermission(CommandSender sender, String colorName) {
        return sender.hasPermission("hydroxide.command.glow.color.*")
                || sender.hasPermission("hydroxide.command.glow.color." + colorName);
    }

    @SuppressWarnings("deprecation")
    private boolean applyGlowTeam(Player target, NamedTextColor color) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return false;
        }
        Scoreboard scoreboard = manager.getMainScoreboard();
        Team existing = scoreboard.getEntityTeam(target);
        if (existing != null && !existing.getName().startsWith(GLOW_TEAM_PREFIX)) {
            return false;
        }
        clearHydroxideGlowTeams(scoreboard, target);
        String teamName = GLOW_TEAM_PREFIX + colorName(color);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.setColor(toBukkitColor(color));
        team.addEntity(target);
        return true;
    }

    private void clearHydroxideGlowTeams(Player target) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            clearHydroxideGlowTeams(manager.getMainScoreboard(), target);
        }
    }

    private void clearHydroxideGlowTeams(Scoreboard scoreboard, Player target) {
        for (Team team : List.copyOf(scoreboard.getTeams())) {
            if (team.getName().startsWith(GLOW_TEAM_PREFIX) && team.hasEntity(target)) {
                team.removeEntity(target);
                if (team.getSize() == 0) {
                    team.unregister();
                }
            }
        }
    }

    private String colorName(NamedTextColor color) {
        if (color == NamedTextColor.BLACK) {
            return "black";
        }
        if (color == NamedTextColor.DARK_BLUE) {
            return "dark_blue";
        }
        if (color == NamedTextColor.DARK_GREEN) {
            return "dark_green";
        }
        if (color == NamedTextColor.DARK_AQUA) {
            return "dark_aqua";
        }
        if (color == NamedTextColor.DARK_RED) {
            return "dark_red";
        }
        if (color == NamedTextColor.DARK_PURPLE) {
            return "dark_purple";
        }
        if (color == NamedTextColor.GOLD) {
            return "gold";
        }
        if (color == NamedTextColor.GRAY) {
            return "gray";
        }
        if (color == NamedTextColor.DARK_GRAY) {
            return "dark_gray";
        }
        if (color == NamedTextColor.BLUE) {
            return "blue";
        }
        if (color == NamedTextColor.GREEN) {
            return "green";
        }
        if (color == NamedTextColor.AQUA) {
            return "aqua";
        }
        if (color == NamedTextColor.RED) {
            return "red";
        }
        if (color == NamedTextColor.LIGHT_PURPLE) {
            return "light_purple";
        }
        if (color == NamedTextColor.YELLOW) {
            return "yellow";
        }
        return "white";
    }

    @SuppressWarnings("deprecation")
    private org.bukkit.ChatColor toBukkitColor(NamedTextColor color) {
        return switch (colorName(color)) {
            case "black" -> org.bukkit.ChatColor.BLACK;
            case "dark_blue" -> org.bukkit.ChatColor.DARK_BLUE;
            case "dark_green" -> org.bukkit.ChatColor.DARK_GREEN;
            case "dark_aqua" -> org.bukkit.ChatColor.DARK_AQUA;
            case "dark_red" -> org.bukkit.ChatColor.DARK_RED;
            case "dark_purple" -> org.bukkit.ChatColor.DARK_PURPLE;
            case "gold" -> org.bukkit.ChatColor.GOLD;
            case "gray" -> org.bukkit.ChatColor.GRAY;
            case "dark_gray" -> org.bukkit.ChatColor.DARK_GRAY;
            case "blue" -> org.bukkit.ChatColor.BLUE;
            case "green" -> org.bukkit.ChatColor.GREEN;
            case "aqua" -> org.bukkit.ChatColor.AQUA;
            case "red" -> org.bukkit.ChatColor.RED;
            case "light_purple" -> org.bukkit.ChatColor.LIGHT_PURPLE;
            case "yellow" -> org.bukkit.ChatColor.YELLOW;
            default -> org.bukkit.ChatColor.WHITE;
        };
    }

    private void cancelCuffed(Player player, CuffedActionPolicy.Action action, org.bukkit.event.Cancellable event) {
        if (CuffedActionPolicy.shouldCancel(cuffedPlayers.contains(player.getUniqueId()), action)) {
            event.setCancelled(true);
            context.message(player, "moderation.cuff.blocked", Map.of());
        }
    }

    private PotionEffectType potionEffect(String input) {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.MOB_EFFECT)
                .get(NamespacedKey.minecraft(ModernRegistryKeys.minecraftKey(input)));
    }

    private List<String> effectKeys() {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.MOB_EFFECT)
                .keyStream()
                .map(NamespacedKey::getKey)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private String key(PotionEffectType effectType) {
        NamespacedKey key = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.MOB_EFFECT)
                .getKey(effectType);
        return key == null ? effectType.toString().toLowerCase(Locale.ROOT) : key.getKey();
    }

    private Map<String, Object> banPlaceholders(String target, BanEntry<?> entry) {
        return Map.of(
                "target", target,
                "reason", valueOrMessage(entry.getReason(), "moderation.banlist.no-reason", "No reason provided"),
                "source", valueOrMessage(entry.getSource(), "moderation.banlist.unknown-source", "unknown"),
                "created", dateOrMessage(entry.getCreated()),
                "expires", expiresOrMessage(entry.getExpiration())
        );
    }

    private String banTargetName(BanEntry<?> entry) {
        Object target = entry.getBanTarget();
        if (target instanceof PlayerProfile profile && profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName();
        }
        return target == null ? context.messages().template("moderation.banlist.unknown-target", "unknown") : String.valueOf(target);
    }

    private String ipBanTargetName(BanEntry<?> entry) {
        Object target = entry.getBanTarget();
        if (target instanceof InetAddress address) {
            return address.getHostAddress();
        }
        return target == null ? context.messages().template("moderation.banlist.unknown-target", "unknown") : String.valueOf(target);
    }

    private Optional<IpBanTarget> ipBanTarget(String input) {
        Optional<InetAddress> address = parseIpLiteral(input);
        if (address.isPresent()) {
            return Optional.of(new IpBanTarget(address.get().getHostAddress(), address.get(), Optional.empty(), false));
        }
        Player online = CommandUtils.onlinePlayer(input).orElse(null);
        if (online == null || online.getAddress() == null || online.getAddress().getAddress() == null) {
            return Optional.empty();
        }
        return Optional.of(new IpBanTarget(online.getName(), online.getAddress().getAddress(), Optional.of(online),
                IpBanTargetPolicy.shouldBanProfile(input)));
    }

    private Optional<InetAddress> parseIpLiteral(String input) {
        if (!IpBanTargetPolicy.looksLikeIpLiteral(input)) {
            return Optional.empty();
        }
        try {
            return Optional.of(InetAddress.getByName(input));
        } catch (UnknownHostException exception) {
            return Optional.empty();
        }
    }

    private String valueOrMessage(String value, String key, String fallback) {
        return value == null || value.isBlank() ? context.messages().template(key, fallback) : value;
    }

    private String dateOrMessage(Date date) {
        return date == null ? context.messages().template("moderation.banlist.unknown-date", "unknown") : date.toInstant().toString();
    }

    private String expiresOrMessage(Date expiration) {
        return expiration == null ? context.messages().template("moderation.banlist.never", "never") : expiration.toInstant().toString();
    }

    private Map<String, ?> statePlaceholders(Player target, boolean enabled) {
        return Map.of(
                "target", target.getName(),
                "state", enabled ? "enabled" : "disabled",
                "state_color", enabled ? "<green>" : "<red>"
        );
    }

    private record MuteTarget(UUID playerId, String name, Optional<Player> online) {
    }

    private record IpBanTarget(String label, InetAddress address, Optional<Player> online, boolean banProfile) {
    }

    private enum TemporaryMode {
        FLY("Temporary flight"),
        GOD("Temporary god mode");

        private final String displayName;

        TemporaryMode(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }
    }
}
