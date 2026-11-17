package com.astrax.dungeon.boss;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonManager;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.dungeon.Modifier;
import com.astrax.dungeon.mob.MobSpawner;
import com.astrax.dungeon.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Boss dung HP ao (virtual HP): HP that nam trong controller nen khong bi gioi han 1024 cua server,
 * tu dong scale theo so nguoi, chong chet/phat thuong nhieu lan nho co co {@code dead}.
 */
public final class BossController {
    private final AstraXDungeon plugin;
    private final DungeonManager dm;
    private final DungeonSession session;
    private final BossDefinition def;
    private final Map<String, Long> cooldowns = new HashMap<>();

    private LivingEntity entity;
    private BossBar bar;
    private BukkitTask task;
    private Location lastLoc;
    private double max, hp, shieldReduction = 0.7;
    private int lastPlayers, phase = -1;
    private boolean dead, summoning, shielded, enraged;
    private long shieldUntil, nextCast, summonAt;

    public BossController(AstraXDungeon plugin, DungeonManager dm, DungeonSession session, BossDefinition def) {
        this.plugin = plugin;
        this.dm = dm;
        this.session = session;
        this.def = def;
    }

    public String name() { return def.displayName(); }
    public boolean isEntity(Entity e) { return entity != null && e.getUniqueId().equals(entity.getUniqueId()); }

    // ---------- vong doi ----------
    public void start(Location loc) {
        int n = Math.max(1, session.players().size());
        lastPlayers = n;
        max = def.maxHealth() * (1 + def.hpPerPlayer() * (n - 1));
        hp = max;
        bar = BossBar.bossBar(Text.c(def.displayName()), 1f, def.barColor(), BossBar.Overlay.NOTCHED_10);
        summon(loc);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        Bukkit.broadcast(plugin.messages().get("boss-spawn", "boss", def.displayName()));
    }

    private void summon(Location loc) {
        summoning = true;
        summonAt = System.currentTimeMillis();
        lastLoc = loc.clone();
        String spec = (def.isMythic() && Bukkit.getPluginManager().isPluginEnabled("MythicMobs"))
                ? "mythic:" + def.mythicId() : def.entityType();
        plugin.spawner().spawn(spec, loc, false, 1, e -> {
            summoning = false;
            if (dead) { e.remove(); return; }
            setup(e);
        });
    }

    private void setup(LivingEntity e) {
        entity = e;
        e.getPersistentDataContainer().set(plugin.bossKey(), PersistentDataType.BYTE, (byte) 1);
        e.customName(Text.c(def.displayName()));
        e.setCustomNameVisible(true);
        e.setGlowing(true);
        if (!def.isMythic()) {
            setAttr(e, Attribute.MAX_HEALTH, 1024);
            e.setHealth(1024);
            setAttr(e, Attribute.ATTACK_DAMAGE, def.damage());
            setAttr(e, Attribute.SCALE, def.scale());
        }
        if (session.modifier() == Modifier.FAST_BOSS) mulAttr(e, Attribute.MOVEMENT_SPEED, 1.25);
        if (enraged) boost(e);
    }

    private void tick() {
        if (dead) return;
        long now = System.currentTimeMillis();

        if (entity == null || !entity.isValid()) {
            if (summoning) {
                if (now - summonAt > 5000) summoning = false; else return;
            }
            entity = null;
            summon(lastLoc); // Boss bi mat (void, despawn...) => hoi sinh, giu nguyen HP
            return;
        }
        lastLoc = entity.getLocation();

        // scale HP theo so nguoi (giu nguyen %)
        int n = Math.max(1, session.players().size());
        if (n != lastPlayers) {
            double newMax = def.maxHealth() * (1 + def.hpPerPlayer() * (n - 1));
            hp = hp * newMax / max;
            max = newMax;
            lastPlayers = n;
        }

        List<Player> ps = online();
        updateBar();

        double pct = hp / max * 100.0;
        int idx = -1;
        List<BossDefinition.Phase> phs = def.phases();
        for (int i = 0; i < phs.size(); i++) if (pct <= phs.get(i).at()) idx = i;
        if (idx > phase) enterPhase(idx);

        if (shielded && now > shieldUntil) {
            shielded = false;
            Bukkit.broadcast(plugin.messages().get("boss-shield-down", "boss", def.displayName()));
        }

        Player nearest = nearest(entity.getLocation(), ps);
        if (nearest != null && entity instanceof Mob m) m.setTarget(nearest);

        if (nearest != null && phase >= 0 && now >= nextCast) castRandom(now);
    }

