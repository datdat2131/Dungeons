package com.astrax.dungeon.reward;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.shop.ShopManager;
import com.astrax.dungeon.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
        // vat pham tuy chinh cua admin: giu nguyen toan bo ten/lore/enchant/NBT
        if (d.itemData() != null && !d.itemData().isEmpty()) {
            try {
                ItemStack custom = ItemStack.deserializeBytes(Base64.getDecoder().decode(d.itemData()));
                custom.setAmount(Math.max(1, amount));
                ItemMeta cm = custom.getItemMeta();
                if (cm != null) {
                    cm.getPersistentDataContainer().set(plugin.itemKey(), PersistentDataType.STRING, d.id());
                    custom.setItemMeta(cm);
                }
                return custom;
            } catch (Exception ex) {
                plugin.getLogger().warning("Khong doc duoc item-data cua '" + d.id() + "': " + ex.getMessage());
            }
        }
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

    // ---------- chinh sua bang GUI admin ----------
    /** Ghi bang thuong ra rewards/<boss>.yml (se mat comment cu) va cap nhat cache. */
    public void save(String bossId, LootTable t) {
        YamlConfiguration y = new YamlConfiguration();
        y.options().setHeader(List.of("File nay duoc ghi boi GUI admin (/dungeon admin lootedit). Van co the sua tay."));
        y.set("id", bossId);
        y.set("money", t.baseMoney());
        List<Map<String, Object>> list = new ArrayList<>();
        for (LootTable.Drop d : t.drops()) list.add(LootTable.toMap(d));
        y.set("drops", list);
        try {
            File f = new File(plugin.getDataFolder(), "rewards/" + bossId + ".yml");
            f.getParentFile().mkdirs();
            y.save(f);
        } catch (IOException e) {
            plugin.getLogger().warning("Khong ghi duoc rewards/" + bossId + ".yml: " + e.getMessage());
        }
        cache.put(bossId, t);
    }

    public static String encode(ItemStack one) {
        return Base64.getEncoder().encodeToString(one.serializeAsBytes());
    }

    private static String nameOf(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        return ShopManager.pretty(item.getType().name().toLowerCase());
    }

    /** Dien thong tin vat pham admin dang cam vao map (xoa cac truong dac thu cua vat pham tao tu config). */
    public void applyItem(Map<String, Object> m, ItemStack item) {
        ItemStack one = item.clone();
        int amount = Math.max(1, item.getAmount());
        one.setAmount(1);
        m.put("type", "ITEM");
        m.put("material", one.getType().name());
        m.put("name", nameOf(item));
        m.put("amount", amount);
        m.remove("max-amount");
        m.put("item-data", encode(one));
        for (String k : new String[]{"glow", "unbreakable", "enchants", "lore", "custom-model-data", "commands"}) m.remove(k);
    }

    /** Tao 1 muc loot moi tu vat pham admin dang cam. */
    public LootTable.Drop dropFromItem(ItemStack item, double chance, Rarity rarity, Set<String> usedIds) {
        String id;
        do {
            id = "custom_" + Integer.toHexString(ThreadLocalRandom.current().nextInt(0x1000000));
        } while (usedIds.contains(id));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("chance", chance);
        m.put("rarity", rarity.name());
        applyItem(m, item);
        return LootTable.parseDrop(m);
    }

    private static Component plain(String s) { return Text.c(s).decoration(TextDecoration.ITALIC, false); }
}
