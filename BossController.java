package com.astrax.dungeon.boss;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonManager;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.dungeon.Modifier;
import com.astrax.dungeon.mob.MobSpawner;
import com.astrax.dungeon.mob.MobSpec;
import com.astrax.dungeon.tier.Tier;
import com.astrax.dungeon.util.EffectUtil;
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
 * Boss dung HP ao (virtual HP) nen khong bi gioi han 1024 cua server; HP/sat thuong nhan he so Tier.
 * Ky nang dinh nghia hoan toan bang YAML (type: blast|wave|meteor|summon|shield|lightning|pull|teleport),
 * deu co canh bao truoc khi gay sat thuong. Chong chet/phat thuong nhieu lan nho co co {@code dead}.
 */
public final class BossController {
    private final AstraXDungeon plugin;
    private final DungeonManager dm;
    private final DungeonSession session;
    private final BossDefinition def;
    private final Tier tier;
    private final Map<String, Long> cooldowns = new HashMap<>();

    private LivingEntity entity;
    private BossBar bar;
    private BukkitTask task;
    private Location lastLoc;
    private double max, hp, shieldReduction = 0.7;
    private int lastPlayers, phase = -1;
    private boolean dead, summoning, shielded, enraged;
    private long shieldUntil, nextCast, summonAt, startMs;

    public BossController(AstraXDungeon plugin, DungeonManager dm, DungeonSession session, BossDefinition def) {
        this.plugin = plugin;
        this.dm = dm;
        this.session = session;
        this.def = def;
        this.tier = session.tier();
    }

    public String name() { return def.displayName(); }
    public boolean isEntity(Entity e) { return entity != null && e.getUniqueId().equals(entity.getUniqueId()); }

    private double calcMax(int n) {
        return def.maxHealth() * tier.bossHealthMult() * (1 + def.hpPerPlayer() * (n - 1));
    }

