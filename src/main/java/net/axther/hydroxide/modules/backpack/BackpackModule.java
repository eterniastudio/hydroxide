package net.axther.hydroxide.modules.backpack;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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

public final class BackpackModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private static final String TITLE_PREFIX = "Hydroxide Vault ";

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
        context.commands().register("backpack", this);
        context.commands().register("pv", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can open backpacks.");
            return true;
        }
        if (!context.requirePermission(sender, command.getName().equalsIgnoreCase("pv")
                ? "hydroxide.command.pv"
                : "hydroxide.command.backpack")) {
            return true;
        }
        int vault = command.getName().equalsIgnoreCase("pv") && args.length > 0 ? parseInt(args[0], 1) : 1;
        open(player, Math.max(1, vault));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("pv") && args.length == 1) {
            int maxVaults = context == null ? 3 : context.plugin().getConfig().getInt("backpacks.max-vaults", 3);
            return CompletionUtils.integerRange(args[0], 1, Math.max(1, maxVaults));
        }
        return List.of();
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
        Inventory inventory = Bukkit.createInventory(player, slots, context.text().format(TITLE_PREFIX + vault));
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
            context.plugin().getLogger().warning("Unable to read backpack data for " + player.getName() + ": " + exception.getMessage());
        }
        return contents;
    }

    private void save(Player player, int vault, ItemStack[] contents) {
        try {
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            pdc.set(key(vault), PersistentDataType.STRING, Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(contents)));
        } catch (RuntimeException exception) {
            context.plugin().getLogger().warning("Unable to save backpack data for " + player.getName() + ": " + exception.getMessage());
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
