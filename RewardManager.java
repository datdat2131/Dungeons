package com.astrax.dungeon.reward;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RewardManager {
    private final AstraXDungeon plugin;
    private final Map<String, LootTable> cache = new HashMap<>();

    public RewardManager(AstraXDungeon plugin) { this.plugin = plugin; }

    public void reload() { cache.clear(); }

    public LootTable get(String bossId) {
        LootTable t = cache.get(bossId);
        if (t == null) {
            t = LootTable.load(new File(plugin.getDataFolder(), "rewards/" + bossId + ".yml"));
            if (t != null) cache.put(bossId, t);
        }
        return t;
    }

    public ItemStack buildItem(LootTable.Drop d) { return buildItem(d, d.amount()); }

    /** Vat pham do plugin tao co PDC 'dungeon_item' de nhan dien/chong gia mao. */
    public ItemStack buildItem(LootTable.Drop d, int amount) {
        Material mat = Material.matchMaterial(d.material());
        if (mat == null || mat.isAir()) mat = Material.STONE;
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(d.rarity().color() + d.name()));
        List<Component> lore = new ArrayList<>();
        for (String l : d.lore()) lore.add(plain(l));
        lore.add(plain("&8Độ hiếm: " + d.rarity().color() + d.rarity().name()));
        meta.lore(lore);
        if (d.glow()) meta.setEnchantmentGlintOverride(true);
        for (Map.Entry<String, Integer> en : d.enchants().entrySet()) {
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(en.getKey()));
            if (ench != null) meta.addEnchant(ench, en.getValue(), true);
            else plugin.getLogger().warning("Enchant khong hop le trong loot '" + d.id() + "': " + en.getKey());
        }
        if (d.unbreakable()) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        if (d.customModelData() > 0) meta.setCustomModelData(d.customModelData());
        meta.getPersistentDataContainer().set(plugin.itemKey(), PersistentDataType.STRING, d.id());
        item.setItemMeta(meta);
        return item;
    }

    private static Component plain(String s) { return Text.c(s).decoration(TextDecoration.ITALIC, false); }
}
