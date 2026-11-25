package com.astrax.dungeon.listener;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.boss.BossController;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.dungeon.Modifier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class CombatListener implements Listener {
    private final AstraXDungeon plugin;

    public CombatListener(AstraXDungeon plugin) { this.plugin = plugin; }

    /** Nguon sat thuong khong phai nguoi choi nhung gay hai cho quai khac: no, phep, doc, lua... */
    private static final Set<EntityDamageEvent.DamageCause> FRIENDLY_CAUSES = EnumSet.of(
            EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
            EntityDamageEvent.DamageCause.LIGHTNING, EntityDamageEvent.DamageCause.MAGIC,
            EntityDamageEvent.DamageCause.POISON, EntityDamageEvent.DamageCause.WITHER,
            EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.DRAGON_BREATH);

    private boolean tagged(Entity e) { return plugin.spawner().isTagged(e); }

    /** Sat thuong den tu 1 con quai khac (can chien, phi tieu, nanh Evoker). */
    private boolean mobSource(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent be)) return false;
        Entity d = be.getDamager();
        if (d instanceof Projectile pr && pr.getShooter() instanceof LivingEntity shooter) d = shooter;
        if (d instanceof EvokerFangs ef && ef.getOwner() != null) d = ef.getOwner();
        return d instanceof LivingEntity le && !(le instanceof Player) && tagged(le);
    }

    /** Quai cung phe khong duoc nham muc tieu nhau (Enderman/Endermite, Skeleton/Zombie...). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (e.getTarget() != null && tagged(e.getEntity()) && tagged(e.getTarget())) e.setCancelled(true);
    }

    /** Binh phep cua Witch khong lam hai quai khac. */
    @EventHandler
    public void onSplash(PotionSplashEvent e) {
        if (e.getPotion().getShooter() instanceof LivingEntity sh && tagged(sh)) {
            for (LivingEntity le : new ArrayList<>(e.getAffectedEntities())) {
                if (tagged(le)) e.setIntensity(le, 0);
            }
        }
    }

    private Player attacker(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent be)) return null;
        Entity d = be.getDamager();
        if (d instanceof Projectile pr && pr.getShooter() instanceof Player ps) return ps;
        return d instanceof Player p ? p : null;
    }

    /** Quai (tagged) gay sat thuong: truc tiep hoac qua phi tieu. */
    private LivingEntity mobAttacker(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent be)) return null;
        Entity d = be.getDamager();
        if (d instanceof Projectile pr && pr.getShooter() instanceof LivingEntity shooter) d = shooter;
        if (d instanceof LivingEntity le && !(le instanceof Player)
                && le.getPersistentDataContainer().has(plugin.mobKey(), PersistentDataType.BYTE)) return le;
        return null;
    }

    private String traits(Entity e) {
        String t = e.getPersistentDataContainer().get(plugin.traitKey(), PersistentDataType.STRING);
        return t == null ? "" : t;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        var dm = plugin.dungeons();

        // Quai KHONG duoc danh nhau (ban tien, no, phep, nanh...): huy sat thuong => khong bi "quay ra danh nhau"
        if (tagged(e.getEntity())) {
            if (mobSource(e)) { e.setCancelled(true); return; }
            if (attacker(e) == null && FRIENDLY_CAUSES.contains(e.getCause())) { e.setCancelled(true); return; }
        }

        // Damage len Boss: HP ao, chi nguoi trong Dungeon moi gay sat thuong duoc
        BossController bc = dm.bossOf(e.getEntity());
        if (bc != null) {
            Player src = attacker(e);
            if (src == null || !dm.isInDungeon(src.getUniqueId())) { e.setCancelled(true); return; }
            var cur = dm.active();
            if (cur != null) cur.touch(src.getUniqueId());
            double dmg = e.getFinalDamage();
            e.setDamage(0); // giu knockback/animation nhung khong tru HP that
            bc.damage(src, dmg);
            return;
        }

        // Nguoi choi trong Dungeon bi quai danh: nhan he so sat thuong + dac tinh
        if (e.getEntity() instanceof Player victim && dm.isInDungeon(victim.getUniqueId())) {
            LivingEntity mob = mobAttacker(e);
            if (mob != null) {
                Double mult = mob.getPersistentDataContainer().get(plugin.dmgKey(), PersistentDataType.DOUBLE);
                if (mult != null && mult != 1.0) e.setDamage(e.getDamage() * mult);
                onMobHit(e, mob, victim);
                return;
            }
        }

        // Danh quai Dungeon => ghi nhan hoat dong that + dac tinh thorns
        if (plugin.spawner().isTagged(e.getEntity())) {
            Player a = attacker(e);
            if (a != null && dm.active() != null && dm.isInDungeon(a.getUniqueId())) {
                dm.active().touch(a.getUniqueId());
                if (traits(e.getEntity()).contains("thorns")) {
                    a.damage(Math.max(1.0, e.getFinalDamage() * 0.25)); // phan don, khong de quy
                }
            }
            return;
        }

        // Chan PvP giua nguoi choi trong Dungeon
        if (e.getEntity() instanceof Player victim && dm.isInDungeon(victim.getUniqueId())) {
            Player src = attacker(e);
            if (src != null && dm.isInDungeon(src.getUniqueId())) e.setCancelled(true);
        }
    }

    private void onMobHit(EntityDamageEvent e, LivingEntity mob, Player victim) {
        String tr = traits(mob);
        if (tr.isEmpty()) return;
        for (String t : tr.split(",")) {
            switch (t) {
                case "vampire" -> {
                    AttributeInstance max = mob.getAttribute(Attribute.MAX_HEALTH);
                    if (max != null) mob.setHealth(Math.min(max.getValue(), mob.getHealth() + e.getFinalDamage() * 0.4));
                }
                case "venom" -> victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                case "frost" -> victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                case "wither" -> victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0));
                case "ignite" -> victim.setFireTicks(80);
                default -> { }
            }
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        var pdc = e.getEntity().getPersistentDataContainer();
        boolean mob = pdc.has(plugin.mobKey(), PersistentDataType.BYTE);
        boolean boss = pdc.has(plugin.bossKey(), PersistentDataType.BYTE);
        if (!mob && !boss) return;

        e.getDrops().clear();   // chong farm drop/exp ngoai he thong thuong
        e.setDroppedExp(0);

        DungeonSession s = plugin.dungeons().active();
        if (s == null) return;
        Player k = e.getEntity().getKiller();
        if (k != null && s.players().contains(k.getUniqueId())) {
            boolean elite = pdc.has(plugin.eliteKey(), PersistentDataType.BYTE);
            s.addPoints(k.getUniqueId(), elite ? 15 : 5);
            if (mob && !boss) {
                plugin.quests().progress(k.getUniqueId(), "KILL_MOBS", "", 1);
                if (elite) plugin.quests().progress(k.getUniqueId(), "KILL_ELITES", "", 1);
            }
        }
        if (mob && !boss && (s.modifier() == Modifier.EXPLOSIVE_MOBS || traits(e.getEntity()).contains("explosive"))) {
            var loc = e.getEntity().getLocation();
            loc.getWorld().createExplosion(loc, 2.5f, false, false);
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        var s = plugin.dungeons().active();
        if (s == null || s.modifier() != Modifier.NO_REGEN || !s.players().contains(p.getUniqueId())) return;
        var r = e.getRegainReason();
        if (r == EntityRegainHealthEvent.RegainReason.SATIATED || r == EntityRegainHealthEvent.RegainReason.REGEN) {
            e.setCancelled(true);
        }
    }

    // Chet trong Dungeon: giu do, tru 1 mang. Het mang: con dong doi song => GUC cho cuu, khong thi bi loai.
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        var s = plugin.dungeons().active();
        if (s == null || !s.players().contains(p.getUniqueId())) return;

        if (plugin.getConfig().getBoolean("dungeon.keep-inventory", true)) {
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.getDrops().clear();
            e.setDroppedExp(0);
        }
        UUID id = p.getUniqueId();
        s.addDeath(id);
        int left = s.loseLife(id);
        var m = plugin.messages();
        if (left == 0) {
            boolean canRevive = plugin.getConfig().getBoolean("revive.enabled", true)
                    && plugin.revive().hasAliveTeammate(s, id);
            if (canRevive) {
                plugin.revive().markDowned(s, p);
                p.sendMessage(m.get("downed"));
            } else {
                s.eliminated().add(id);
                p.sendMessage(m.get("eliminated"));
            }
        } else if (left > 0) {
            p.sendMessage(m.get("lives-left", "lives", left));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        var s = plugin.dungeons().active();
        Player p = e.getPlayer();
        if (s == null || !s.players().contains(p.getUniqueId())) return;

        if (s.downed().contains(p.getUniqueId())) {
            Location spot = plugin.revive().spot(p.getUniqueId());
            if (spot != null) e.setRespawnLocation(spot);
            plugin.revive().scheduleDownState(p);
            return;
        }
        if (s.eliminated().contains(p.getUniqueId())) {
            s.players().remove(p.getUniqueId());
            var lobby = plugin.dungeons().lobby();
            if (lobby != null) e.setRespawnLocation(lobby);
            return;
        }
        var spawn = s.def().spawn();
        if (spawn != null) e.setRespawnLocation(spawn);
        // vai giay bat tu sau khi hoi sinh
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4));
        }, 2L);
    }
}
