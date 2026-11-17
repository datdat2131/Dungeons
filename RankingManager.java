package com.astrax.dungeon.ranking;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.database.Database;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Cache BXH (top 50) va thong ke nguoi choi online; DB chi duoc doc bat dong bo, 5 phut/lan hoac sau moi Boss. */
public final class RankingManager {
    public enum Category {
        BOSS_KILLS("boss_kills", false, "Boss tiêu diệt"),
        COMPLETIONS("completions", false, "Dungeon hoàn thành"),
        DAMAGE("total_damage", false, "Tổng sát thương"),
        POINTS("total_points", false, "Điểm đóng góp"),
        BEST_TIME("best_time", true, "Thời gian nhanh nhất");

        public final String column;
        public final boolean asc;
        public final String label;
        Category(String column, boolean asc, String label) { this.column = column; this.asc = asc; this.label = label; }
    }

    public record Entry(UUID uuid, String name, double value) {}

    private final AstraXDungeon plugin;
    private final Map<Category, List<Entry>> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Database.Stats> stats = new ConcurrentHashMap<>();

    public RankingManager(AstraXDungeon plugin) { this.plugin = plugin; }

    public void start() {
        refresh();
        long period = 20L * 60 * 5;
        Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, period, period);
    }

    public void refresh() {
        Database db = plugin.database();
        for (Category cat : Category.values()) {
            db.async(() -> db.top(cat.column, cat.asc, 50)).thenAccept(rows -> {
                List<Entry> list = new ArrayList<>();
                for (Database.TopRow r : rows) {
                    try {
                        list.add(new Entry(UUID.fromString(r.uuid()), r.name() == null ? "?" : r.name(), r.value()));
                    } catch (IllegalArgumentException ignored) { }
                }
                cache.put(cat, list);
            }).exceptionally(e -> {
                plugin.getLogger().warning("Khong tai duoc BXH " + cat + ": " + e.getMessage());
                return null;
            });
        }
    }

    public List<Entry> top(Category cat, int limit) {
        List<Entry> l = cache.getOrDefault(cat, List.of());
        return l.subList(0, Math.min(limit, l.size()));
    }

    /** 1-based; -1 neu khong nam trong top 50. */
    public int rankOf(Category cat, UUID id) {
        List<Entry> l = cache.getOrDefault(cat, List.of());
        for (int i = 0; i < l.size(); i++) if (l.get(i).uuid().equals(id)) return i + 1;
        return -1;
    }

    // ---- thong ke ca nhan (cho PlaceholderAPI, khong truy van DB tren main thread) ----
    public void loadStats(UUID id) {
        Database db = plugin.database();
        db.async(() -> db.loadStats(id.toString())).thenAccept(st -> {
            if (st != null) stats.put(id, st);
        }).exceptionally(e -> null);
    }

    public Database.Stats stats(UUID id) { return stats.get(id); }
    public void unload(UUID id) { stats.remove(id); }

    /** Cap do Dungeon suy ra tu tong diem dong gop. */
    public static int levelOf(double totalPoints) { return 1 + (int) Math.sqrt(Math.max(0, totalPoints) / 100.0); }
}
