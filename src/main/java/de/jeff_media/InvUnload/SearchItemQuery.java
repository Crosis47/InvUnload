package de.jeff_media.InvUnload;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

class SearchItemQuery {

    private final Material material;
    private final Enchantment enchantment;

    private SearchItemQuery(@Nullable Material material, @Nullable Enchantment enchantment) {
        this.material = material;
        this.enchantment = enchantment;
    }

    static SearchItemQuery material(Material material) {
        return new SearchItemQuery(material, null);
    }

    static SearchItemQuery enchantment(Enchantment enchantment) {
        return new SearchItemQuery(null, enchantment);
    }

    static SearchItemQuery materialAndEnchantment(Material material, Enchantment enchantment) {
        return new SearchItemQuery(material, enchantment);
    }

    boolean hasEnchantmentFilter() {
        return enchantment != null;
    }

    @Nullable
    Material getMaterial() {
        return material;
    }

    boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (material != null && item.getType() != material) return false;
        if (enchantment == null) return true;
        return hasEnchantment(item, enchantment);
    }

    String getDescription() {
        if (material != null && enchantment != null) {
            return material.name() + " enchanted with " + getEnchantmentName(enchantment);
        }
        if (enchantment != null) {
            return "items enchanted with " + getEnchantmentName(enchantment);
        }
        return material == null ? "items" : material.name();
    }

    private boolean hasEnchantment(ItemStack item, Enchantment enchantment) {
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        if (meta.hasEnchant(enchantment)) return true;

        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
            return storageMeta.hasStoredEnchant(enchantment);
        }

        return false;
    }

    @Nullable
    static Enchantment parseEnchantment(String input) {
        String normalized = normalize(input);
        NamespacedKey key = NamespacedKey.fromString(normalized.contains(":") ? normalized : "minecraft:" + normalized);
        if (key != null) {
            Enchantment enchantment = Registry.ENCHANTMENT.get(key);
            if (enchantment != null) {
                return enchantment;
            }
        }

        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            NamespacedKey enchantmentKey = Registry.ENCHANTMENT.getKey(enchantment);
            if (enchantmentKey == null) continue;
            if (enchantmentKey.getKey().equalsIgnoreCase(normalized)
                    || enchantmentKey.toString().equalsIgnoreCase(normalized)
                    || getEnchantmentName(enchantment).equalsIgnoreCase(normalized)) {
                return enchantment;
            }
        }

        return null;
    }

    static ArrayList<String> getEnchantmentNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            names.add(getEnchantmentName(enchantment));
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    static String getEnchantmentName(Enchantment enchantment) {
        NamespacedKey key = Registry.ENCHANTMENT.getKey(enchantment);
        if (key == null) {
            key = enchantment.getKey();
        }
        if (NamespacedKey.MINECRAFT.equals(key.getNamespace())) {
            return key.getKey().toUpperCase(Locale.ROOT);
        }
        return key.toString();
    }

    static String normalize(String input) {
        return input.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
