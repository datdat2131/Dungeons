package com.astrax.dungeon.reward;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LootTable {
    /**
     * guaranteed: luon nhan (khong roll). onlyTop: chi top N sat thuong Boss moi duoc roll (0 = ai cung duoc).
     * amount..maxAmount: so luong ngau nhien. enchants: {sharpness: 5}.
     * itemData: vat pham tuy chinh do admin tao (Base64 cua ItemStack) - giu nguyen ten, lore, enchant, NBT.
     */
    public record Drop(String id, String name, String type, double chance, Rarity rarity, String material,
                       int amount, int maxAmount, List<String> lore, List<String> commands, double money,
                       boolean glow, double pityStep, double pityMax, boolean guaranteed, int onlyTop,
                       Map<String, Integer> enchants, boolean unbreakable, int customModelData, String itemData) {}

    private double baseMoney;
    private final List<Drop> drops = new ArrayList<>();

    private LootTable(YamlConfiguration y) {
        baseMoney = y.getDouble("money", 0);
        for (Map<?, ?> m : y.getMapList("drops")) drops.add(parseDrop(m));
    }

    public static LootTable empty() { return new LootTable(new YamlConfiguration()); }

    /** Doc 1 muc vat pham tu YAML (dung cho loot, cua hang, phan thuong nhiem vu). */
    public static Drop parseDrop(Map<?, ?> m) {
        String id = str(m, "id", "drop");
        int amount = (int) num(m, "amount", 1);
        int maxAmount = Math.max(amount, (int) num(m, "max-amount", amount));
        Map<String, Integer> ench = new LinkedHashMap<>();
        if (m.get("enchants") instanceof Map<?, ?> em) {
            for (Map.Entry<?, ?> en : em.entrySet()) {
                int lvl = en.getValue() instanceof Number nn ? nn.intValue() : 1;
                ench.put(String.valueOf(en.getKey()).toLowerCase(), lvl);
            }
        }
        return new Drop(id, str(m, "name", id), str(m, "type", "ITEM").toUpperCase(),
                num(m, "chance", 1), Rarity.parse(str(m, "rarity", "COMMON"), Rarity.COMMON),
                str(m, "material", "STONE"), amount, maxAmount, list(m, "lore"), list(m, "commands"),
                num(m, "money", 0), bool(m, "glow"), num(m, "pity-step", 0), num(m, "pity-max", 0),
                bool(m, "guaranteed"), (int) num(m, "only-top", 0), ench,
                bool(m, "unbreakable"), (int) num(m, "custom-model-data", 0), str(m, "item-data", ""));
    }

    /** Chuyen nguoc Drop thanh map de ghi YAML / chinh sua roi parse lai. */
    public static Map<String, Object> toMap(Drop d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.id());
        m.put("name", d.name());
        m.put("type", d.type());
        m.put("rarity", d.rarity().name());
        if (d.type().equals("ITEM")) m.put("material", d.material());
        m.put("amount", d.amount());
        if (d.maxAmount() > d.amount()) m.put("max-amount", d.maxAmount());
        m.put("chance", d.chance());
        if (d.guaranteed()) m.put("guaranteed", true);
        if (d.onlyTop() > 0) m.put("only-top", d.onlyTop());
        if (d.money() > 0) m.put("money", d.money());
        if (d.glow()) m.put("glow", true);
        if (d.unbreakable()) m.put("unbreakable", true);
        if (d.customModelData() > 0) m.put("custom-model-data", d.customModelData());
        if (d.pityStep() > 0) {
            m.put("pity-step", d.pityStep());
            m.put("pity-max", d.pityMax());
        }
        if (!d.lore().isEmpty()) m.put("lore", new ArrayList<>(d.lore()));
        if (!d.commands().isEmpty()) m.put("commands", new ArrayList<>(d.commands()));
        if (!d.enchants().isEmpty()) m.put("enchants", new LinkedHashMap<>(d.enchants()));
        if (d.itemData() != null && !d.itemData().isEmpty()) m.put("item-data", d.itemData());
        return m;
    }

    /** null neu khong co file. */
    public static LootTable load(File f) {
        return f.exists() ? new LootTable(YamlConfiguration.loadConfiguration(f)) : null;
    }

    public double baseMoney() { return baseMoney; }
    public void setBaseMoney(double v) { this.baseMoney = Math.max(0, v); }
    public List<Drop> drops() { return drops; }

    private static String str(Map<?, ?> m, String k, String d) { return m.get(k) == null ? d : String.valueOf(m.get(k)); }
    private static double num(Map<?, ?> m, String k, double d) { return m.get(k) instanceof Number n ? n.doubleValue() : d; }
    private static boolean bool(Map<?, ?> m, String k) { return Boolean.TRUE.equals(m.get(k)); }
    private static List<String> list(Map<?, ?> m, String k) {
        List<String> out = new ArrayList<>();
        if (m.get(k) instanceof List<?> l) for (Object o : l) out.add(String.valueOf(o));
        return out;
    }
}
