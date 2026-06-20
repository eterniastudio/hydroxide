package net.axther.hydroxide.modules.announcement;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AnnouncementModule implements HydroModule {

    private HydroxideContext context;
    private BukkitTask task;
    private int cursor;
    private final List<BukkitTask> transientTasks = new ArrayList<>();
    private final List<ShownBossBar> transientBossBars = new ArrayList<>();

    @Override
    public String id() {
        return "announcements";
    }

    @Override
    public String displayName() {
        return "Announcements";
    }

    @Override
    public String description() {
        return "Schedules chat, action bar, title, and bossbar announcement campaigns.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        context.commands().register("actionbarmsg", command("actionbarmsg", "hydroxide.command.actionbarmsg", "/{label} <player|all> [-s:seconds] <message>",
                ctx -> actionbar(ctx.sender(), ctx.label(), ctx.arguments())));
        context.commands().register("bossbarmsg", command("bossbarmsg", "hydroxide.command.bossbarmsg", "/{label} <player|all> [-sec:seconds] <message>",
                ctx -> bossbar(ctx.sender(), ctx.label(), ctx.arguments())));
        context.commands().register("titlemsg", command("titlemsg", "hydroxide.command.titlemsg", "/{label} <player|all> [-in:ticks] [-keep:ticks] [-out:ticks] <title> [\\n subtitle]",
                ctx -> title(ctx.sender(), ctx.label(), ctx.arguments())));
        context.commands().register("ctellraw", command("ctellraw", "hydroxide.command.ctellraw", "/{label} <player|all> <message>",
                ctx -> tellRaw(ctx.sender(), ctx.label(), ctx.arguments())));

        List<Campaign> campaigns = loadCampaigns();
        if (campaigns.isEmpty()) {
            return;
        }
        long interval = Math.max(20L, context.plugin().getConfig().getLong("announcements.interval-seconds", 180L) * 20L);
        task = Bukkit.getScheduler().runTaskTimer(context.plugin(), () -> {
            Campaign campaign = campaigns.get(cursor++ % campaigns.size());
            broadcast(campaign);
        }, interval, interval);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        if (task != null) {
            task.cancel();
        }
        transientTasks.forEach(BukkitTask::cancel);
        transientTasks.clear();
        transientBossBars.forEach(shown -> shown.player().hideBossBar(shown.bar()));
        transientBossBars.clear();
    }

    private CommandService command(String name, String permission, String usage, HydroCommand.HydroCommandExecutor executor) {
        return new CommandService(HydroCommand.builder(name)
                .permission(permission)
                .usage(usage)
                .executor(executor)
                .completer(this::targetCompletions)
                .build(), context.messages());
    }

    private List<String> targetCompletions(CommandContext ctx) {
        if (ctx.arguments().size() != 1) {
            return List.of();
        }
        List<String> options = new ArrayList<>(List.of("all", "*"));
        options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        return CommandUtils.matching(ctx.argument(0), options);
    }

    private List<Campaign> loadCampaigns() {
        YamlStore store = new YamlStore(new File(context.plugin().getDataFolder(), "announcements.yml"));
        YamlConfiguration yaml = store.load();
        if (!yaml.contains("campaigns")) {
            yaml.set("campaigns.welcome.type", "chat");
            yaml.set("campaigns.welcome.message", "<#44CCFF>Tip: <gray>Use <white>/homesgui <gray>to navigate homes.");
            store.save(yaml);
        }
        ConfigurationSection section = yaml.getConfigurationSection("campaigns");
        if (section == null) {
            return List.of();
        }
        List<Campaign> campaigns = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection node = section.getConfigurationSection(key);
            if (node == null) {
                continue;
            }
            campaigns.add(new Campaign(
                    node.getString("type", "chat").toLowerCase(Locale.ROOT),
                    node.getString("message", ""),
                    node.getString("subtitle", ""),
                    node.getString("permission", ""),
                    node.getString("world", ""),
                    node.getLong("duration-seconds", 6L)
            ));
        }
        return campaigns;
    }

    private void broadcast(Campaign campaign) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!campaign.permission().isBlank() && !player.hasPermission(campaign.permission())) {
                continue;
            }
            if (!campaign.world().isBlank() && !player.getWorld().getName().equalsIgnoreCase(campaign.world())) {
                continue;
            }
            switch (campaign.type()) {
                case "actionbar" -> player.sendActionBar(context.text().format(campaign.message()));
                case "title" -> player.showTitle(Title.title(
                        context.text().format(campaign.message()),
                        context.text().format(campaign.subtitle()),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                ));
                case "bossbar" -> showBossBar(player, campaign);
                default -> player.sendMessage(context.text().format(campaign.message()));
            }
        }
    }

    private void actionbar(CommandSender sender, String label, List<String> args) {
        Optional<AnnouncementCommandParser.TimedMessage> parsed = AnnouncementCommandParser.actionbar(args);
        if (parsed.isEmpty()) {
            context.message(sender, "announcements.actionbar.usage", Map.of("label", label));
            return;
        }
        AnnouncementCommandParser.TimedMessage request = parsed.get();
        List<Player> targets = targets(sender, request.target());
        if (targets.isEmpty()) {
            return;
        }
        showActionBar(targets, context.text().format(request.message()), request.duration());
        context.message(sender, "announcements.sent", Map.of("type", "actionbar", "count", targets.size()));
    }

    private void bossbar(CommandSender sender, String label, List<String> args) {
        Optional<AnnouncementCommandParser.TimedMessage> parsed = AnnouncementCommandParser.bossbar(args);
        if (parsed.isEmpty()) {
            context.message(sender, "announcements.bossbar.usage", Map.of("label", label));
            return;
        }
        AnnouncementCommandParser.TimedMessage request = parsed.get();
        List<Player> targets = targets(sender, request.target());
        if (targets.isEmpty()) {
            return;
        }
        targets.forEach(player -> showBossBar(player, request.message(), request.duration()));
        context.message(sender, "announcements.sent", Map.of("type", "bossbar", "count", targets.size()));
    }

    private void title(CommandSender sender, String label, List<String> args) {
        Optional<AnnouncementCommandParser.TitleMessage> parsed = AnnouncementCommandParser.title(args);
        if (parsed.isEmpty()) {
            context.message(sender, "announcements.title.usage", Map.of("label", label));
            return;
        }
        AnnouncementCommandParser.TitleMessage request = parsed.get();
        List<Player> targets = targets(sender, request.target());
        if (targets.isEmpty()) {
            return;
        }
        Title title = Title.title(
                context.text().format(request.title()),
                context.text().format(request.subtitle()),
                Title.Times.times(ticks(request.fadeInTicks()), ticks(request.stayTicks()), ticks(request.fadeOutTicks()))
        );
        targets.forEach(player -> player.showTitle(title));
        context.message(sender, "announcements.sent", Map.of("type", "title", "count", targets.size()));
    }

    private void tellRaw(CommandSender sender, String label, List<String> args) {
        Optional<AnnouncementCommandParser.FormattedMessage> parsed = AnnouncementCommandParser.tellRaw(args);
        if (parsed.isEmpty()) {
            context.message(sender, "announcements.ctellraw.usage", Map.of("label", label));
            return;
        }
        AnnouncementCommandParser.FormattedMessage request = parsed.get();
        List<Player> targets = targets(sender, request.target());
        if (targets.isEmpty()) {
            return;
        }
        Component message = context.text().format(request.message());
        targets.forEach(player -> player.sendMessage(message));
        context.message(sender, "announcements.ctellraw.sent", Map.of("target", request.target().name(), "count", targets.size()));
    }

    private List<Player> targets(CommandSender sender, AnnouncementCommandParser.Target target) {
        if (target.all()) {
            List<Player> players = List.copyOf(Bukkit.getOnlinePlayers());
            if (players.isEmpty()) {
                context.message(sender, "announcements.none-online", Map.of());
            }
            return players;
        }
        Player exact = Bukkit.getPlayerExact(target.name());
        Player player = exact == null
                ? Bukkit.getOnlinePlayers().stream()
                        .filter(candidate -> candidate.getName().equalsIgnoreCase(target.name()))
                        .findFirst()
                        .orElse(null)
                : exact;
        if (player == null) {
            context.message(sender, "announcements.player-offline", Map.of("target", target.name()));
            return List.of();
        }
        return List.of(player);
    }

    private void showActionBar(Collection<Player> players, Component message, Duration duration) {
        players.forEach(player -> player.sendActionBar(message));
        long ticks = Math.max(20L, duration.toSeconds() * 20L);
        if (ticks <= 20L) {
            return;
        }
        BukkitTask repeating = Bukkit.getScheduler().runTaskTimer(context.plugin(), () ->
                players.forEach(player -> player.sendActionBar(message)), 20L, 20L);
        transientTasks.add(repeating);
        BukkitTask cleanup = Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
            repeating.cancel();
            transientTasks.remove(repeating);
            transientTasks.removeIf(BukkitTask::isCancelled);
        }, ticks);
        transientTasks.add(cleanup);
    }

    private void showBossBar(Player player, Campaign campaign) {
        showBossBar(player, campaign.message(), Duration.ofSeconds(Math.max(1L, campaign.durationSeconds())));
    }

    private void showBossBar(Player player, String message, Duration duration) {
        BossBar bar = BossBar.bossBar(
                context.text().format(message),
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );
        player.showBossBar(bar);
        ShownBossBar shown = new ShownBossBar(player, bar);
        transientBossBars.add(shown);
        int ticks = (int) Math.max(20L, duration.toSeconds() * 20L);
        BukkitTask progress = Bukkit.getScheduler().runTaskTimer(context.plugin(), new Runnable() {
            private int elapsed;

            @Override
            public void run() {
                elapsed += 10;
                bar.progress(Math.max(0.0f, 1.0f - (elapsed / (float) ticks)));
            }
        }, 10L, 10L);
        transientTasks.add(progress);
        BukkitTask cleanup = Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
            progress.cancel();
            transientTasks.remove(progress);
            player.hideBossBar(bar);
            transientBossBars.remove(shown);
        }, ticks);
        transientTasks.add(cleanup);
    }

    private Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(1L, ticks) * 50L);
    }

    private record Campaign(String type, String message, String subtitle, String permission, String world, long durationSeconds) {
    }

    private record ShownBossBar(Player player, BossBar bar) {
    }
}
