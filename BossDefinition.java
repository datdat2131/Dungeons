package com.astrax.dungeon.boss;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class BossDefinition {
    public record Phase(int at, List<String> skills, boolean enrage, String message) {}

    private final YamlConfiguration yml;
    private final List<Phase> phases = new ArrayList<>();

    private BossDefinition(YamlConfiguration yml) {
        this.yml = yml;
        for (Map<?, ?> m : yml.getMapList("phases")) {
            if (!(m.get("at") instanceof Number at)) continue;
            List<String> skills = new ArrayList<>();
            if (m.get("skills") instanceof List<?> l) for (Object o : l) skills.add(String.valueOf(o));
            boolean enrage = Boolean.TRUE.equals(m.get("enrage"));
            String msg = m.get("message") == null ? "" : String.valueOf(m.get("message"));
            phases.add(new Phase(at.intValue(), skills, enrage, msg));
        }
        phases.sort(Comparator.comparingInt(Phase::at).reversed());
    }

    /** Tra ve null neu file khong ton tai. */
    public static BossDefinition load(File f) {
        if (!f.exists()) return null;
        return new BossDefinition(YamlConfiguration.loadConfiguration(f));
    }

    public String id() { return yml.getString("id", "boss"); }
    public String displayName() { return yml.getString("display-name", id()); }
    public boolean isMythic() { return "MYTHICMOBS".equalsIgnoreCase(yml.getString("type", "VANILLA")); }
    public String mythicId() { return yml.getString("mythic-id", ""); }
    public String entityType() { return yml.getString("entity", "ZOMBIE"); }
    public double maxHealth() { return yml.getDouble("max-health", 5000); }
    public double damage() { return yml.getDouble("damage", 10); }
    public double scale() { return yml.getDouble("scale", 1.0); }
    public double hpPerPlayer() { return yml.getDouble("hp-per-player", 0.35); }
    public List<Phase> phases() { return phases; }
    public ConfigurationSection skill(String name) { return yml.getConfigurationSection("skills." + name); }

    public net.kyori.adventure.bossbar.BossBar.Color barColor() {
        try {
            return net.kyori.adventure.bossbar.BossBar.Color.valueOf(yml.getString("bar-color", "RED").toUpperCase());
        } catch (IllegalArgumentException e) {
            return net.kyori.adventure.bossbar.BossBar.Color.RED;
        }
    }
}
