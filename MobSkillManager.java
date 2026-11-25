package com.astrax.dungeon.mob;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.dungeon.DungeonState;
import com.astrax.dungeon.tier.Tier;
import com.astrax.dungeon.util.EffectUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ky nang chu dong cho QUAI THUONG (cau hinh trong mob-skills.yml) - Boss cung dung duoc cac loai nay.
 * Loai: leap, fireball, volley, blast, heal, buff, self_buff, summon, teleport. Moi don deu co canh bao/hieu ung bao truoc.
 * Quai gay sat thuong bang p.damage(dmg, quai) nen duoc nhan he so sat thuong cua Tier trong CombatListener.
 */
public final class MobSkillManager {
    private final AstraXDungeon plugin;
    private final Map<String, ConfigurationSection> skills = new LinkedHashMap<>();
    private final List<String> randomPool = new ArrayList<>();
    private final Map<UUID, List<String>> tracked = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Long> nextCast = new HashMap<>();
    private BukkitTask task;

    public MobSkillManager(AstraXDungeon plugin) { this.plugin = plugin; }

    public void load() {
        skills.clear();
        randomPool.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "mob-skills.yml"));
        ConfigurationSection sec = y.getConfigurationSection("skills");
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(k);
            if (s == null) continue;
            skills.put(k, s);
            if (s.getBoolean("random", true)) randomPool.add(k);
        }
    }

    public void start() {
        load();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        clear();
    }

    public void clear() {
        tracked.clear();
        cooldowns.clear();
        nextCast.clear();
    }

    public List<String> randomPool() { return randomPool; }

    /** Dang ky ky nang cho 1 con quai (bo qua ten khong ton tai). */
    public void register(LivingEntity e, List<String> names) {
        List<String> ok = new ArrayList<>();
        for (String n : names) if (skills.containsKey(n) && !ok.contains(n)) ok.add(n);
        if (!ok.isEmpty()) tracked.put(e.getUniqueId(), ok);
    }

    // ---------- vong lap ----------
    private List<Player> players(DungeonSession s) {
        List<Player> out = new ArrayList<>();
        for (UUID id : s.players()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && !p.isDead() && p.getGameMode() != GameMode.SPECTATOR
                    && !s.downed().contains(id)) out.add(p);
        }
        return out;
    }

    private void tick() {
        if (tracked.isEmpty()) return;
        DungeonSession s = plugin.dungeons().active();
        if (s == null || (s.state() != DungeonState.RUNNING && s.state() != DungeonState.BOSS)) return;
        List<Player> ps = players(s);
        if (ps.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (UUID id : new ArrayList<>(tracked.keySet())) { // ban sao: cast co the them quai moi
            Entity ent = Bukkit.getEntity(id);
            if (!(ent instanceof LivingEntity mob) || !mob.isValid() || mob.isDead()) {
                tracked.remove(id);
                cooldowns.remove(id);
                nextCast.remove(id);
                continue;
            }
            if (now < nextCast.getOrDefault(id, 0L)) continue;
            Player target = nearest(mob.getLocation(), ps);
            if (target == null) continue;

            double dist = mob.getLocation().distance(target.getLocation());
            List<String> names = new ArrayList<>(tracked.get(id));
            Collections.shuffle(names);
            Map<String, Long> cd = cooldowns.computeIfAbsent(id, k -> new HashMap<>());
            for (String name : names) {
                ConfigurationSection sec = skills.get(name);
                if (sec == null || cd.getOrDefault(name, 0L) > now) continue;
                if (dist < sec.getDouble("min-range", 0) || dist > sec.getDouble("range", 20)) continue;
                if (ThreadLocalRandom.current().nextDouble() > sec.getDouble("chance", 1.0)) continue;
                cd.put(name, now + (long) (sec.getDouble("cooldown", 8) * 1000));
                nextCast.put(id, now + 2000);
                cast(mob, sec.getString("type", name), sec, target, 1.0);
                break;
            }
        }
    }

    private static Player nearest(Location from, List<Player> ps) {
        Player best = null;
        double bd = 30 * 30;
        for (Player p : ps) {
            if (!p.getWorld().equals(from.getWorld())) continue;
            double d = p.getLocation().distanceSquared(from);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    // ---------- thuc thi ky nang ----------
    /** @return true neu 'type' la loai ky nang duoc ho tro (Boss goi ham nay cho cac loai moi). */
    public boolean cast(LivingEntity caster, String type, ConfigurationSection s, Player target, double dmgMult) {
        switch (type) {
            case "leap" -> leap(caster, target, s);
            case "fireball" -> fireball(caster, target, s);
            case "volley" -> volley(caster, target, s);
            case "blast", "shockwave" -> blast(caster, s, dmgMult);
            case "heal" -> heal(caster, s);
            case "buff" -> buff(caster, s);
            case "self_buff" -> EffectUtil.apply(caster, s.getString("effect", "RESISTANCE:1:6"), 6);
            case "summon" -> summon(caster, s);
            case "teleport", "blink" -> blink(caster, target, s);
            default -> { return false; }
        }
        return true;
    }

    public static Particle particle(String n) {
        try {
            Particle p = Particle.valueOf(n.toUpperCase());
            return p.getDataType() == Void.class ? p : Particle.CRIT;
        } catch (IllegalArgumentException e) {
            return Particle.CRIT;
        }
    }

    private void leap(LivingEntity caster, Player target, ConfigurationSection s) {
        Vector v = target.getLocation().toVector().subtract(caster.getLocation().toVector());
        v.setY(0);
        if (v.lengthSquared() < 0.01) return;
        v.normalize().multiply(s.getDouble("strength", 1.2));
        v.setY(s.getDouble("lift", 0.5));
        caster.setVelocity(v);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.8f);
        caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation(), 12, 0.4, 0.1, 0.4, 0.02);
    }

    private void fireball(LivingEntity caster, Player target, ConfigurationSection s) {
        final int count = Math.max(1, s.getInt("count", 1));
        final int warn = s.getInt("warn", 10);
        World w = caster.getWorld();
        w.playSound(caster.getLocation(), Sound.ENTITY_GHAST_WARN, 1.5f, 1.0f);
        w.spawnParticle(Particle.FLAME, caster.getEyeLocation(), 15, 0.3, 0.3, 0.3, 0.02);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!caster.isValid() || !target.isOnline()) return;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                Vector dir = target.getEyeLocation().toVector().subtract(caster.getEyeLocation().toVector());
                if (dir.lengthSquared() < 0.01) return;
                dir.normalize().add(new Vector(r.nextDouble(-0.08, 0.08), r.nextDouble(-0.04, 0.04), r.nextDouble(-0.08, 0.08)));
                SmallFireball fb = caster.launchProjectile(SmallFireball.class, dir.multiply(1.3));
                fb.setIsIncendiary(false);
                fb.setYield(0f);
            }
        }, warn);
    }

    private void volley(LivingEntity caster, Player target, ConfigurationSection s) {
        final int count = Math.max(1, s.getInt("count", 5));
        final double spread = s.getDouble("spread", 0.12);
        final int warn = s.getInt("warn", 10);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.5f, 0.7f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!caster.isValid() || !target.isOnline()) return;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            Vector base = target.getEyeLocation().toVector().subtract(caster.getEyeLocation().toVector());
            if (base.lengthSquared() < 0.01) return;
            base.normalize();
            for (int i = 0; i < count; i++) {
                Vector v = base.clone().add(new Vector(r.nextDouble(-spread, spread),
                        r.nextDouble(-spread / 2, spread / 2), r.nextDouble(-spread, spread))).normalize().multiply(2.0);
                Arrow a = caster.launchProjectile(Arrow.class, v);
                a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            }
        }, warn);
    }

    /** Don vung tron quanh quai: vong hat canh bao roi moi no. */
    private void blast(LivingEntity caster, ConfigurationSection s, double dmgMult) {
        final double radius = s.getDouble("radius", 4);
        final double dmg = s.getDouble("damage", 5) * dmgMult;
        final int warn = s.getInt("warn", 20);
        final Particle ring = particle(s.getString("particle", "CRIT"));
        final String effect = s.getString("effect", "");
        final World w = caster.getWorld();
        w.playSound(caster.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.2f, 1.5f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!caster.isValid()) { cancel(); return; }
                Location c = caster.getLocation();
                if (t >= warn) {
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, c, 1);
                    w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.2f);
                    DungeonSession sess = plugin.dungeons().active();
                    if (sess != null) {
                        for (Player p : players(sess)) {
                            if (p.getWorld().equals(w) && p.getLocation().distanceSquared(c) <= radius * radius) {
                                p.damage(dmg, caster);
                                EffectUtil.apply(p, effect, 5);
                            }
                        }
                    }
                    cancel();
                    return;
                }
                for (int i = 0; i < 24; i++) {
                    double ang = 2 * Math.PI * i / 24;
                    w.spawnParticle(ring, c.getX() + Math.cos(ang) * radius, c.getY() + 0.2,
                            c.getZ() + Math.sin(ang) * radius, 1, 0, 0, 0, 0);
                }
                t += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void heal(LivingEntity caster, ConfigurationSection s) {
        double radius = s.getDouble("radius", 8);
        double pct = s.getDouble("percent", 0.15);
        for (LivingEntity le : caster.getWorld().getNearbyLivingEntities(caster.getLocation(), radius)) {
            if (le instanceof Player || !plugin.spawner().isTagged(le)) continue;
            AttributeInstance max = le.getAttribute(Attribute.MAX_HEALTH);
            if (max == null) continue;
            le.setHealth(Math.min(max.getValue(), le.getHealth() + max.getValue() * pct));
            le.getWorld().spawnParticle(Particle.HEART, le.getLocation().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 0);
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.4f);
    }

    private void buff(LivingEntity caster, ConfigurationSection s) {
        double radius = s.getDouble("radius", 10);
        String effect = s.getString("effect", "SPEED:1:8");
        for (LivingEntity le : caster.getWorld().getNearbyLivingEntities(caster.getLocation(), radius)) {
            if (le instanceof Player || !plugin.spawner().isTagged(le)) continue;
            EffectUtil.apply(le, effect, 8);
            le.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, le.getLocation().add(0, 1.5, 0), 3, 0.3, 0.2, 0.3, 0);
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.0f);
    }

    private void summon(LivingEntity caster, ConfigurationSection s) {
        DungeonSession sess = plugin.dungeons().active();
        if (sess == null) return;
        Tier tier = sess.tier();
        if (plugin.dungeons().mobs().aliveCount() >= tier.mobCap()) return; // khong vuot gioi han quai
        String mob = s.getString("mob", "ZOMBIE");
        int amount = Math.max(1, s.getInt("amount", 2));
        double hp = s.getDouble("hp-mult", 0.6);
        int wave = Math.max(1, tier.waves() / 2);
        int n = Math.max(1, sess.players().size());
        for (int i = 0; i < amount; i++) {
            Location l = MobSpawner.safeNear(caster.getLocation(), 4);
            MobSpec ms = new MobSpec(mob, List.of(), false, hp, null);
            plugin.spawner().spawn(ms, l, n, tier, wave, e -> plugin.dungeons().mobs().track(e.getUniqueId()));
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 1.0f);
    }

    private void blink(LivingEntity caster, Player target, ConfigurationSection s) {
        final Location dest = MobSpawner.safeNear(target.getLocation(), 4);
        final int warn = s.getInt("warn", 10);
        World w = dest.getWorld();
        w.spawnParticle(Particle.PORTAL, dest, 25, 0.4, 0.8, 0.4, 0.1);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!caster.isValid()) return;
            caster.teleport(dest);
            w.spawnParticle(Particle.PORTAL, dest, 30, 0.4, 0.8, 0.4, 0.1);
            w.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.0f);
        }, warn);
    }
}
