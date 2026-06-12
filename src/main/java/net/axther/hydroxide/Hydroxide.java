package net.axther.hydroxide;

import net.axther.hydroxide.commands.CommandRegistrar;
import net.axther.hydroxide.modules.ModuleConfig;
import net.axther.hydroxide.modules.ModuleManager;
import net.axther.hydroxide.modules.afk.AfkModule;
import net.axther.hydroxide.modules.admin.AdminUtilityModule;
import net.axther.hydroxide.modules.api.EmbeddedApiModule;
import net.axther.hydroxide.modules.announcement.AnnouncementModule;
import net.axther.hydroxide.modules.armorstand.ArmorStandEditorModule;
import net.axther.hydroxide.modules.backpack.BackpackModule;
import net.axther.hydroxide.modules.backup.BackupModule;
import net.axther.hydroxide.modules.bridge.RedisBridgeModule;
import net.axther.hydroxide.modules.builder.BuilderModule;
import net.axther.hydroxide.modules.channels.ChatChannelsModule;
import net.axther.hydroxide.modules.chatfilter.ChatFilterModule;
import net.axther.hydroxide.modules.chat.ChatModule;
import net.axther.hydroxide.modules.combat.CombatTagModule;
import net.axther.hydroxide.modules.core.CoreModule;
import net.axther.hydroxide.modules.economy.EconomyModule;
import net.axther.hydroxide.modules.integration.PlaceholderIntegrationModule;
import net.axther.hydroxide.modules.interaction.InteractionModule;
import net.axther.hydroxide.modules.item.ItemEditorModule;
import net.axther.hydroxide.modules.jail.JailModule;
import net.axther.hydroxide.modules.kit.KitModule;
import net.axther.hydroxide.modules.motd.MotdModule;
import net.axther.hydroxide.modules.navigation.NavigationModule;
import net.axther.hydroxide.modules.moderation.ModerationModule;
import net.axther.hydroxide.modules.nickname.NicknameModule;
import net.axther.hydroxide.modules.hologram.HologramModule;
import net.axther.hydroxide.modules.options.PlayerOptionsModule;
import net.axther.hydroxide.modules.portal.PortalModule;
import net.axther.hydroxide.modules.proxy.ProxyBridgeModule;
import net.axther.hydroxide.modules.protection.ProtectionModule;
import net.axther.hydroxide.modules.rtp.RandomTeleportModule;
import net.axther.hydroxide.modules.shop.ShopModule;
import net.axther.hydroxide.modules.social.SocialModule;
import net.axther.hydroxide.modules.spawn.AdvancedSpawnModule;
import net.axther.hydroxide.modules.staff.VanishModule;
import net.axther.hydroxide.modules.stats.StatsModule;
import net.axther.hydroxide.modules.tablist.TablistModule;
import net.axther.hydroxide.modules.teleport.TeleportModule;
import net.axther.hydroxide.modules.utility.UtilityBindingModule;
import net.axther.hydroxide.modules.welcome.WelcomeModule;
import net.axther.hydroxide.modules.world.WorldManagerModule;
import net.axther.hydroxide.storage.NamedLocationStore;
import net.axther.hydroxide.storage.PlayerDataStores;
import net.axther.hydroxide.teleport.BackLocationService;
import net.axther.hydroxide.teleport.TeleportRequestService;
import net.axther.hydroxide.text.TextFormatter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Hydroxide extends JavaPlugin {

    private ModuleManager moduleManager;
    private HydroxideContext context;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        moduleManager = new ModuleManager(ModuleConfig.fromConfiguration(getConfig()));
        context = new HydroxideContext(
                this,
                new TextFormatter(),
                new CommandRegistrar(this),
                moduleManager,
                PlayerDataStores.create(this),
                new NamedLocationStore(new File(getDataFolder(), "warps.yml"), "warps"),
                new NamedLocationStore(new File(getDataFolder(), "spawn.yml"), "spawns"),
                new BackLocationService(),
                new TeleportRequestService(),
                new HydroxideServices()
        );

        moduleManager.register(new CoreModule());
        moduleManager.register(new NicknameModule());
        moduleManager.register(new EconomyModule());
        moduleManager.register(new PlaceholderIntegrationModule());
        moduleManager.register(new ChatModule());
        moduleManager.register(new TeleportModule());
        moduleManager.register(new AdvancedSpawnModule());
        moduleManager.register(new WelcomeModule());
        moduleManager.register(new NavigationModule());
        moduleManager.register(new JailModule());
        moduleManager.register(new AnnouncementModule());
        moduleManager.register(new ChatFilterModule());
        moduleManager.register(new TablistModule());
        moduleManager.register(new BackpackModule());
        moduleManager.register(new CombatTagModule());
        moduleManager.register(new RandomTeleportModule());
        moduleManager.register(new ItemEditorModule());
        moduleManager.register(new UtilityBindingModule());
        moduleManager.register(new AfkModule());
        moduleManager.register(new InteractionModule());
        moduleManager.register(new StatsModule());
        moduleManager.register(new RedisBridgeModule());
        moduleManager.register(new EmbeddedApiModule());
        moduleManager.register(new ProxyBridgeModule());
        moduleManager.register(new ChatChannelsModule());
        moduleManager.register(new SocialModule());
        moduleManager.register(new WorldManagerModule());
        moduleManager.register(new BackupModule());
        moduleManager.register(new KitModule());
        moduleManager.register(new ShopModule());
        moduleManager.register(new PortalModule());
        moduleManager.register(new MotdModule());
        moduleManager.register(new BuilderModule());
        moduleManager.register(new ArmorStandEditorModule());
        moduleManager.register(new HologramModule());
        moduleManager.register(new AdminUtilityModule());
        moduleManager.register(new PlayerOptionsModule());
        moduleManager.register(new ProtectionModule());
        moduleManager.register(new VanishModule());
        moduleManager.register(new ModerationModule());
        moduleManager.enableConfiguredModules(context);
        getLogger().info("Hydroxide enabled with modules: " + String.join(", ", moduleManager.enabledModuleIds()));
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll(context);
        }
        if (context != null) {
            context.playerData().close();
        }
    }
}
