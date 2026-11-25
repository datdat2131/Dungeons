package com.astrax.dungeon.mob;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.tier.Tier;
import com.astrax.dungeon.util.EffectUtil;
import com.astrax.dungeon.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Spawn quai vanilla hoac MythicMobs ("mythic:Ten"). Quai theo Tier/Wave duoc nhan mau, sat thuong, toc do,
 * trang bi giap, hieu ung va dac tinh (traits). Moi quai gan PDC de don dep khi reset.
 */
public final class MobSpawner {
    // set, mu, ao, quan, giay, kiem
    private static final String[][] GEAR = {
        {"LEATHER", "LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS", "STONE_SWORD"},
        {"CHAINMAIL", "CHAINMAIL_HELMET", "CHAINMAIL_CHESTPLATE", "CHAINMAIL_LEGGINGS", "CHAINMAIL_BOOTS", "IRON_SWORD"},
        {"IRON", "IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS", "IRON_SWORD"},
        {"DIAMOND", "DIAMOND_HELMET", "DIAMOND_CHESTPLATE", "DIAMOND_LEGGINGS", "DIAMOND_BOOTS", "DIAMOND_SWORD"},
        {"NETHERITE", "NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS", "NETHERITE_SWORD"},
    };

    private final AstraXDungeon plugin;

    public MobSpawner(AstraXDungeon plugin) { this.plugin = plugin; }

    public boolean isTagged(Entity e) {
        var pdc = e.getPersistentDataContainer();
        return pdc.has(plugin.mobKey(), PersistentDataType.BYTE) || pdc.has(plugin.bossKey(), PersistentDataType.BYTE);
    }

    /** Spawn khong scale (dung cho Boss). */
    public void spawnRaw(String spec, Location loc, Consumer<LivingEntity> cb) {
        create(spec, loc, e -> { tag(e); cb.accept(e); });
    }

    /** Spawn quai da scale theo Tier + dot. */
    public void spawn(MobSpec ms, Location loc, int players, Tier tier, int wave, Consumer<LivingEntity> cb) {
        create(ms.type(), loc, e -> { tag(e); scale(e, ms, players, tier, wave); cb.accept(e); });
    }

