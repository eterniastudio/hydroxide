package net.axther.hydroxide.modules.announcement;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AnnouncementModule implements HydroModule {

    private HydroxideContext context;
    private BukkitTask task;
    private int cursor;

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

    private void showBossBar(Player player, Campaign campaign) {
        BossBar bar = BossBar.bossBar(
                context.text().format(campaign.message()),
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );
        player.showBossBar(bar);
        int ticks = (int) Math.max(20L, campaign.durationSeconds() * 20L);
        BukkitTask progress = Bukkit.getScheduler().runTaskTimer(context.plugin(), new Runnable() {
            private int elapsed;

            @Override
            public void run() {
                elapsed += 10;
                bar.progress(Math.max(0.0f, 1.0f - (elapsed / (float) ticks)));
            }
        }, 10L, 10L);
        Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
            progress.cancel();
            player.hideBossBar(bar);
        }, ticks);
    }

    private record Campaign(String type, String message, String subtitle, String permission, String world, long durationSeconds) {
    }
}
