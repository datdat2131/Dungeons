package com.astrax.dungeon.mob;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonManager;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.dungeon.DungeonState;
import com.astrax.dungeon.tier.Tier;
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
 * Chay Dungeon theo TUNG DOT: NGHI (dem nguoc) -> DOT ACTIVE (spawn het quai cua dot) -> clear het moi sang dot sau
 * -> het dot cuoi thi goi Boss. Dot dac biet: ELITE, SWARM, MINIBOSS.
 */
public final class MobManager {
    private enum Stage { INTERMISSION, ACTIVE }

    private final AstraXDungeon plugin;
    private final DungeonManager dm;
    private final Set<UUID> alive = new HashSet<>();
    private final Deque<MobSpec> queue = new ArrayDeque<>();
    private List<Wave> waves = List.of();
    private DungeonSession session;
    private Tier tier;
    private BukkitTask task;
    private BossBar bar;

    private Stage stage = Stage.INTERMISSION;
    private int waveIdx;      // so dot DA bat dau (dot hien tai = waveIdx)
    private int waveTotal;
    private int countdown;
    private int delayTotal;
    private String kind = "NORMAL";

    public MobManager(AstraXDungeon plugin, DungeonManager dm) {
        this.plugin = plugin;
        this.dm = dm;
    }

    public int totalWaves() { return waves.size(); }

    /** Quai do ky nang/trieu hoi sinh ra: wave chi clear khi chung cung chet. */
    public void track(UUID id) { alive.add(id); }
    public int aliveCount() { return alive.size(); }

    public void start(DungeonSession s) {
        stopTask();
        session = s;
        tier = s.tier();
        waves = WaveGenerator.generate(s.def(), tier);
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
        if (ps.isEmpty()) return; // khong co nguoi => tam dung

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
        Wave w = waves.get(waveIdx);
        waveIdx++;
        queue.addAll(w.mobs());
        waveTotal = queue.size();
        kind = w.kind();
        stage = Stage.ACTIVE;

        var m = plugin.messages();
        Title title = Title.title(m.plain("wave-title", "n", waveIdx, "total", waves.size()),
                m.plain("wave-kind-" + kind, "count", waveTotal));
        for (Player p : ps) {
            p.showTitle(title);
            p.playSound(p.getLocation(), org.bukkit.Sound.EVENT_RAID_HORN, 1f, kind.equals("MINIBOSS") ? 0.6f : 1f);
        }
        dm.broadcastSession(m.get("wave", "n", waveIdx, "total", waves.size()));
        if (kind.equals("MINIBOSS")) dm.broadcastSession(m.get("wave-miniboss"));
    }

    // ---------- dot dang chay ----------
    private void activeWave(List<Player> ps) {
        int cap = tier.mobCap();
        int radius = plugin.getConfig().getInt("mobs.spawn-radius", 14);
        double eliteChance = tier.eliteChance();

        int n = Math.min(Math.min(queue.size(), cap - alive.size()), 8);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            MobSpec ms = queue.poll();
            if (ms == null) break;
            if (!ms.elite() && r.nextDouble() < eliteChance) {
                ms = new MobSpec(ms.type(), ms.traits(), true, ms.hpMult(), ms.name(), ms.skills());
            }
            Location center = ps.get(r.nextInt(ps.size())).getLocation();
            Location at = MobSpawner.safeNear(center, radius);
            plugin.spawner().spawn(ms, at, ps.size(), tier, waveIdx, e -> alive.add(e.getUniqueId()));
        }

        updateWaveBar(ps);
        // vua spawn xong thi chua kiem tra clear (MythicMobs them vao 'alive' tre 2 tick)
        if (n > 0) return;
        if (queue.isEmpty() && alive.isEmpty()) waveCleared(ps);
    }

    private void updateWaveBar(List<Player> ps) {
        int left = queue.size() + alive.size();
        Component name = plugin.messages().plain("wave-bar", "n", waveIdx, "total", waves.size(), "left", left);
        BossBar.Color color = switch (kind) {
            case "ELITE" -> BossBar.Color.YELLOW;
            case "SWARM" -> BossBar.Color.PINK;
            case "MINIBOSS" -> BossBar.Color.PURPLE;
            default -> BossBar.Color.RED;
        };
        setBar(ps, name, (float) left / Math.max(1, waveTotal), color);
    }

    private void waveCleared(List<Player> ps) {
        var cfg = plugin.getConfig();
        var m = plugin.messages();
        double pts = tier.waveClearPoints();
        for (UUID id : session.players()) {
            session.addPoints(id, pts);
            plugin.quests().progress(id, "WAVES", "", 1);
        }

        if (cfg.getBoolean("mobs.heal-between-waves", true)) {
            for (Player p : ps) {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) p.setHealth(max.getValue());
                p.setFoodLevel(20);
            }
        }
        dm.broadcastSession(m.get("wave-clear", "n", waveIdx, "points", (int) pts));

        stage = Stage.INTERMISSION;
        delayTotal = countdown = tier.waveDelaySeconds();
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
        plugin.mobSkills().clear();
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
