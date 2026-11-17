package com.astrax.dungeon.listener;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.boss.BossController;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.dungeon.Modifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;

public final class CombatListener implements Listener {
    private final AstraXDungeon plugin;

    public CombatListener(AstraXDungeon plugin) { this.plugin = plugin; }

    private Player attacker(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent be)) return null;
        Entity d = be.getDamager();
        if (d instanceof Projectile pr && pr.getShooter() instanceof Player ps) return ps;
        return d instanceof Player p ? p : null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        var dm = plugin.dungeons();

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

        // Danh quai Dungeon => ghi nhan hoat dong that
        if (plugin.spawner().isTagged(e.getEntity())) {
            Player a = attacker(e);
            if (a != null && dm.active() != null && dm.isInDungeon(a.getUniqueId())) dm.active().touch(a.getUniqueId());
        }

        // Chan PvP giua nguoi choi trong Dungeon
        if (e.getEntity() instanceof Player victim && dm.isInDungeon(victim.getUniqueId())) {
            Player src = attacker(e);
            if (src != null && dm.isInDungeon(src.getUniqueId())) e.setCancelled(true);
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
        }
        if (mob && !boss && s.modifier() == Modifier.EXPLOSIVE_MOBS) {
            var loc = e.getEntity().getLocation();
            loc.getWorld().createExplosion(loc, 2f, false, false);
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

    // Chet trong Dungeon: giu do, hoi sinh tai spawn (tranh mat do khi choi PvE)
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!plugin.dungeons().isInDungeon(e.getPlayer().getUniqueId())) return;
        if (!plugin.getConfig().getBoolean("dungeon.keep-inventory", true)) return;
        e.setKeepInventory(true);
        e.setKeepLevel(true);
        e.getDrops().clear();
        e.setDroppedExp(0);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        var s = plugin.dungeons().active();
        if (s == null || !s.players().contains(e.getPlayer().getUniqueId())) return;
        var spawn = s.def().spawn();
        if (spawn != null) e.setRespawnLocation(spawn);
    }
}
