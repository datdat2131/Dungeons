package com.astrax.dungeon.reward;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
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

    /** Vat pham do plugin tao co PDC 'dungeon_item' de nhan dien/chong gia mao. */
    public ItemStack buildItem(LootTable.Drop d) {
        Material mat = Material.matchMaterial(d.material());
        if (mat == null || mat.isAir()) mat = Material.STONE;
        ItemStack item = new ItemStack(mat, Math.max(1, d.amount()));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(d.rarity().color() + d.name()));
        List<Component> lore = new ArrayList<>();
        for (String l : d.lore()) lore.add(plain(l));
        lore.add(plain("&8Độ hiếm: " + d.rarity().color() + d.rarity().name()));
        meta.lore(lore);
        if (d.glow()) meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(plugin.itemKey(), PersistentDataType.STRING, d.id());
        item.setItemMeta(meta);
        return item;
    }

    private static Component plain(String s) { return Text.c(s).decoration(TextDecoration.ITALIC, false); }
}
