package com.astrax.dungeon.database;

import com.astrax.dungeon.AstraXDungeon;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/** SQLite mac dinh, MySQL tuy chon. Moi thao tac chay tren 1 luong rieng (khong chan main thread). */
public final class Database {
    public record TopRow(String uuid, String name, double value) {}
    public record MailRow(String entryId, String bossId, String dropId, double amount) {}
    public record QuestRow(String questId, String periodKey, int progress, int claimed) {}
    public record StreakRow(String lastDay, int streak, int best) {}
    public record Stats(int completions, int bossKills, double points, double damage, int bestTime) {}

    private final AstraXDungeon plugin;
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> new Thread(r, "AstraX-DB"));
    private String url, user = "", pass = "";
    private boolean mysql;

    public Database(AstraXDungeon plugin) { this.plugin = plugin; }

    public void init() {
        var c = plugin.getConfig();
        mysql = "mysql".equalsIgnoreCase(c.getString("database.type", "sqlite"));
        if (mysql) {
            url = "jdbc:mysql://" + c.getString("database.mysql.host", "localhost") + ":" + c.getInt("database.mysql.port", 3306)
                    + "/" + c.getString("database.mysql.database", "astrax") + "?useSSL=false&characterEncoding=utf8";
            user = c.getString("database.mysql.user", "root");
            pass = c.getString("database.mysql.password", "");
        } else {
            url = "jdbc:sqlite:" + new File(plugin.getDataFolder(), "astrax.db").getAbsolutePath();
            try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException ignored) { }
        }
        async(() -> { createTables(); return null; })
                .exceptionally(e -> { plugin.getLogger().severe("Khong tao duoc bang DB: " + e.getMessage()); return null; });
    }

    public void shutdown() {
        exec.shutdown();
        try { exec.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
    }

    public <T> CompletableFuture<T> async(Callable<T> job) {
        return CompletableFuture.supplyAsync(() -> {
            try { return job.call(); } catch (Exception e) { throw new CompletionException(e); }
        }, exec);
    }

    private Connection conn() throws SQLException { return DriverManager.getConnection(url, user, pass); }

    private void createTables() throws SQLException {
        try (Connection c = conn(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS player_stats (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(32), "
                    + "completions INT DEFAULT 0, boss_kills INT DEFAULT 0, total_points DOUBLE DEFAULT 0, "
                    + "total_damage DOUBLE DEFAULT 0, best_time INT DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS reward_claims (session_id VARCHAR(36), uuid VARCHAR(36), ts BIGINT, "
                    + "PRIMARY KEY (session_id, uuid))");
            st.execute("CREATE TABLE IF NOT EXISTS reward_log (ts BIGINT, uuid VARCHAR(36), session_id VARCHAR(36), "
                    + "dungeon VARCHAR(64), item VARCHAR(64), rarity VARCHAR(16), amount DOUBLE)");
            st.execute("CREATE TABLE IF NOT EXISTS pity (uuid VARCHAR(36), drop_id VARCHAR(64), misses INT, "
                    + "PRIMARY KEY (uuid, drop_id))");
            st.execute("CREATE TABLE IF NOT EXISTS mailbox (entry_id VARCHAR(36) PRIMARY KEY, uuid VARCHAR(36), "
                    + "boss_id VARCHAR(64), drop_id VARCHAR(64), amount DOUBLE, ts BIGINT)");
            st.execute("CREATE TABLE IF NOT EXISTS shop_purchases (uuid VARCHAR(36), item_id VARCHAR(64), "
                    + "day_key VARCHAR(10), cnt INT, PRIMARY KEY (uuid, item_id, day_key))");
            st.execute("CREATE TABLE IF NOT EXISTS quest_progress (uuid VARCHAR(36), quest_id VARCHAR(64), "
                    + "period_key VARCHAR(16), progress INT, claimed INT, PRIMARY KEY (uuid, quest_id, period_key))");
            st.execute("CREATE TABLE IF NOT EXISTS streaks (uuid VARCHAR(36) PRIMARY KEY, last_day VARCHAR(10), "
                    + "streak INT, best INT)");
        }
    }

    // ---- chong nhan thuong trung: moi (session, nguoi choi) chi claim duoc 1 lan ----
    public boolean claim(String sessionId, String uuid) throws SQLException {
        String sql = (mysql ? "INSERT IGNORE" : "INSERT OR IGNORE") + " INTO reward_claims(session_id, uuid, ts) VALUES(?,?,?)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setString(2, uuid);
            ps.setLong(3, System.currentTimeMillis());
            return ps.executeUpdate() == 1;
        }
    }

    // ---- pity ----
    public Map<String, Integer> loadPity(String uuid) throws SQLException {
        Map<String, Integer> out = new HashMap<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("SELECT drop_id, misses FROM pity WHERE uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getInt(2));
            }
        }
        return out;
    }

    public void savePity(String uuid, Map<String, Integer> pity) throws SQLException {
        try (Connection c = conn()) {
            for (Map.Entry<String, Integer> e : pity.entrySet()) {
                int rows;
                try (PreparedStatement up = c.prepareStatement("UPDATE pity SET misses=? WHERE uuid=? AND drop_id=?")) {
                    up.setInt(1, e.getValue()); up.setString(2, uuid); up.setString(3, e.getKey());
                    rows = up.executeUpdate();
                }
                if (rows == 0) {
                    try (PreparedStatement in = c.prepareStatement("INSERT INTO pity(uuid, drop_id, misses) VALUES(?,?,?)")) {
                        in.setString(1, uuid); in.setString(2, e.getKey()); in.setInt(3, e.getValue());
                        in.executeUpdate();
                    }
                }
            }
        }
    }

    // ---- thong ke ----
    public void addStats(String uuid, String name, double points, double damage, int seconds) throws SQLException {
        try (Connection c = conn()) {
            int rows;
            try (PreparedStatement up = c.prepareStatement("UPDATE player_stats SET name=?, completions=completions+1, "
                    + "boss_kills=boss_kills+1, total_points=total_points+?, total_damage=total_damage+?, "
                    + "best_time=CASE WHEN best_time=0 OR ? < best_time THEN ? ELSE best_time END WHERE uuid=?")) {
                up.setString(1, name); up.setDouble(2, points); up.setDouble(3, damage);
                up.setInt(4, seconds); up.setInt(5, seconds); up.setString(6, uuid);
                rows = up.executeUpdate();
            }
            if (rows == 0) {
                try (PreparedStatement in = c.prepareStatement("INSERT INTO player_stats(uuid, name, completions, boss_kills, "
                        + "total_points, total_damage, best_time) VALUES(?,?,1,1,?,?,?)")) {
                    in.setString(1, uuid); in.setString(2, name); in.setDouble(3, points);
                    in.setDouble(4, damage); in.setInt(5, seconds);
                    in.executeUpdate();
                }
            }
        }
    }

    public Stats loadStats(String uuid) throws SQLException {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT completions, boss_kills, total_points, total_damage, best_time FROM player_stats WHERE uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Stats(rs.getInt(1), rs.getInt(2), rs.getDouble(3), rs.getDouble(4), rs.getInt(5));
            }
        }
    }

    /** column chi nhan tu enum noi bo (khong tu nguoi dung) nen an toan SQL. */
    public java.util.List<TopRow> top(String column, boolean asc, int limit) throws SQLException {
        String where = column.equals("best_time") ? " WHERE best_time>0" : " WHERE " + column + ">0";
        String sql = "SELECT uuid, name, " + column + " FROM player_stats" + where
                + " ORDER BY " + column + (asc ? " ASC" : " DESC") + " LIMIT ?";
        java.util.List<TopRow> out = new java.util.ArrayList<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new TopRow(rs.getString(1), rs.getString(2), rs.getDouble(3)));
            }
        }
        return out;
    }

    // ---------------- hop thu thuong ----------------
    public void addMail(String uuid, String bossId, String dropId, double amount) throws SQLException {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO mailbox(entry_id, uuid, boss_id, drop_id, amount, ts) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, java.util.UUID.randomUUID().toString());
            ps.setString(2, uuid); ps.setString(3, bossId); ps.setString(4, dropId);
            ps.setDouble(5, amount); ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public java.util.List<MailRow> loadMail(String uuid) throws SQLException {
        java.util.List<MailRow> out = new java.util.ArrayList<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT entry_id, boss_id, drop_id, amount FROM mailbox WHERE uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new MailRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4)));
            }
        }
        return out;
    }

    /** true neu xoa duoc (dam bao moi muc chi duoc nhan 1 lan). */
    public boolean deleteMail(String entryId) throws SQLException {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("DELETE FROM mailbox WHERE entry_id=?")) {
            ps.setString(1, entryId);
            return ps.executeUpdate() == 1;
        }
    }

    public int countMail(String uuid) throws SQLException {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM mailbox WHERE uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    // ---------------- cua hang: gioi han mua moi ngay ----------------
    public boolean tryPurchase(String uuid, String itemId, String dayKey, int limit) throws SQLException {
        try (Connection c = conn()) {
            try (PreparedStatement up = c.prepareStatement(
                    "UPDATE shop_purchases SET cnt=cnt+1 WHERE uuid=? AND item_id=? AND day_key=? AND cnt<?")) {
                up.setString(1, uuid); up.setString(2, itemId); up.setString(3, dayKey); up.setInt(4, limit);
                if (up.executeUpdate() == 1) return true;
            }
            try (PreparedStatement q = c.prepareStatement(
                    "SELECT cnt FROM shop_purchases WHERE uuid=? AND item_id=? AND day_key=?")) {
                q.setString(1, uuid); q.setString(2, itemId); q.setString(3, dayKey);
                try (ResultSet rs = q.executeQuery()) { if (rs.next()) return false; }
            }
            try (PreparedStatement in = c.prepareStatement(
                    "INSERT INTO shop_purchases(uuid, item_id, day_key, cnt) VALUES(?,?,?,1)")) {
                in.setString(1, uuid); in.setString(2, itemId); in.setString(3, dayKey);
                return in.executeUpdate() == 1;
            } catch (SQLException dup) {
                return false;
            }
        }
    }

    public void refundPurchase(String uuid, String itemId, String dayKey) throws SQLException {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "UPDATE shop_purchases SET cnt=cnt-1 WHERE uuid=? AND item_id=? AND day_key=? AND cnt>0")) {
            ps.setString(1, uuid); ps.setString(2, itemId); ps.setString(3, dayKey);
            ps.executeUpdate();
        }
    }

    // ---------------- nhiem vu + streak ----------------
    public java.util.List<QuestRow> loadQuests(String uuid, java.util.List<String> periods) throws SQLException {
        java.util.List<QuestRow> out = new java.util.ArrayList<>();
        if (periods.isEmpty()) return out;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < periods.size(); i++) in.append(i == 0 ? "?" : ",?");
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT quest_id, period_key, progress, claimed FROM quest_progress WHERE uuid=? AND period_key IN (" + in + ")")) {
            ps.setString(1, uuid);
            for (int i = 0; i < periods.size(); i++) ps.setString(i + 2, periods.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new QuestRow(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4)));
            }
        }
        return out;
    }

    public void saveQuest(String uuid, String questId, String periodKey, int progress, int claimed) throws SQLException {
        try (Connection c = conn()) {
            int rows;
            try (PreparedStatement up = c.prepareStatement(
                    "UPDATE quest_progress SET progress=?, claimed=? WHERE uuid=? AND quest_id=? AND period_key=?")) {
                up.setInt(1, progress); up.setInt(2, claimed); up.setString(3, uuid);
                up.setString(4, questId); up.setString(5, periodKey);
                rows = up.executeUpdate();
            }
            if (rows == 0) {
                try (PreparedStatement in = c.prepareStatement(
                        "INSERT INTO quest_progress(uuid, quest_id, period_key, progress, claimed) VALUES(?,?,?,?,?)")) {
                    in.setString(1, uuid); in.setString(2, questId); in.setString(3, periodKey);
                    in.setInt(4, progress); in.setInt(5, claimed);
                    in.executeUpdate();
                }
            }
        }
    }

    public StreakRow loadStreak(String uuid) throws SQLException {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT last_day, streak, best FROM streaks WHERE uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new StreakRow(rs.getString(1), rs.getInt(2), rs.getInt(3)) : null;
            }
        }
    }

    public void saveStreak(String uuid, String lastDay, int streak, int best) throws SQLException {
        try (Connection c = conn()) {
            int rows;
            try (PreparedStatement up = c.prepareStatement("UPDATE streaks SET last_day=?, streak=?, best=? WHERE uuid=?")) {
                up.setString(1, lastDay); up.setInt(2, streak); up.setInt(3, best); up.setString(4, uuid);
                rows = up.executeUpdate();
            }
            if (rows == 0) {
                try (PreparedStatement in = c.prepareStatement("INSERT INTO streaks(uuid, last_day, streak, best) VALUES(?,?,?,?)")) {
                    in.setString(1, uuid); in.setString(2, lastDay); in.setInt(3, streak); in.setInt(4, best);
                    in.executeUpdate();
                }
            }
        }
    }

    public void log(String uuid, String sessionId, String dungeon, String item, String rarity, double amount) throws SQLException {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reward_log(ts, uuid, session_id, dungeon, item, rarity, amount) VALUES(?,?,?,?,?,?,?)")) {
            ps.setLong(1, System.currentTimeMillis()); ps.setString(2, uuid); ps.setString(3, sessionId);
            ps.setString(4, dungeon); ps.setString(5, item); ps.setString(6, rarity); ps.setDouble(7, amount);
            ps.executeUpdate();
        }
    }
}
