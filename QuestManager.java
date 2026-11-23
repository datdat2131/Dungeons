package com.astrax.dungeon.quest;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.database.Database;
import com.astrax.dungeon.reward.LootTable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nhiem vu ngay/tuan (chon ngau nhien nhung giong nhau cho moi nguoi trong cung ngay/tuan) va chuoi ngay tham gia.
 * Tien do nam trong bo nho, ghi DB bat dong bo moi 30 giay va khi thoat game.
 * Loai nhiem vu: KILL_MOBS KILL_ELITES BOSS_DAMAGE CLEAR_DUNGEON CLEAR_TIER(param) WAVES JOIN_DUNGEON REVIVE NO_DEATH_CLEAR.
 */
public final class QuestManager {
    public record Quest(String id, String name, String scope, String type, String param, int amount,
                        double money, List<LootTable.Drop> items) {}

    private record Dirty(String key, int progress, int claimed) {}

    private static final class Streak {
        String lastDay = "";
        int streak;
        int best;
    }

    private final AstraXDungeon plugin;
    private final List<Quest> daily = new ArrayList<>();
    private final List<Quest> weekly = new ArrayList<>();
    private int dailyCount = 3;
    private int weeklyCount = 3;
    private final Map<String, List<Quest>> activeCache = new HashMap<>();
    private final Map<UUID, Map<String, int[]>> data = new ConcurrentHashMap<>();  // {tien do, da nhan, dirty}
    private final Map<UUID, Streak> streaks = new ConcurrentHashMap<>();
    private BukkitTask flushTask;

    public QuestManager(AstraXDungeon plugin) { this.plugin = plugin; }