    private void enterPhase(int idx) {
        phase = idx;
        BossDefinition.Phase p = def.phases().get(idx);
        if (idx > 0) {
            Bukkit.broadcast(plugin.messages().get("boss-phase", "boss", def.displayName(), "n", idx + 1));
        }
        if (!p.message().isEmpty()) Bukkit.broadcast(Text.c(p.message()));
        if (p.enrage() && !enraged) {
            enraged = true;
            boost(entity);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3f, 0.8f);
            Bukkit.broadcast(plugin.messages().get("boss-enrage", "boss", def.displayName()));
        }
    }

    private void boost(LivingEntity e) {
        if (!def.isMythic()) mulAttr(e, Attribute.ATTACK_DAMAGE, 1.5);
        mulAttr(e, Attribute.MOVEMENT_SPEED, 1.3);
    }

    // ---------- ky nang ----------
    private void castRandom(long now) {
        List<String> ready = new ArrayList<>();
        for (String s : def.phases().get(phase).skills()) {
            if (cooldowns.getOrDefault(s, 0L) <= now && def.skill(s) != null) ready.add(s);
        }
        if (ready.isEmpty()) return;
        String name = ready.get(ThreadLocalRandom.current().nextInt(ready.size()));
        ConfigurationSection sec = def.skill(name);
        cooldowns.put(name, now + (long) (sec.getDouble("cooldown", 10) * 1000));
        nextCast = now + 3000;
        cast(name, sec);
    }

    private void cast(String name, ConfigurationSection s) {
        double dmg = s.getDouble("damage", 10);
        int warn = s.getInt("warn", 40);
        switch (name) {
            case "ground_slam" -> blast(entity.getLocation(), s.getDouble("radius", 6), dmg, warn, Particle.FLAME);
            case "dark_explosion" -> blast(entity.getLocation(), s.getDouble("radius", 8), dmg, warn, Particle.SOUL_FIRE_FLAME);
            case "meteor_strike" -> {
                List<Player> ps = online();
                if (ps.isEmpty()) return;
                Location t = ps.get(ThreadLocalRandom.current().nextInt(ps.size())).getLocation();
                blast(t, s.getDouble("radius", 4), dmg, warn, Particle.FLAME);
            }
            case "energy_wave" -> wave(s.getDouble("length", 12), dmg, warn);
            case "summon_minions" -> {
                String mob = s.getString("mob", "ZOMBIE");
                int amount = s.getInt("amount", 4);
                for (int i = 0; i < amount; i++) {
                    Location l = MobSpawner.safeNear(entity.getLocation(), 6);
                    plugin.spawner().spawn(mob, l, false, lastPlayers, e -> { });
                }
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 2f, 1f);
            }
            case "shield" -> {
                shieldReduction = s.getDouble("reduction", 0.7);
                double dur = s.getDouble("duration", 8);
                shielded = true;
                shieldUntil = System.currentTimeMillis() + (long) (dur * 1000);
                entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2f, 1f);
                Bukkit.broadcast(plugin.messages().get("boss-shield", "boss", def.displayName(), "sec", (int) dur));
            }
            default -> plugin.getLogger().warning("Ky nang chua ho tro: " + name);
        }
    }

    /** Don vung tron: canh bao bang vong hat roi moi no, de nguoi choi kip ne. */
    private void blast(Location center, double radius, double dmg, int warn, Particle ring) {
        final World w = center.getWorld();
        final Location c = center.clone();
        w.playSound(c, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2f, 1f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (dead) { cancel(); return; }
                if (t >= warn) {
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, c, 1);
                    w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
                    for (Player p : online()) {
                        if (p.getWorld().equals(w) && p.getLocation().distanceSquared(c) <= radius * radius) hurt(p, dmg);
                    }
                    cancel();
                    return;
                }
                for (int i = 0; i < 32; i++) {
                    double ang = 2 * Math.PI * i / 32;
                    w.spawnParticle(ring, c.getX() + Math.cos(ang) * radius, c.getY() + 0.2,
                            c.getZ() + Math.sin(ang) * radius, 1, 0, 0, 0, 0);
                }
                t += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    /** Song nang luong: vach canh bao theo huong nguoi choi gan nhat, sau do quet. */
    private void wave(double length, double dmg, int warn) {
        List<Player> ps = online();
        final Location o = entity.getLocation().add(0, 1, 0);
        Player target = nearest(o, ps);
        if (target == null) return;
        final Vector dir = target.getLocation().add(0, 1, 0).toVector().subtract(o.toVector()).normalize();
        final World w = o.getWorld();
        w.playSound(o, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2f, 1.4f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (dead) { cancel(); return; }
                if (t >= warn) {
                    for (int i = 1; i <= length; i++) {
                        w.spawnParticle(Particle.SWEEP_ATTACK, o.clone().add(dir.clone().multiply(i)), 1);
                    }
                    w.playSound(o, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2f, 0.7f);
                    for (Player p : online()) {
                        if (!p.getWorld().equals(w)) continue;
                        Vector v = p.getLocation().add(0, 1, 0).toVector().subtract(o.toVector());
                        double proj = v.dot(dir);
                        if (proj >= 0 && proj <= length && v.clone().subtract(dir.clone().multiply(proj)).length() <= 2.0) {
                            hurt(p, dmg);
                        }
                    }
                    cancel();
                    return;
                }
                for (int i = 1; i <= length; i++) {
                    w.spawnParticle(Particle.CRIT, o.clone().add(dir.clone().multiply(i)), 1, 0, 0, 0, 0);
                }
                t += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void hurt(Player p, double d) {
        if (entity != null && entity.isValid()) p.damage(d, entity); else p.damage(d);
    }

    // ---------- sat thuong len Boss ----------
    public void damage(Player p, double dmg) {
        if (dead) return;
        if (shielded) dmg *= (1 - shieldReduction);
        hp -= dmg;
        session.addBossDamage(p.getUniqueId(), dmg);
        session.addPoints(p.getUniqueId(), dmg * 0.05);
        if (hp <= 0) { hp = 0; die(); }
    }

    /** Chi chay 1 lan nho co {@code dead} => khong phat thuong nhieu lan. */
    public void die() {
        if (dead) return;
        dead = true;
        if (task != null) task.cancel();
        hideBar();
        if (entity != null && entity.isValid()) {
            entity.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, entity.getLocation(), 3);
            entity.remove();
        }
        Bukkit.broadcast(plugin.messages().get("boss-dead", "boss", def.displayName()));
        dm.onBossKilled(session);
    }

    /** Don dep khi reset Dungeon, khong phat thuong. */
    public void cleanup() {
        dead = true;
        if (task != null) task.cancel();
        hideBar();
        if (entity != null && entity.isValid()) entity.remove();
    }

    // ---------- tien ich ----------
    private List<Player> online() {
        List<Player> ps = new ArrayList<>();
        for (UUID id : session.players()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && !p.isDead()) ps.add(p);
        }
        return ps;
    }

    private Player nearest(Location from, List<Player> ps) {
        Player best = null;
        double bd = Double.MAX_VALUE;
        for (Player p : ps) {
            if (!p.getWorld().equals(from.getWorld())) continue;
            double d = p.getLocation().distanceSquared(from);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    private void updateBar() {
        bar.progress((float) Math.max(0, Math.min(1, hp / max)));
        bar.name(Text.c(def.displayName() + " &7- &c" + (int) Math.ceil(hp) + "&7/&c" + (int) max
                + (shielded ? " &b[KHIEN]" : "")));
        for (Player p : online()) p.showBossBar(bar);
    }

    private void hideBar() {
        if (bar == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
    }

    private static void setAttr(LivingEntity e, Attribute a, double v) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(v);
    }

    private static void mulAttr(LivingEntity e, Attribute a, double m) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(i.getBaseValue() * m);
    }
}
