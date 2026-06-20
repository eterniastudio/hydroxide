package net.axther.hydroxide.modules.backup;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupModule implements HydroModule {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private HydroxideContext context;
    private YamlStore store;
    private BukkitTask task;

    @Override
    public String id() {
        return "backups";
    }

    @Override
    public String displayName() {
        return "Hot Backups";
    }

    @Override
    public String description() {
        return "Asynchronous zip backups with main-thread world saves and rotation.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "backups.yml"));
        seedDefaults();
        context.commands().register("backup", backupCommand());
        long intervalMinutes = store.load().getLong("interval-minutes", 360L);
        if (intervalMinutes > 0) {
            task = Bukkit.getScheduler().runTaskTimer(context.plugin(), () -> runBackup(null), 20L * 60L, intervalMinutes * 60L * 20L);
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        if (task != null) {
            task.cancel();
        }
    }

    private CommandService backupCommand() {
        return new CommandService(HydroCommand.builder("backup")
                .permission("hydroxide.command.backup")
                .usage("/{label}")
                .executor(ctx -> runBackup(ctx.sender()))
                .build(), context.messages());
    }

    private void runBackup(CommandSender requester) {
        for (World world : Bukkit.getWorlds()) {
            world.save();
        }
        if (requester != null) {
            context.message(requester, "backups.started", Map.of());
        }
        Bukkit.getScheduler().runTaskAsynchronously(context.plugin(), () -> {
            try {
                File backup = createZip();
                rotate();
                if (requester != null) {
                    Bukkit.getScheduler().runTask(context.plugin(), () -> context.message(requester, "backups.completed",
                            Map.of("file", backup.getName())));
                }
            } catch (IOException exception) {
                context.plugin().getLogger().warning(context.text().plain(context.messages().component("backups.failed-log",
                        Map.of("reason", exception.getMessage()))));
                if (requester != null) {
                    Bukkit.getScheduler().runTask(context.plugin(), () -> context.message(requester, "backups.failed",
                            Map.of("reason", exception.getMessage())));
                }
            }
        });
    }

    private File createZip() throws IOException {
        YamlConfiguration yaml = store.load();
        File outputDirectory = new File(context.plugin().getDataFolder(), yaml.getString("output-directory", "backups"));
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IOException("Unable to create backup directory");
        }
        File zipFile = new File(outputDirectory, "hydroxide-" + FORMATTER.format(Instant.now()) + ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (String source : yaml.getStringList("sources")) {
                File file = resolveSource(source);
                if (file.exists()) {
                    zipPath(zip, file.toPath(), file.toPath().getParent(), outputDirectory.toPath().toAbsolutePath().normalize());
                }
            }
        }
        return zipFile;
    }

    private File resolveSource(String source) {
        File file = new File(source);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(Bukkit.getWorldContainer(), source);
    }

    private void zipPath(ZipOutputStream zip, Path source, Path root, Path outputDirectory) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        try (java.util.stream.Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
                Path normalized = path.toAbsolutePath().normalize();
                if (normalized.startsWith(outputDirectory) || Files.isDirectory(path)) {
                    continue;
                }
                ZipEntry entry = new ZipEntry(root.relativize(path).toString().replace('\\', '/'));
                zip.putNextEntry(entry);
                Files.copy(path, zip);
                zip.closeEntry();
            }
        }
    }

    private void rotate() throws IOException {
        YamlConfiguration yaml = store.load();
        File outputDirectory = new File(context.plugin().getDataFolder(), yaml.getString("output-directory", "backups"));
        File[] files = outputDirectory.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null) {
            return;
        }
        List<BackupRotationPolicy.BackupFile> backups = java.util.Arrays.stream(files)
                .map(file -> new BackupRotationPolicy.BackupFile(file.getName(), Instant.ofEpochMilli(file.lastModified())))
                .toList();
        BackupRotationPolicy policy = new BackupRotationPolicy(yaml.getInt("retain", 10));
        for (BackupRotationPolicy.BackupFile deletion : policy.toDelete(backups)) {
            Files.deleteIfExists(new File(outputDirectory, deletion.name()).toPath());
        }
    }

    private void seedDefaults() {
        YamlConfiguration yaml = store.load();
        if (yaml.contains("sources")) {
            return;
        }
        yaml.set("interval-minutes", 360);
        yaml.set("retain", 10);
        yaml.set("output-directory", "backups");
        yaml.set("sources", List.of("world", "world_nether", "world_the_end", "plugins/Hydroxide"));
        store.save(yaml);
    }
}
