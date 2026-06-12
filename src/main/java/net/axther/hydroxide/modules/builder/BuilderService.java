package net.axther.hydroxide.modules.builder;

import org.bukkit.entity.Player;

public interface BuilderService {

    boolean buildMode(Player player);

    boolean canBypassHydroxideProtection(Player player);
}
