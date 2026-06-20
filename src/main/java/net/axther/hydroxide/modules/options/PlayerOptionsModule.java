package net.axther.hydroxide.modules.options;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PlayerOptionsModule implements HydroModule, Listener, PlayerOptionsService {

    private HydroxideContext context;
    private YamlStore store;

    @Override
    public String id() {
        return "options";
    }

    @Override
    public String displayName() {
        return "Player Options";
    }

    @Override
    public String description() {
        return "Persisted player preference toggles and a control-panel GUI.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "options.yml"));
        context.services().playerOptionsService(this);
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("options", optionsCommand());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        context.services().clearPlayerOptionsService(this);
    }

    private CommandService optionsCommand() {
        return new CommandService(HydroCommand.builder("options")
                .permission("hydroxide.command.options")
                .playerOnly(true)
                .usage("/{label} [option] [on|off]")
                .executor(ctx -> options((Player) ctx.sender(), ctx.arguments().toArray(String[]::new)))
                .completer(ctx -> {
                    if (ctx.arguments().size() == 1) {
                        return CommandUtils.matching(ctx.argument(0), java.util.Arrays.stream(PlayerOption.values()).map(PlayerOption::key).toList());
                    }
                    if (ctx.arguments().size() == 2) {
                        return CommandUtils.matching(ctx.argument(1), List.of("off", "on"));
                    }
                    return List.of();
                })
                .build(), context.messages());
    }

    private void options(Player player, String[] args) {
        if (args.length >= 2) {
            PlayerOption option = PlayerOption.fromKey(args[0]);
            if (option == null) {
                context.message(player, "options.unknown", Map.of());
                return;
            }
            boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
            set(player.getUniqueId(), option, enabled);
            context.message(player, "options.updated", Map.of(
                    "option", option.key(),
                    "state", enabled
            ));
            return;
        }
        player.openInventory(menu(player));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !context.text().plain(event.getView().title()).equals(menuTitle())) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().lore() == null || item.getItemMeta().lore().isEmpty()) {
            return;
        }
        String key = context.text().plain(item.getItemMeta().displayName()).toLowerCase(Locale.ROOT).replace(" ", "-");
        PlayerOption option = PlayerOption.fromKey(key);
        if (option == null) {
            return;
        }
        set(player.getUniqueId(), option, !enabled(player.getUniqueId(), option));
        player.openInventory(menu(player));
    }

    @Override
    public boolean enabled(UUID playerId, PlayerOption option) {
        return new PlayerOptions(store.load()).enabled(playerId, option);
    }

    @Override
    public void set(UUID playerId, PlayerOption option, boolean enabled) {
        YamlConfiguration yaml = store.load();
        PlayerOptions options = new PlayerOptions(yaml);
        options.set(playerId, option, enabled);
        store.save(yaml);
    }

    private Inventory menu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, context.messages().component("options.title", Map.of()));
        for (PlayerOption option : PlayerOption.values()) {
            boolean enabled = enabled(player.getUniqueId(), option);
            ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(option.key().replace("-", " ")));
            meta.lore(List.of(context.messages().component(enabled ? "options.state.enabled" : "options.state.disabled", Map.of())));
            item.setItemMeta(meta);
            inventory.addItem(item);
        }
        return inventory;
    }

    private String menuTitle() {
        return context.text().plain(context.messages().component("options.title", Map.of()));
    }
}