    // ---------- vong doi ----------
    public void start(Location loc) {
        int n = Math.max(1, session.players().size());
        lastPlayers = n;
        max = calcMax(n);
        hp = max;
        startMs = System.currentTimeMillis();
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
        plugin.spawner().spawnRaw(spec, loc, e -> {
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
            setAttr(e, Attribute.ATTACK_DAMAGE, def.damage() * tier.bossDamageMult());
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
            double newMax = calcMax(n);
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

        // danh qua lau => Boss noi dien
        int enrageSec = tier.bossEnrageSeconds();
        if (!enraged && enrageSec > 0 && (now - startMs) / 1000 >= enrageSec) {
            enraged = true;
            boost(entity);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3f, 0.6f);
            Bukkit.broadcast(plugin.messages().get("boss-enrage-timer", "boss", def.displayName()));
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
        // boss noi dien cast nhanh hon
        nextCast = now + (enraged ? 2000 : 3000);
        cast(name, sec);
    }

    private void cast(String name, ConfigurationSection s) {
        String type = s.getString("type", name);
        double dmg = s.getDouble("damage", 10) * tier.bossSkillDamageMult();
        int warn = s.getInt("warn", 40);
        Particle particle = particle(s.getString("particle", "FLAME"));
        String effect = s.getString("effect", "");

        switch (type) {
            case "ground_slam", "blast", "dark_explosion", "frost_nova" ->
                    blast(entity.getLocation(), s.getDouble("radius", 6), dmg, warn, particle, effect, false);
            case "meteor_strike", "meteor" -> {
                int count = Math.max(1, s.getInt("count", 1));
                double radius = s.getDouble("radius", 4);
                for (int i = 0; i < count; i++) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (dead) return;
                        List<Player> now = online();
                        if (now.isEmpty()) return;
                        Location t = now.get(ThreadLocalRandom.current().nextInt(now.size())).getLocation();
                        blast(t, radius, dmg, warn, particle, effect, false);
                    }, i * 8L);
                }
            }
            case "energy_wave", "wave" -> wave(s.getDouble("length", 12), dmg, warn, effect);
            case "summon_minions", "summon" -> {
                String mob = s.getString("mob", "ZOMBIE");
                int amount = s.getInt("amount", 4);
                boolean elite = s.getBoolean("elite", false);
                List<String> traits = new ArrayList<>();
                for (String t : s.getStringList("traits")) traits.add(t.toLowerCase());
                for (int i = 0; i < amount; i++) {
                    Location l = MobSpawner.safeNear(entity.getLocation(), 6);
                    MobSpec ms = new MobSpec(mob, traits, elite, 1.0, null);
                    plugin.spawner().spawn(ms, l, lastPlayers, tier, tier.waves(), e -> { });
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
            case "lightning_storm", "lightning" -> {
                int strikes = Math.max(1, s.getInt("strikes", 6));
                double radius = s.getDouble("radius", 3);
                for (int i = 0; i < strikes; i++) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (dead) return;
                        List<Player> now = online();
                        if (now.isEmpty()) return;
                        ThreadLocalRandom r = ThreadLocalRandom.current();
                        Location at = now.get(r.nextInt(now.size())).getLocation().add(r.nextDouble(-2, 2), 0, r.nextDouble(-2, 2));
                        blast(at, radius, dmg, warn, Particle.ELECTRIC_SPARK, effect, true);
                    }, i * 6L);
                }
            }
            case "gravity_pull", "pull" -> pull(s.getDouble("radius", 14), dmg, warn, s.getDouble("strength", 1.3));
            case "blink", "teleport" -> blink(s.getDouble("radius", 5), dmg, warn, effect);
            default -> {
                // cac loai ky nang dung chung voi quai thuong (leap, fireball, volley, heal, buff, self_buff...)
                Player tgt = nearest(entity.getLocation(), online());
                if (tgt != null && !plugin.mobSkills().cast(entity, type, s, tgt, tier.bossSkillDamageMult())) {
                    plugin.getLogger().warning("Ky nang chua ho tro: " + name + " (type=" + type + ")");
                }
            }
        }
    }

    private static Particle particle(String n) {
        try {
            Particle p = Particle.valueOf(n.toUpperCase());
            return p.getDataType() == Void.class ? p : Particle.FLAME;
        } catch (IllegalArgumentException e) {
            return Particle.FLAME;
        }
    }

    /** Don vung tron: canh bao bang vong hat roi moi no, de nguoi choi kip ne. */
    private void blast(Location center, double radius, double dmg, int warn, Particle ring, String effect, boolean lightning) {
        final World w = center.getWorld();
        final Location c = center.clone();
        w.playSound(c, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2f, 1f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (dead) { cancel(); return; }
                if (t >= warn) {
                    if (lightning) w.strikeLightningEffect(c);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, c, 1);
                    w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
                    for (Player p : online()) {
                        if (p.getWorld().equals(w) && p.getLocation().distanceSquared(c) <= radius * radius) {
                            hurt(p, dmg);
                            EffectUtil.apply(p, effect, 5);
                        }
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
    private void wave(double length, double dmg, int warn, String effect) {
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
                            EffectUtil.apply(p, effect, 5);
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

    /** Hut nguoi choi ve phia Boss sau khi canh bao. */
    private void pull(double radius, double dmg, int warn, double strength) {
        final World w = entity.getWorld();
        final Location c = entity.getLocation().clone();
        w.playSound(c, Sound.ENTITY_ENDERMAN_STARE, 2f, 0.5f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (dead) { cancel(); return; }
                if (t >= warn) {
                    Location now = entity != null && entity.isValid() ? entity.getLocation() : c;
                    for (Player p : online()) {
                        if (!p.getWorld().equals(w) || p.getLocation().distanceSquared(now) > radius * radius) continue;
                        Vector v = now.toVector().subtract(p.getLocation().toVector());
                        v.setY(0);
                        if (v.lengthSquared() > 0) v.normalize().multiply(strength);
                        v.setY(0.4);
                        p.setVelocity(v);
                        if (dmg > 0) hurt(p, dmg);
                    }
                    w.playSound(now, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                    cancel();
                    return;
                }
                for (int i = 0; i < 32; i++) {
                    double ang = 2 * Math.PI * i / 32;
                    w.spawnParticle(Particle.REVERSE_PORTAL, c.getX() + Math.cos(ang) * radius, c.getY() + 0.5,
                            c.getZ() + Math.sin(ang) * radius, 1, 0, 0, 0, 0);
                }
                t += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    /** Dich chuyen den canh 1 nguoi choi ngau nhien roi no nho. */
    private void blink(double radius, double dmg, int warn, String effect) {
        List<Player> ps = online();
        if (ps.isEmpty()) return;
        Player target = ps.get(ThreadLocalRandom.current().nextInt(ps.size()));
        final Location dest = MobSpawner.safeNear(target.getLocation(), 4);
        final World w = dest.getWorld();
        w.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (dead) { cancel(); return; }
                if (t >= warn) {
                    if (entity != null && entity.isValid()) {
                        entity.teleport(dest);
                        w.spawnParticle(Particle.PORTAL, dest, 40, 0.5, 1, 0.5, 0.1);
                        blast(entity.getLocation(), radius, dmg, 15, Particle.WITCH, effect, false);
                    }
                    cancel();
                    return;
                }
                w.spawnParticle(Particle.PORTAL, dest, 12, 0.5, 1, 0.5, 0.1);
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
        plugin.quests().progress(p.getUniqueId(), "BOSS_DAMAGE", "", (int) Math.ceil(dmg));
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
                + (shielded ? " &b[KHIEN]" : "") + (enraged ? " &4[NOI DIEN]" : "")));
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
