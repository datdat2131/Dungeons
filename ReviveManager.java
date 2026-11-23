package com.astrax.dungeon.revive;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Het mang nhung con dong doi song => nguoi choi GUC (spectator tai cho chet). Dong doi dung gan va GIU SHIFT
 * de cuu. Het thoi gian cho hoac khong con ai song => bi loai. Vong lap chi lam viec khi co nguoi dang guc.
 */
public final class ReviveManager {
    private final AstraXDungeon plugin;
    private final Map<UUID, Location> spots = new HashMap<>();
    private final Map<UUID, Double> progress = new HashMap<>();
    private final Map<UUID, Long> downedAt = new HashMap<>();
    private final Map<UUID, GameMode> prevMode = new HashMap<>();
    private BukkitTask task;

    public ReviveManager(AstraXDungeon plugin) { this.plugin = plugin; }

    public void start() { task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 5L); }
    public void stop() { if (task != null) { task.cancel(); task = null; } }

    public Location spot(UUID id) { return spots.get(id); }

    public boolean hasAliveTeammate(DungeonSession s, UUID except) {
        for (UUID u : s.players()) {
            if (u.equals(except) || s.downed().contains(u)) continue;
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline() && !p.isDead() && p.getGameMode() != GameMode.SPECTATOR) return true;
        }
        return false;
    }

    /** Goi luc nguoi choi chet va het mang (con dong doi song). */
    public void markDowned(DungeonSession s, Player p) {
        UUID id = p.getUniqueId();
        s.downed().add(id);
        spots.put(id, p.getLocation().clone());
        downedAt.put(id, System.currentTimeMillis());
        progress.put(id, 0.0);
        for (UUID u : s.players()) {
            Player t = Bukkit.getPlayer(u);
            if (t != null && !u.equals(id)) t.sendMessage(plugin.messages().get("downed-team", "name", p.getName()));
        }
    }

    public void scheduleDownState(Player p) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyDownState(p), 2L);
    }

    private void applyDownState(Player p) {
        UUID id = p.getUniqueId();
        var s = plugin.dungeons().active();
        if (!p.isOnline() || s == null || !s.downed().contains(id)) return;
        prevMode.putIfAbsent(id, p.getGameMode() == GameMode.SPECTATOR ? GameMode.SURVIVAL : p.getGameMode());
        p.setGameMode(GameMode.SPECTATOR);
        p.getPersistentDataContainer().set(plugin.downKey(), PersistentDataType.BYTE, (byte) 1);
        Location spot = spots.get(id);
        if (spot != null) p.teleport(spot);
        var m = plugin.messages();
        p.showTitle(Title.title(m.plain("downed-title"), m.plain("downed-sub")));
    }

    private void tick() {
        DungeonSession s = plugin.dungeons().active();
        if (s == null || s.downed().isEmpty()) return;

        var cfg = plugin.getConfig();
        var m = plugin.messages();
        double reviveSec = cfg.getDouble("revive.seconds", 5);
        long timeout = cfg.getLong("revive.timeout-seconds", 60) * 1000L;
        long now = System.currentTimeMillis();

        List<Player> alive = new ArrayList<>();
        for (UUID u : s.players()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline() && !p.isDead() && !s.downed().contains(u)
                    && p.getGameMode() != GameMode.SPECTATOR) alive.add(p);
        }
        if (alive.isEmpty()) { // ca doi guc het => loai het nguoi dang guc
            for (UUID id : new ArrayList<>(s.downed())) eliminate(s, id);
            return;
        }

        for (UUID id : new ArrayList<>(s.downed())) {
            Location spot = spots.get(id);
            long since = downedAt.getOrDefault(id, now);
            if (spot == null || now - since > timeout) { eliminate(s, id); continue; }

            spot.getWorld().spawnParticle(Particle.HEART, spot.clone().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0);
            spot.getWorld().spawnParticle(Particle.END_ROD, spot.clone().add(0, 0.5, 0), 4, 0.3, 0.5, 0.3, 0.01);

            Player reviver = null;
            for (Player a : alive) {
                if (a.getWorld().equals(spot.getWorld()) && a.isSneaking() && a.getLocation().distanceSquared(spot) <= 9) {
                    reviver = a;
                    break;
                }
            }
            double pr = progress.getOrDefault(id, 0.0);
            String name = Bukkit.getOfflinePlayer(id).getName();
            if (reviver != null) {
                pr += 0.25;
                reviver.sendActionBar(m.plain("reviving", "name", name == null ? "?" : name,
                        "pct", (int) Math.min(100, pr / reviveSec * 100)));
            } else {
                pr = Math.max(0, pr - 0.125);
            }
            progress.put(id, pr);

            if (pr >= reviveSec) {
                revive(s, id, reviver);
            } else {
                Player d = Bukkit.getPlayer(id);
                if (d != null) d.sendActionBar(m.plain("downed-wait", "sec", Math.max(0, (timeout - (now - since)) / 1000)));
            }
        }
    }

    private void revive(DungeonSession s, UUID id, Player reviver) {
        var cfg = plugin.getConfig();
        var m = plugin.messages();
        s.downed().remove(id);
        s.setLives(id, Math.max(1, cfg.getInt("revive.lives-on-revive", 1)));
        Location spot = spots.remove(id);
        progress.remove(id);
        downedAt.remove(id);

        Player d = Bukkit.getPlayer(id);
        if (d != null) {
            restoreMode(d);
            if (spot != null) d.teleport(spot);
            AttributeInstance max = d.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) d.setHealth(max.getValue());
            d.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4));
            d.sendMessage(m.get("revived"));
        }
        if (reviver != null) {
            s.addPoints(reviver.getUniqueId(), 25);
            plugin.quests().progress(reviver.getUniqueId(), "REVIVE", "", 1);
            String dn = d != null ? d.getName() : "?";
            plugin.dungeons().broadcastSession(m.get("revive-done", "reviver", reviver.getName(), "name", dn));
        }
    }

    private void eliminate(DungeonSession s, UUID id) {
        s.downed().remove(id);
        s.eliminated().add(id);
        s.players().remove(id);
        spots.remove(id);
        progress.remove(id);
        downedAt.remove(id);
        Player d = Bukkit.getPlayer(id);
        if (d != null) {
            restoreMode(d);
            Location lobby = plugin.dungeons().lobby();
            if (lobby != null) d.teleport(lobby);
            d.sendMessage(plugin.messages().get("eliminated"));
        }
    }

    /** Goi khi nguoi choi thoat game / roi Dungeon luc dang guc. */
    public void release(UUID id) {
        var s = plugin.dungeons().active();
        boolean wasDowned = s != null && s.downed().remove(id);
        if (wasDowned) s.eliminated().add(id);
        spots.remove(id);
        progress.remove(id);
        downedAt.remove(id);
        Player d = Bukkit.getPlayer(id);
        if (d != null && (wasDowned || prevMode.containsKey(id))) restoreMode(d);
    }

    /** Khi Dungeon ket thuc: tra che do choi cho moi nguoi dang guc. */
    public void clearAll() {
        for (UUID id : new ArrayList<>(prevMode.keySet())) {
            Player d = Bukkit.getPlayer(id);
            if (d != null) restoreMode(d);
        }
        prevMode.clear();
        spots.clear();
        progress.clear();
        downedAt.clear();
        var s = plugin.dungeons().active();
        if (s != null) s.downed().clear();
    }

    private void restoreMode(Player d) {
        GameMode gm = prevMode.remove(d.getUniqueId());
        d.setGameMode(gm != null ? gm : GameMode.SURVIVAL);
        d.getPersistentDataContainer().remove(plugin.downKey());
    }
}
