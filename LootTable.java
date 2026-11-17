package com.astrax.dungeon.reward;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LootTable {
    public record Drop(String id, String name, String type, double chance, Rarity rarity, String material, int amount,
                       List<String> lore, List<String> commands, double money, boolean glow,
                       double pityStep, double pityMax) {}

    private final double baseMoney;
    private final List<Drop> drops = new ArrayList<>();

    private LootTable(YamlConfiguration y) {
        baseMoney = y.getDouble("money", 0);
        for (Map<?, ?> m : y.getMapList("drops")) {
            String id = str(m, "id", "drop");
            drops.add(new Drop(id, str(m, "name", id), str(m, "type", "ITEM").toUpperCase(),
                    num(m, "chance", 1), Rarity.parse(str(m, "rarity", "COMMON"), Rarity.COMMON),
                    str(m, "material", "STONE"), (int) num(m, "amount", 1), list(m, "lore"), list(m, "commands"),
                    num(m, "money", 0), Boolean.TRUE.equals(m.get("glow")),
                    num(m, "pity-step", 0), num(m, "pity-max", 0)));
        }
    }

    /** null neu khong co file. */
    public static LootTable load(File f) {
        return f.exists() ? new LootTable(YamlConfiguration.loadConfiguration(f)) : null;
    }

    public double baseMoney() { return baseMoney; }
    public List<Drop> drops() { return drops; }

    private static String str(Map<?, ?> m, String k, String d) { return m.get(k) == null ? d : String.valueOf(m.get(k)); }
    private static double num(Map<?, ?> m, String k, double d) { return m.get(k) instanceof Number n ? n.doubleValue() : d; }
    private static List<String> list(Map<?, ?> m, String k) {
        List<String> out = new ArrayList<>();
        if (m.get(k) instanceof List<?> l) for (Object o : l) out.add(String.valueOf(o));
        return out;
    }
}