    // ---------- cau hinh ----------
    public void load() {
        daily.clear();
        weekly.clear();
        activeCache.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "quests.yml"));
        dailyCount = y.getInt("daily-count", 3);
        weeklyCount = y.getInt("weekly-count", 3);
        parse(y.getMapList("daily"), "DAILY", daily);
        parse(y.getMapList("weekly"), "WEEKLY", weekly);
    }

    private void parse(List<Map<?, ?>> list, String scope, List<Quest> out) {
        for (Map<?, ?> m : list) {
            if (m.get("id") == null) continue;
            String id = String.valueOf(m.get("id"));
            double money = 0;
            List<LootTable.Drop> items = new ArrayList<>();
            if (m.get("reward") instanceof Map<?, ?> rw) {
                if (rw.get("money") instanceof Number n) money = n.doubleValue();
                if (rw.get("items") instanceof List<?> l) {
                    for (Object o : l) if (o instanceof Map<?, ?> im) items.add(LootTable.parseDrop(im));
                }
            }
            out.add(new Quest(id, m.get("name") == null ? id : String.valueOf(m.get("name")), scope,
                    m.get("type") == null ? "KILL_MOBS" : String.valueOf(m.get("type")).toUpperCase(),
                    m.get("param") == null ? "" : String.valueOf(m.get("param")),
                    m.get("amount") instanceof Number a ? a.intValue() : 1, money, items));
        }
    }

    public void start() {
        load();
        flushTask = Bukkit.getScheduler().runTaskTimer(plugin, this::flushAll, 20L * 30, 20L * 30);
    }

    public void stop() {
        if (flushTask != null) { flushTask.cancel(); flushTask = null; }
        flushAll();
    }

    // ---------- nhiem vu dang hoat dong ----------
    public String periodKey(String scope) {
        LocalDate d = LocalDate.now();
        if (scope.equals("DAILY")) return d.toString();
        return d.get(IsoFields.WEEK_BASED_YEAR) + "-W" + String.format("%02d", d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    public List<Quest> active(String scope) {
        String key = scope + ":" + periodKey(scope);
        List<Quest> cached = activeCache.get(key);
        if (cached == null) {
            List<Quest> pool = new ArrayList<>(scope.equals("DAILY") ? daily : weekly);
            Collections.shuffle(pool, new Random(key.hashCode()));
            int n = Math.min(scope.equals("DAILY") ? dailyCount : weeklyCount, pool.size());
            cached = new ArrayList<>(pool.subList(0, n));
            activeCache.put(key, cached);
        }
        return cached;
    }

    private String key(Quest q) { return q.id() + "|" + periodKey(q.scope()); }

    /** {tien do, da nhan} - ban sao de hien thi. */
    public int[] state(UUID id, Quest q) {
        Map<String, int[]> map = data.get(id);
        int[] e = map == null ? null : map.get(key(q));
        return e == null ? new int[]{0, 0} : new int[]{e[0], e[1]};
    }

    // ---------- tien do ----------
    public void progress(UUID id, String type, String param, int amount) {
        if (amount <= 0) return;
        for (String scope : new String[]{"DAILY", "WEEKLY"}) {
            for (Quest q : active(scope)) {
                if (!q.type().equals(type)) continue;
                if (!q.param().isEmpty() && !q.param().equalsIgnoreCase(param)) continue;
                int[] e = data.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).computeIfAbsent(key(q), k -> new int[3]);
                if (e[1] == 1 || e[0] >= q.amount()) continue;
                e[0] = Math.min(q.amount(), e[0] + amount);
                e[2] = 1;
                if (e[0] >= q.amount()) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) p.sendMessage(plugin.messages().get("quest-complete", "name", q.name()));
                }
            }
        }
    }

    public void claim(Player p, Quest q) {
        var m = plugin.messages();
        Map<String, int[]> map = data.computeIfAbsent(p.getUniqueId(), k -> new ConcurrentHashMap<>());
        int[] e = map.computeIfAbsent(key(q), k -> new int[3]);
        if (e[1] == 1) { p.sendMessage(m.get("quest-already")); return; }
        if (e[0] < q.amount()) { p.sendMessage(m.get("quest-incomplete")); return; }
        e[1] = 1;
        e[2] = 1;
        if (q.money() > 0) {
            plugin.distributor().giveMoney(p, q.money());
            p.sendMessage(m.plain("reward-money", "money", (long) q.money()));
        }
        for (LootTable.Drop d : q.items()) {
            plugin.distributor().give(p, d, d.amount());
            p.sendMessage(m.plain("reward-item", "item", d.rarity().color() + d.name(), "amount", d.amount()));
        }
        p.sendMessage(m.get("quest-claimed", "name", q.name()));
        flush(p.getUniqueId());
    }

    // ---------- nap / luu ----------
    public void loadPlayer(UUID id) {
        Database db = plugin.database();
        List<String> periods = List.of(periodKey("DAILY"), periodKey("WEEKLY"));
        db.async(() -> db.loadQuests(id.toString(), periods)).thenAccept(rows -> {
            Map<String, int[]> map = data.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
            for (Database.QuestRow r : rows) {
                String k = r.questId() + "|" + r.periodKey();
                int[] cur = map.get(k);
                if (cur == null) map.put(k, new int[]{r.progress(), r.claimed(), 0});
                else { cur[0] = Math.max(cur[0], r.progress()); cur[1] = Math.max(cur[1], r.claimed()); }
            }
        }).exceptionally(e -> null);
        db.async(() -> db.loadStreak(id.toString())).thenAccept(row -> {
            if (row == null) return;
            Streak st = streaks.computeIfAbsent(id, k -> new Streak());
            st.lastDay = row.lastDay() == null ? "" : row.lastDay();
            st.streak = row.streak();
            st.best = row.best();
        }).exceptionally(e -> null);
    }

    public void unload(UUID id) {
        flush(id);
        data.remove(id);
        streaks.remove(id);
    }

    public void flushAll() {
        for (UUID id : new ArrayList<>(data.keySet())) flush(id);
    }

    private void flush(UUID id) {
        Map<String, int[]> map = data.get(id);
        if (map == null) return;
        List<Dirty> dirty = new ArrayList<>();
        for (Map.Entry<String, int[]> en : map.entrySet()) {
            int[] e = en.getValue();
            if (e[2] == 1) {
                e[2] = 0;
                dirty.add(new Dirty(en.getKey(), e[0], e[1]));
            }
        }
        if (dirty.isEmpty()) return;
        Database db = plugin.database();
        db.async(() -> {
            for (Dirty d : dirty) {
                String[] kp = d.key().split("\\|", 2);
                if (kp.length == 2) db.saveQuest(id.toString(), kp[0], kp[1], d.progress(), d.claimed());
            }
            return null;
        }).exceptionally(e -> {
            plugin.getLogger().warning("Khong luu duoc tien do nhiem vu: " + e.getMessage());
            return null;
        });
    }

    // ---------- chuoi ngay tham gia (streak) ----------
    /** Goi moi khi nguoi choi vao Dungeon: cap nhat nhiem vu JOIN_DUNGEON va streak. */
    public void onDungeonJoin(Player p) {
        progress(p.getUniqueId(), "JOIN_DUNGEON", "", 1);
        touchStreak(p);
    }

    private void touchStreak(Player p) {
        UUID id = p.getUniqueId();
        LocalDate today = LocalDate.now();
        String t = today.toString();
        Streak st = streaks.computeIfAbsent(id, k -> new Streak());
        if (t.equals(st.lastDay)) return;
        st.streak = today.minusDays(1).toString().equals(st.lastDay) ? st.streak + 1 : 1;
        st.best = Math.max(st.best, st.streak);
        st.lastDay = t;
        p.sendMessage(plugin.messages().get("streak-up", "days", st.streak, "bonus", (int) Math.round(streakBonus(id) * 100)));

        final String last = st.lastDay;
        final int s = st.streak;
        final int b = st.best;
        Database db = plugin.database();
        db.async(() -> { db.saveStreak(id.toString(), last, s, b); return null; }).exceptionally(e -> null);
    }

    public int effectiveStreak(UUID id) {
        Streak st = streaks.get(id);
        if (st == null) return 0;
        LocalDate today = LocalDate.now();
        if (today.toString().equals(st.lastDay) || today.minusDays(1).toString().equals(st.lastDay)) return st.streak;
        return 0;
    }

    public int bestStreak(UUID id) {
        Streak st = streaks.get(id);
        return st == null ? 0 : st.best;
    }

    /** Bonus ty le roi theo chuoi ngay: +2%/ngay, toi da +20% (chinh trong config quests.*). */
    public double streakBonus(UUID id) {
        int s = effectiveStreak(id);
        if (s <= 1) return 0;
        double per = plugin.getConfig().getDouble("quests.streak-bonus-per-day", 0.02);
        double max = plugin.getConfig().getDouble("quests.streak-max-bonus", 0.20);
        return Math.min(max, per * (s - 1));
    }
}
