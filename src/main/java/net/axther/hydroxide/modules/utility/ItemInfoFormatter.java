package net.axther.hydroxide.modules.utility;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

final class ItemInfoFormatter {

    private ItemInfoFormatter() {
    }

    static Details details(ItemStack item) {
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;
        String damage = "none";
        if (meta instanceof Damageable damageable && damageable.getDamage() > 0) {
            damage = String.valueOf(damageable.getDamage());
        }
        int loreLines = meta == null || meta.lore() == null ? 0 : meta.lore().size();
        boolean hasDisplayName = meta != null && meta.displayName() != null;
        return new Details(
                item.getType().name(),
                item.getType().key().asString(),
                item.getAmount(),
                item.getMaxStackSize(),
                damage,
                hasDisplayName,
                loreLines,
                item.getEnchantments().size()
        );
    }

    record Details(String material, String key, int amount, int maxStackSize, String damage,
                   boolean hasDisplayName, int loreLines, int enchantments) {
    }
}
