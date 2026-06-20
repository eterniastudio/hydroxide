package net.axther.hydroxide.modules.economy;

import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.UUID;

final class ChequeRedemptionStore {

    private static final String REDEEMED = "redeemed";

    private final YamlStore store;

    ChequeRedemptionStore(YamlStore store) {
        this.store = store;
    }

    boolean redeemed(UUID chequeId) {
        return store.load().getBoolean(path(chequeId), false);
    }

    void markRedeemed(UUID chequeId) {
        YamlConfiguration yaml = store.load();
        yaml.set(path(chequeId), true);
        store.save(yaml);
    }

    private String path(UUID chequeId) {
        return REDEEMED + "." + chequeId;
    }
}
