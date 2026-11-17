package com.astrax.dungeon.mob;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonManager;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.dungeon.DungeonState;
import com.astrax.dungeon.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chay Dungeon theo TUNG DOT wave:
 * NGHI (dem nguoc) -> DOT ACTIVE (spawn het quai cua dot) -> clear het moi sang dot sau -> het dot cuoi thi goi Boss.
 */
public final class MobManager {
    private enum Stage { INTERMISSION, ACTIVE }

    private final AstraXDungeon plugin;
    private final DungeonManager dm;
    private final Set<UUID> alive = new HashSet<>();
    private final Deque<String> queue = new ArrayDeque<>();
    private List<List<String>> waves = List.of();
    private DungeonSession session;
    private BukkitTask task;
    private BossBar bar;

    private Stage stage = Stage.INTERMISSION;
    private int waveIdx;      // so dot DA bat dau (dot hien tai = waveIdx)
    private int waveTotal;    // so quai cua dot hien tai
    private int countdown;    // giay nghi con lai
    private int delayTotal;   // tong giay nghi (de tinh thanh tien trinh)

    public MobManager(AstraXDungeon plugin, DungeonManager dm) {
        this.plugin = plugin;
        this.dm = dm;
    }

    public void start(DungeonSession s) {
        stopTask();
        session = s;
        waves = s.def().waves();
        waveIdx = 0;
        waveTotal = 0;
        queue.clear();
        alive.clear();
        stage = Stage.INTERMISSION;
        delayTotal = countdown = plugin.getConfig().getInt("mobs.first-wave-delay-seconds", 5);
        dm.broadcastSession(plugin.messages().get("wave-wait", "n", 1, "total", waves.size(), "sec", countdown));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        if (session == null || dm.active() != session || session.state() != DungeonState.RUNNING) return;

        alive.removeIf(id -> {
            Entity e = Bukkit.getEntity(id);
            return e == null || e.isDead() || !e.isValid();
        });

        List<Player> ps = online();
        if (ps.isEmpty()) return; // khong co nguoi => tam dung, khong ton tai nguyen

        if (stage == Stage.INTERMISSION) intermission(ps); else activeWave(ps);
    }

    // ---------- nghi giua cac dot ----------
    private void intermission(List<Player> ps) {
        boolean bossNext = waveIdx >= waves.size();
        if (countdown <= 0) {
            hideBar();
            if (bossNext) dm.startBoss(); else startWave(ps);
            return;
        }
        Component name = plugin.messages().plain(bossNext ? "boss-wait" : "wave-wait",
                "n", waveIdx + 1, "total", waves.size(), "sec", countdown);
        setBar(ps, name, (float) countdown / Math.max(1, delayTotal), BossBar.Color.YELLOW);
        countdown--;
    }

    private void startWave(List<Player> ps) {
        loadWave(waveIdx);
        waveIdx++;
        waveTotal = queue.size();
        stage = Stage.ACTIVE;

        var m = plugin.messages();
        Title title = Title.title(m.plain("wave-title", "n", waveIdx, "total", waves.size()),
                m.plain("wave-subtitle", "count", waveTotal));
        for (Player p : ps) {
            p.showTitle(title);
            p.playSound(p.getLocation(), org.bukkit.Sound.EVENT_RAID_HORN, 1f, 1f);
        }
        dm.broadcastSession(m.get("wave", "n", waveIdx, "total", waves.size()));
    }

    // ---------- dot dang chay ----------
    private void activeWave(List<Player> ps) {
        int cap = plugin.getConfig().getInt("mobs.cap", 40);
        int radius = plugin.getConfig().getInt("mobs.spawn-radius", 14);
        double eliteChance = plugin.getConfig().getDouble("mobs.elite-chance", 0.05);

        int n = Math.min(Math.min(queue.size(), cap - alive.size()), 8);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            String spec = queue.poll();
            if (spec == null) break;
            Location center = ps.get(r.nextInt(ps.size())).getLocation();
            Location at = MobSpawner.safeNear(center, radius);
            boolean elite = r.nextDouble() < eliteChance;
            plugin.spawner().spawn(spec, at, elite, ps.size(), e -> alive.add(e.getUniqueId()));
        }

        updateWaveBar(ps);
        // vua spawn xong thi chua kiem tra clear (MythicMobs them vao 'alive' tre 2 tick)
        if (n > 0) return;
        if (queue.isEmpty() && alive.isEmpty()) waveCleared(ps);
    }

    private void updateWaveBar(List<Player> ps) {
        int left = queue.size() + alive.size();
        Component name = plugin.messages().plain("wave-bar", "n", waveIdx, "total", waves.size(), "left", left);
        setBar(ps, name, (float) left / Math.max(1, waveTotal), BossBar.Color.RED);
    }

    private void waveCleared(List<Player> ps) {
        var cfg = plugin.getConfig();
        var m = plugin.messages();
        double pts = cfg.getDouble("mobs.wave-clear-points", 20);
        for (UUID id : session.players()) session.addPoints(id, pts);

        if (cfg.getBoolean("mobs.heal-between-waves", true)) {
            for (Player p : ps) {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) p.setHealth(max.getValue());
                p.setFoodLevel(20);
            }
        }
        dm.broadcastSession(m.get("wave-clear", "n", waveIdx, "points", (int) pts));

        stage = Stage.INTERMISSION;
        delayTotal = countdown = cfg.getInt("mobs.wave-delay-seconds", 10);
    }

    private void loadWave(int idx) {
        for (String entry : waves.get(idx)) {
            String spec = entry;
            int amount = 1;
            int i = entry.lastIndexOf(':');
            if (i > 0) {
                try {
                    amount = Integer.parseInt(entry.substring(i + 1).trim());
                    spec = entry.substring(0, i).trim();
                } catch (NumberFormatException ignored) { /* ten mythic co dau ':' */ }
            }
            for (int k = 0; k < amount; k++) queue.add(spec);
        }
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

    private void setBar(List<Player> ps, Component name, float progress, BossBar.Color color) {
        if (bar == null) bar = BossBar.bossBar(name, 1f, color, BossBar.Overlay.NOTCHED_10);
        bar.name(name);
        bar.color(color);
        bar.progress(Math.max(0f, Math.min(1f, progress)));
        for (Player p : ps) p.showBossBar(bar);
    }

    private void hideBar() {
        if (bar == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
    }

    private void stopTask() {
        if (task != null) { task.cancel(); task = null; }
    }

    /** Dung vong lap va don sach moi thuc the do plugin tao (theo PDC). */
    public void stop() {
        stopTask();
        hideBar();
        queue.clear();
        Set<World> worlds = new HashSet<>();
        if (session != null) {
            Location a = session.def().spawn();
            Location b = session.def().bossSpawn();
            if (a != null) worlds.add(a.getWorld());
            if (b != null) worlds.add(b.getWorld());
        }
        for (UUID id : alive) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) { worlds.add(e.getWorld()); e.remove(); }
        }
        alive.clear();
        for (World w : worlds) {
            for (Entity e : w.getEntities()) {
                if (plugin.spawner().isTagged(e)) e.remove();
            }
        }
    }
}
