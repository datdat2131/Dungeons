package com.astrax.dungeon.mob;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/** Spawn quai vanilla hoac MythicMobs ("mythic:Ten"), gan PDC de don dep khi reset. */
public final class MobSpawner {
    private final AstraXDungeon plugin;

    public MobSpawner(AstraXDungeon plugin) { this.plugin = plugin; }

    public boolean isTagged(Entity e) {
        var pdc = e.getPersistentDataContainer();
        return pdc.has(plugin.mobKey(), PersistentDataType.BYTE) || pdc.has(plugin.bossKey(), PersistentDataType.BYTE);
    }

    public void spawn(String spec, Location loc, boolean elite, int playerCount, Consumer<LivingEntity> callback) {
        if (spec.toLowerCase().startsWith("mythic:")) {
            if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
                spawnMythic(spec.substring(7), loc, elite, playerCount, callback);
                return;
            }
            plugin.getLogger().warning("MythicMobs chua cai, dung ZOMBIE thay cho " + spec);
            spec = "ZOMBIE";
        }
        EntityType type;
        try { type = EntityType.valueOf(spec.toUpperCase()); } catch (IllegalArgumentException ex) { type = EntityType.ZOMBIE; }
        if (!type.isSpawnable() || type.getEntityClass() == null || !LivingEntity.class.isAssignableFrom(type.getEntityClass())) {
            type = EntityType.ZOMBIE;
        }
        Entity ent = loc.getWorld().spawnEntity(loc, type);
        if (ent instanceof LivingEntity le) {
            prepare(le, elite, playerCount);
            callback.accept(le);
        } else {
            ent.remove();
        }
    }

    /** Best-effort: goi lenh /mm roi bat thuc the moi xuat hien gan vi tri. */
    private void spawnMythic(String name, Location loc, boolean elite, int playerCount, Consumer<LivingEntity> cb) {
        World w = loc.getWorld();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mm mobs spawn " + name + " 1 " + w.getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (LivingEntity e : w.getNearbyLivingEntities(loc, 3)) {
                if (e instanceof Player || e instanceof ArmorStand || isTagged(e)) continue;
                prepare(e, elite, playerCount);
                cb.accept(e);
                return;
            }
        }, 2L);
    }

    private void prepare(LivingEntity e, boolean elite, int playerCount) {
        e.getPersistentDataContainer().set(plugin.mobKey(), PersistentDataType.BYTE, (byte) 1);
        if (e instanceof Mob m) m.setRemoveWhenFarAway(false);
        e.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));

        double perPlayer = plugin.getConfig().getDouble("mobs.hp-per-player", 0.1);
        double mult = (1 + perPlayer * (Math.max(1, playerCount) - 1)) * (elite ? 3.0 : 1.0);
        AttributeInstance hp = e.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null && mult != 1.0) {
            hp.setBaseValue(Math.min(1024, hp.getBaseValue() * mult));
            e.setHealth(hp.getValue());
        }
        if (elite) {
            e.getPersistentDataContainer().set(plugin.eliteKey(), PersistentDataType.BYTE, (byte) 1);
            e.customName(Text.c(plugin.messages().raw("elite-name")));
            e.setCustomNameVisible(true);
            e.setGlowing(true);
        }
    }

    /** Tim cho dung gan tam: 2 block khong khi, ben duoi la block ran. */
    public static Location safeNear(Location center, int radius) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        World w = center.getWorld();
        for (int i = 0; i < 12; i++) {
            double dx = r.nextDouble(-radius, radius), dz = r.nextDouble(-radius, radius);
            if (dx * dx + dz * dz < 16) continue;
            int bx = center.getBlockX() + (int) dx, bz = center.getBlockZ() + (int) dz;
            for (int dy = 2; dy >= -2; dy--) {
                int by = center.getBlockY() + dy;
                var feet = w.getBlockAt(bx, by, bz);
                var head = w.getBlockAt(bx, by + 1, bz);
                var below = w.getBlockAt(bx, by - 1, bz);
                if (feet.isPassable() && !feet.isLiquid() && head.isPassable() && !below.isPassable()) {
                    return new Location(w, bx + 0.5, by, bz + 0.5);
                }
            }
        }
        return center.clone();
    }
}
