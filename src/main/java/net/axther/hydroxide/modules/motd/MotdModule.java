package net.axther.hydroxide.modules.motd;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.List;
import java.util.UUID;

public final class MotdModule implements HydroModule, Listener {

    private HydroxideContext context;
    private YamlStore motdStore;

    @Override
    public String id() {
        return "motd";
    }

    @Override
    public String displayName() {
        return "MOTD";
    }

    @Override
    public String description() {
        return "Customizes server list MOTD, max-player display, version text, and hover samples.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.motdStore = new YamlStore(new File(context.plugin().getDataFolder(), "motd.yml"));
        seedDefault();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        YamlConfiguration yaml = motdStore.load();
        List<String> lines = yaml.getStringList("motd.lines");
        if (!lines.isEmpty()) {
            event.motd(context.text().format(String.join("\n", lines)));
        }
        if (yaml.getBoolean("player-count.enabled", false)) {
            event.setMaxPlayers(yaml.getInt("player-count.max", event.getMaxPlayers()));
            if (yaml.contains("player-count.online")) {
                event.setNumPlayers(yaml.getInt("player-count.online", event.getNumPlayers()));
            }
        }
        String versionText = yaml.getString("version-text", "");
        if (!versionText.isBlank()) {
            event.setVersion(versionText);
        }
        List<String> hover = yaml.getStringList("hover");
        if (!hover.isEmpty()) {
            event.getListedPlayers().clear();
            for (String line : hover) {
                event.getListedPlayers().add(new PaperServerListPingEvent.ListedPlayerInfo(
                        line,
                        UUID.nameUUIDFromBytes(line.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                ));
            }
        }
    }

    private void seedDefault() {
        YamlConfiguration yaml = motdStore.load();
        if (yaml.contains("motd.lines")) {
            return;
        }
        yaml.set("motd.lines", List.of("<gradient:#44CCFF:#FFB000>Hydroxide Server</gradient>", "<gray>Modern Paper core online"));
        yaml.set("hover", List.of("Hydroxide", "Modular server core"));
        yaml.set("player-count.enabled", false);
        yaml.set("player-count.max", 100);
        yaml.set("version-text", "");
        motdStore.save(yaml);
    }
}
