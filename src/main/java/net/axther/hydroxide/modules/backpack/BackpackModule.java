package net.axther.hydroxide.modules.backpack;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BackpackModule implements HydroModule, Listener {

    private HydroxideContext context;
    private BackpackSizePolicy policy;
    private final Map<UUID, Integer> openVaults = new HashMap<>();

    @Override
    public String id() {
        return "backpacks";
    }

    @Override
    public String displayName() {
        return "Backpacks";
    }

    @Override
    public String description() {
        return "PDC-backed portable backpack and player vault inventories.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.policy = new BackpackSizePolicy(
                context.plugin().getConfig().getInt("backpacks.default-slots", 27),
                context.plugin().getConfig().getInt("backpacks.max-slots", 54)
        );
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("backpack", command("backpack", "hydroxide.command.backpack", ctx -> open((Player) ctx.sender(), 1), null));
        context.commands().register("pv", command("pv", "hydroxide.command.pv",
                ctx -> open((Player) ctx.sender(), Math.max(1, parseInt(ctx.argument(0), 1))), this::vaultCompletions));
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    private CommandService command(String name, String permission, HydroCommand.HydroCommandExecutor executor,
                                   HydroCommand.HydroTabCompleter completer) {
        return new CommandService(HydroCommand.builder(name)
                .permission(permission)
                .playerOnly(true)
                .usage("/{label} [number]")
                .executor(executor)
                .completer(completer)
                .build(), context.messages());
    }

    private List<String> vaultCompletions(CommandContext ctx) {
        int maxVaults = context == null ? 3 : context.plugin().getConfig().getInt("backpacks.max-vaults", 3);
        return ctx.arguments().size() == 1
                ? CompletionUtils.integerRange(ctx.argument(0), 1, Math.max(1, maxVaults))
                : List.of();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Integer vault = openVaults.remove(player.getUniqueId());
        if (vault != null) {
            save(player, vault, event.getInventory().getContents());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        openVaults.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!context.plugin().getConfig().getBoolean("backpacks.drop-on-death", false)) {
            return;
        }
        Player player = event.getEntity();
        for (int vault = 1; vault <= context.plugin().getConfig().getInt("backpacks.max-vaults", 3); vault++) {
            ItemStack[] contents = load(player, vault, 54);
            for (ItemStack item : contents) {
                if (item != null && item.getType() != Material.AIR) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            player.getPersistentDataContainer().remove(key(vault));
        }
    }

    private void open(Player player, int vault) {
        int slots = policy.slotsFor(permissions(player));
        Inventory inventory = Bukkit.createInventory(player, slots, context.messages().component("backpacks.title", Map.of("vault", vault)));
        ItemStack[] saved = load(player, vault, slots);
        inventory.setContents(saved);
        openVaults.put(player.getUniqueId(), vault);
        player.openInventory(inventory);
    }

    private Set<String> permissions(Player player) {
        return player.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .collect(java.util.stream.Collectors.toSet());
    }

    private ItemStack[] load(Player player, int vault, int size) {
        String encoded = player.getPersistentDataContainer().get(key(vault), PersistentDataType.STRING);
        ItemStack[] contents = new ItemStack[size];
        if (encoded == null || encoded.isBlank()) {
            return contents;
        }
        try {
            ItemStack[] saved = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(encoded));
            System.arraycopy(saved, 0, contents, 0, Math.min(size, saved.length));
        } catch (RuntimeException exception) {
            context.plugin().getLogger().warning(context.text().plain(context.messages().component("backpacks.load-failed-log",
                    Map.of("player", player.getName(), "reason", exception.getMessage()))));
        }
        return contents;
    }

    private void save(Player player, int vault, ItemStack[] contents) {
        try {
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            pdc.set(key(vault), PersistentDataType.STRING, Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(contents)));
        } catch (RuntimeException exception) {
            context.plugin().getLogger().warning(context.text().plain(context.messages().component("backpacks.save-failed-log",
                    Map.of("player", player.getName(), "reason", exception.getMessage()))));
        }
    }

    private NamespacedKey key(int vault) {
        return new NamespacedKey(context.plugin(), "backpack_" + vault);
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
