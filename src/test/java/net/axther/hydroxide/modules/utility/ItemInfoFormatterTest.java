package net.axther.hydroxide.modules.utility;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemInfoFormatterTest {

    @Test
    void formatsBasicMaterialDetails() {
        ItemInfoFormatter.Details details = ItemInfoFormatter.details(new FakeItemStack(Material.DIAMOND, 3, 64, null));

        assertEquals("DIAMOND", details.material());
        assertEquals("minecraft:diamond", details.key());
        assertEquals(3, details.amount());
        assertEquals(64, details.maxStackSize());
        assertEquals("none", details.damage());
        assertEquals(0, details.enchantments());
    }

    @Test
    void formatsDamageWhenPresent() {
        ItemStack stack = new FakeItemStack(Material.DIAMOND_SWORD, 1, 1, damageMeta(12));

        ItemInfoFormatter.Details details = ItemInfoFormatter.details(stack);

        assertEquals("12", details.damage());
    }

    private static ItemMeta damageMeta(int damage) {
        return (ItemMeta) Proxy.newProxyInstance(
                ItemInfoFormatterTest.class.getClassLoader(),
                new Class<?>[]{Damageable.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDamage" -> damage;
                    case "displayName", "lore" -> null;
                    default -> null;
                }
        );
    }

    private static final class FakeItemStack extends ItemStack {
        private final Material type;
        private final int amount;
        private final int maxStackSize;
        private final ItemMeta meta;

        private FakeItemStack(Material type, int amount, int maxStackSize, ItemMeta meta) {
            this.type = type;
            this.amount = amount;
            this.maxStackSize = maxStackSize;
            this.meta = meta;
        }

        @Override
        public Material getType() {
            return type;
        }

        @Override
        public int getAmount() {
            return amount;
        }

        @Override
        public int getMaxStackSize() {
            return maxStackSize;
        }

        @Override
        public boolean hasItemMeta() {
            return meta != null;
        }

        @Override
        public ItemMeta getItemMeta() {
            return meta;
        }

        @Override
        public Map<Enchantment, Integer> getEnchantments() {
            return Map.of();
        }
    }
}