    // ---------- tao thuc the ----------
    private void create(String spec, Location loc, Consumer<LivingEntity> cb) {
        if (spec.toLowerCase().startsWith("mythic:")) {
            if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
                createMythic(spec.substring(7), loc, cb);
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
        if (ent instanceof LivingEntity le) cb.accept(le); else ent.remove();
    }

    /** Best-effort: goi lenh /mm roi bat thuc the moi xuat hien gan vi tri. */
    private void createMythic(String name, Location loc, Consumer<LivingEntity> cb) {
        World w = loc.getWorld();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mm mobs spawn " + name + " 1 " + w.getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (LivingEntity e : w.getNearbyLivingEntities(loc, 3)) {
                if (e instanceof Player || e instanceof ArmorStand || isTagged(e)) continue;
                cb.accept(e);
                return;
            }
        }, 2L);
    }

    private void tag(LivingEntity e) {
        e.getPersistentDataContainer().set(plugin.mobKey(), PersistentDataType.BYTE, (byte) 1);
        if (e instanceof Mob m) m.setRemoveWhenFarAway(false);
        e.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
    }

    // ---------- scale theo Tier ----------
    private void scale(LivingEntity e, MobSpec ms, int players, Tier tier, int wave) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<String> traits = new ArrayList<>(ms.traits());
        if (r.nextDouble() < tier.traitChance()) addRandomTrait(traits, r);
        if (tier.traitChance() > 0.5 && r.nextDouble() < 0.35) addRandomTrait(traits, r);

        double perPlayer = plugin.getConfig().getDouble("mobs.hp-per-player", 0.1);
        int n = Math.max(1, players);
        double w = Math.max(0, wave - 1);
        double hpMult = tier.mobHealthMult() * (1 + tier.waveHealthGrowth() * w) * (1 + perPlayer * (n - 1))
                * ms.hpMult() * (ms.elite() ? 3.0 : 1.0) * (traits.contains("tank") ? 1.8 : 1.0);
        double dmgMult = tier.mobDamageMult() * (1 + tier.waveDamageGrowth() * w)
                * (ms.elite() ? 1.3 : 1.0) * (traits.contains("brute") ? 1.5 : 1.0);
        double speedMult = Math.min(2.0, tier.mobSpeedMult() * (traits.contains("swift") ? 1.3 : 1.0));

        AttributeInstance hp = e.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(Math.min(1024, hp.getBaseValue() * hpMult));
            e.setHealth(hp.getValue());
        }
        AttributeInstance sp = e.getAttribute(Attribute.MOVEMENT_SPEED);
        if (sp != null && speedMult != 1.0) sp.setBaseValue(sp.getBaseValue() * speedMult);

        var pdc = e.getPersistentDataContainer();
        pdc.set(plugin.dmgKey(), PersistentDataType.DOUBLE, dmgMult);
        if (!traits.isEmpty()) pdc.set(plugin.traitKey(), PersistentDataType.STRING, String.join(",", traits));
        if (ms.elite()) {
            pdc.set(plugin.eliteKey(), PersistentDataType.BYTE, (byte) 1);
            e.setGlowing(true);
        }

        if (traits.contains("tank")) EffectUtil.apply(e, "RESISTANCE:0", -1);
        if (traits.contains("shielded")) e.setAbsorptionAmount(Math.min(1024, e.getHealth() * 0.5));
        for (String eff : tier.mobEffects()) EffectUtil.apply(e, eff, -1);

        // ky nang chu dong: tu mob-pool + ngau nhien theo tier.mob.skill-chance
        List<String> skills = new ArrayList<>(ms.skills());
        List<String> skillPool = plugin.mobSkills().randomPool();
        if (!skillPool.isEmpty() && r.nextDouble() < tier.skillChance()) {
            String sk = skillPool.get(r.nextInt(skillPool.size()));
            if (!skills.contains(sk)) skills.add(sk);
        }
        plugin.mobSkills().register(e, skills);

        if (ms.elite() || !traits.isEmpty() || ms.name() != null) {
            StringBuilder sb = new StringBuilder();
            if (ms.elite()) sb.append("&6★ ");
            for (String t : traits) sb.append("&c[").append(plugin.messages().raw("traits." + t)).append("] ");
            sb.append("&f").append(ms.name() != null ? ms.name() : pretty(e.getType().name()));
            e.customName(Text.c(sb.toString()));
            e.setCustomNameVisible(true);
        }
        gear(e, tier, r);
    }

    private static void addRandomTrait(List<String> traits, ThreadLocalRandom r) {
        for (int tries = 0; tries < 6; tries++) {
            String t = Traits.ALL.get(r.nextInt(Traits.ALL.size()));
            if (!traits.contains(t)) { traits.add(t); return; }
        }
    }

    private static String pretty(String enumName) {
        StringBuilder sb = new StringBuilder();
        for (String part : enumName.toLowerCase().split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static boolean humanoid(LivingEntity e) {
        return e instanceof Zombie || e instanceof AbstractSkeleton || e instanceof Piglin
                || e instanceof PiglinBrute || e instanceof Illager;
    }

    private void gear(LivingEntity e, Tier tier, ThreadLocalRandom r) {
        if (!humanoid(e)) return;
        String[] set = null;
        for (String[] g : GEAR) if (g[0].equalsIgnoreCase(tier.gear())) set = g;
        EntityEquipment eq = e.getEquipment();
        if (set == null || eq == null) return;
        double ch = tier.gearChance();
        if (r.nextDouble() < ch) eq.setHelmet(item(set[1]));
        if (r.nextDouble() < ch) eq.setChestplate(item(set[2]));
        if (r.nextDouble() < ch) eq.setLeggings(item(set[3]));
        if (r.nextDouble() < ch) eq.setBoots(item(set[4]));
        if ((e instanceof Zombie || e instanceof WitherSkeleton || e instanceof PiglinBrute) && r.nextDouble() < ch) {
            eq.setItemInMainHand(item(set[5]));
        }
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
    }

    private static ItemStack item(String material) {
        Material m = Material.matchMaterial(material);
        return new ItemStack(m == null ? Material.AIR : m);
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
