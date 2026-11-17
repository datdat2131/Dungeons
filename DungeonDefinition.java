package com.astrax.dungeon.dungeon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class DungeonDefinition {
    private final File file;
    private final YamlConfiguration yml;
    private final String id;

    private DungeonDefinition(File file) {
        this.file = file;
        this.yml = YamlConfiguration.loadConfiguration(file);
        String fallback = file.getName().replace(".yml", "");
        this.id = yml.getString("id", fallback);
    }

    public static DungeonDefinition load(File file) { return new DungeonDefinition(file); }

    public String id() { return id; }
    public boolean enabled() { return yml.getBoolean("enabled", true); }
    public String displayName() { return yml.getString("display-name", id); }
    public int weight() { return Math.max(1, yml.getInt("weight", 10)); }
    public int minPlayers() { return yml.getInt("min-players", 1); }
    public int maxPlayers() { return yml.getInt("max-players", 30); }
    public String bossId() { return yml.getString("boss.id", ""); }
    public String icon() { return yml.getString("icon", "PAPER"); }
    public String recommendedLevel() { return yml.getString("recommended-level", "?"); }

    /** Tra ve null neu chua dat spawn hoac world chua load. */
    public Location spawn() {
        if (!yml.contains("spawn.world")) return null;
        World w = Bukkit.getWorld(yml.getString("spawn.world", ""));
        if (w == null) return null;
        return new Location(w, yml.getDouble("spawn.x"), yml.getDouble("spawn.y"), yml.getDouble("spawn.z"),
                (float) yml.getDouble("spawn.yaw"), (float) yml.getDouble("spawn.pitch"));
    }

    public Location bossSpawn() {
        if (!yml.contains("boss.spawn.world")) return null;
        World w = Bukkit.getWorld(yml.getString("boss.spawn.world", ""));
        if (w == null) return null;
        return new Location(w, yml.getDouble("boss.spawn.x"), yml.getDouble("boss.spawn.y"), yml.getDouble("boss.spawn.z"),
                (float) yml.getDouble("boss.spawn.yaw"), (float) yml.getDouble("boss.spawn.pitch"));
    }

    public void setBossSpawn(Location l) {
        yml.set("boss.spawn.world", l.getWorld().getName());
        yml.set("boss.spawn.x", l.getX());
        yml.set("boss.spawn.y", l.getY());
        yml.set("boss.spawn.z", l.getZ());
        yml.set("boss.spawn.yaw", l.getYaw());
        yml.set("boss.spawn.pitch", l.getPitch());
        try { yml.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    /** Moi wave: danh sach "LOAI:SO_LUONG" hoac "mythic:Ten:SO_LUONG". */
    public java.util.List<java.util.List<String>> waves() {
        java.util.List<java.util.List<String>> out = new java.util.ArrayList<>();
        for (java.util.Map<?, ?> m : yml.getMapList("waves")) {
            java.util.List<String> w = new java.util.ArrayList<>();
            if (m.get("mobs") instanceof java.util.List<?> l) for (Object o : l) w.add(String.valueOf(o));
            if (!w.isEmpty()) out.add(w);
        }
        return out;
    }

    public void setSpawn(Location l) {
        yml.set("spawn.world", l.getWorld().getName());
        yml.set("spawn.x", l.getX());
        yml.set("spawn.y", l.getY());
        yml.set("spawn.z", l.getZ());
        yml.set("spawn.yaw", l.getYaw());
        yml.set("spawn.pitch", l.getPitch());
        try { yml.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}
