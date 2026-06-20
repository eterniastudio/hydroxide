package net.axther.hydroxide.storage;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class YamlStore {

    private final File file;

    public YamlStore(File file) {
        this.file = file;
    }

    public File file() {
        return file;
    }

    public YamlConfiguration load() {
        ensureParent();
        return YamlConfiguration.loadConfiguration(file);
    }

    public void save(YamlConfiguration configuration) {
        ensureParent();
        try {
            configuration.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save " + file.getAbsolutePath(), exception);
        }
    }

    private void ensureParent() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create directory " + parent.getAbsolutePath());
        }
    }
}
