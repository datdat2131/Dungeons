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

    public void log(String uuid, String sessionId, String dungeon, String item, String rarity, double amount) throws SQLException {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reward_log(ts, uuid, session_id, dungeon, item, rarity, amount) VALUES(?,?,?,?,?,?,?)")) {
            ps.setLong(1, System.currentTimeMillis()); ps.setString(2, uuid); ps.setString(3, sessionId);
            ps.setString(4, dungeon); ps.setString(5, item); ps.setString(6, rarity); ps.setDouble(7, amount);
            ps.executeUpdate();
        }
    }
}
