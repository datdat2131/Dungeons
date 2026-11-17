package com.astrax.dungeon.schedule;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonDefinition;
import com.astrax.dungeon.dungeon.DungeonManager;
import com.astrax.dungeon.dungeon.Modifier;
import com.astrax.dungeon.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Moi gio mo 1 Dungeon: dem nguoc 10/5/1 phut, BossBar, gio vang, chon co trong so va khong lap. */
public final class DungeonScheduler {
    private final AstraXDungeon plugin;
    private final DungeonManager dm;
    private final File dataFile;
    private final Set<Integer> announced = new HashSet<>();

    private boolean enabled;
    private ZoneId zone = ZoneId.systemDefault();
    private int everyHours = 1;
    private int atMinute = 0;
    private boolean noRepeat = true;
    private List<Integer> announceAt = List.of(10, 5, 1);
    private GoldenHour golden = new GoldenHour(null);

    private ZonedDateTime nextOpen;
    private DungeonDefinition nextDungeon;
    private String lastId;
    private BossBar bar;
    private BukkitTask task;

    public DungeonScheduler(AstraXDungeon plugin, DungeonManager dm) {
        this.plugin = plugin;
        this.dm = dm;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void start() {
        reload();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        hideBar();
    }

    public void reload() {
        YamlConfiguration c = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "schedule.yml"));
        enabled = c.getBoolean("enabled", true);
        try { zone = ZoneId.of(c.getString("timezone", "Asia/Ho_Chi_Minh")); } catch (Exception e) { zone = ZoneId.systemDefault(); }
        everyHours = Math.max(1, c.getInt("open-every-hours", 1));
        atMinute = Math.min(59, Math.max(0, c.getInt("open-at-minute", 0)));
        noRepeat = c.getBoolean("no-repeat", true);
        List<Integer> a = c.getIntegerList("announce-at-minutes");
        announceAt = a.isEmpty() ? List.of(10, 5, 1) : a;
        golden = new GoldenHour(c.getConfigurationSection("golden-hour"));
        lastId = YamlConfiguration.loadConfiguration(dataFile).getString("last-dungeon-id");
        announced.clear();
        hideBar();
        computeNext();
    }

    // ---------- tinh gio mo ke tiep ----------
    private void computeNext() {
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime t = now.truncatedTo(ChronoUnit.HOURS).withMinute(atMinute);
        while (!t.isAfter(now) || t.getHour() % everyHours != 0) t = t.plusHours(1);
        nextOpen = t;
        nextDungeon = pick();
        announced.clear();
    }

    private DungeonDefinition pick() {
        List<DungeonDefinition> pool = new ArrayList<>(dm.enabled());
        if (noRepeat && pool.size() > 1) pool.removeIf(d -> d.id().equals(lastId));
        if (pool.isEmpty()) return null;
        int total = pool.stream().mapToInt(DungeonDefinition::weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (DungeonDefinition d : pool) {
            roll -= d.weight();
            if (roll < 0) return d;
        }
        return pool.get(0);
    }

    // ---------- vong lap moi giay ----------
    private void tick() {
        dm.tick();
        if (!enabled || nextOpen == null) return;

        long left = Duration.between(ZonedDateTime.now(zone), nextOpen).getSeconds();
        if (left <= 0) { openNow(); return; }
        if (nextDungeon == null) return;

        int maxMin = Collections.max(announceAt);
        if (left <= maxMin * 60L) showBar(left, maxMin * 60L); else hideBar();

        for (int m : announceAt) {
            // chi bao khi dung moc (tranh bao muon sau khi server vua khoi dong)
            if (left <= m * 60L && left > m * 60L - 10 && announced.add(m)) announceCountdown(m);
        }
    }

    private void openNow() {
        hideBar();
        if (nextDungeon != null) {
            ZonedDateTime now = ZonedDateTime.now(zone);
            boolean gold = golden.isActive(now);
            lastId = nextDungeon.id();
            saveLast();
            dm.open(nextDungeon, Modifier.random(), gold ? golden.contribution() : 1.0, gold ? golden.drop() : 1.0);
        }
        computeNext();
    }

    // ---------- hien thi ----------
    private void announceCountdown(int minutes) {
        var m = plugin.messages();
        String name = nextDungeon.displayName();
        Bukkit.broadcast(m.get("countdown", "dungeon", name, "minutes", minutes));
        if (golden.isActive(nextOpen)) Bukkit.broadcast(m.get("golden-soon"));
        Title title = Title.title(Text.c(name), m.plain("countdown", "dungeon", name, "minutes", minutes));
        for (Player p : Bukkit.getOnlinePlayers()) p.showTitle(title);
    }

    private void showBar(long left, long total) {
        if (bar == null) bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        bar.name(plugin.messages().plain("bossbar", "dungeon", nextDungeon.displayName(), "time", Text.duration(left)));
        bar.progress(Math.max(0f, Math.min(1f, (float) left / total)));
        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(bar);
    }

    private void hideBar() {
        if (bar == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
    }

    private void saveLast() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(dataFile);
        y.set("last-dungeon-id", lastId);
        y.set("last-open-epoch", System.currentTimeMillis() / 1000);
        try { y.save(dataFile); } catch (IOException e) { plugin.getLogger().warning("Khong luu duoc data.yml"); }
    }

    // ---------- API cho lenh / placeholder ----------
    public DungeonDefinition nextDungeon() { return nextDungeon; }
    public long secondsUntilNext() {
        return nextOpen == null ? -1 : Math.max(0, Duration.between(ZonedDateTime.now(zone), nextOpen).getSeconds());
    }
    public boolean nextIsGolden() { return nextOpen != null && golden.isActive(nextOpen); }
}
