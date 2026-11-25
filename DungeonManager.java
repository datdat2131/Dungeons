package com.astrax.dungeon.dungeon;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.boss.BossController;
import com.astrax.dungeon.boss.BossDefinition;
import com.astrax.dungeon.mob.MobManager;
import com.astrax.dungeon.tier.Tier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class DungeonManager {
    private final AstraXDungeon plugin;
    private final Map<String, DungeonDefinition> defs = new LinkedHashMap<>();
    private final MobManager mobs;
    private DungeonSession active;
    private BossController boss;
    private BukkitTask joinTask;
    private BukkitTask closeTask;

    public DungeonManager(AstraXDungeon plugin) {
        this.plugin = plugin;
        this.mobs = new MobManager(plugin, this);
    }

    public void loadAll() {
        defs.clear();
        File[] files = new File(plugin.getDataFolder(), "dungeons").listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        Arrays.sort(files);
        for (File f : files) {
            try {
                DungeonDefinition d = DungeonDefinition.load(f);
                defs.put(d.id(), d);
            } catch (Exception e) {
                plugin.getLogger().warning("Loi doc " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    public Collection<DungeonDefinition> all() { return defs.values(); }
    public List<DungeonDefinition> enabled() { return defs.values().stream().filter(DungeonDefinition::enabled).toList(); }
    public Optional<DungeonDefinition> get(String id) { return Optional.ofNullable(defs.get(id.toLowerCase())); }
    public DungeonSession active() { return active; }
    public MobManager mobs() { return mobs; }
    public boolean isInDungeon(UUID id) { return active != null && active.players().contains(id); }

    public BossController bossOf(Entity e) {
        return boss != null && boss.isEntity(e) ? boss : null;
    }

    // ---------- lobby ----------
    public Location lobby() {
        var c = plugin.getConfig();
        if (!c.contains("lobby.world")) return null;
        var w = Bukkit.getWorld(c.getString("lobby.world", ""));
        if (w == null) return null;
        return new Location(w, c.getDouble("lobby.x"), c.getDouble("lobby.y"), c.getDouble("lobby.z"),
                (float) c.getDouble("lobby.yaw"), (float) c.getDouble("lobby.pitch"));
    }

    public void setLobby(Location l) {
        var c = plugin.getConfig();
        c.set("lobby.world", l.getWorld().getName());
        c.set("lobby.x", l.getX()); c.set("lobby.y", l.getY()); c.set("lobby.z", l.getZ());
        c.set("lobby.yaw", l.getYaw()); c.set("lobby.pitch", l.getPitch());
        plugin.saveConfig();
    }

    // ---------- vong doi ----------
    public void open(DungeonDefinition def, Modifier mod, double contribMult, double dropMult) {
        if (active != null) end("force-end-new");
        Tier tier = plugin.tiers().get(def.tier());
        final DungeonSession session = new DungeonSession(def, tier, mod, contribMult, dropMult);
        active = session;
        // Dungeon "bat dau" sau join-window (2 phut); them late-join (3 phut) cho nguoi vao muon; sau do dong cong han.
        int window = plugin.getConfig().getInt("dungeon.join-window-seconds", 120);
        int late = plugin.getConfig().getInt("dungeon.late-join-seconds", 180);
        session.joinClosesAt(System.currentTimeMillis() + (window + late) * 1000L);
        var m = plugin.messages();

        Bukkit.broadcast(m.get("opened", "dungeon", def.displayName(), "seconds", window));
        Bukkit.broadcast(m.get("modifier", "name", m.modifierName(mod.name()), "desc", m.modifierDesc(mod.name())));
        Bukkit.broadcast(m.get("tier-line", "tier", tier.displayName(), "waves", tier.waves(),
                "lives", tier.lives() < 0 ? "vô hạn" : String.valueOf(tier.lives())));
        if (plugin.getConfig().getBoolean("discord.notify-open", true)) {
            plugin.discord().send("🏰 **" + def.displayName() + "** (" + tier.key() + ") vừa mở! Vào server gõ /dungeon join trong " + window + " giây.");
        }
        if (contribMult > 1 || dropMult > 1) {
            Bukkit.broadcast(m.get("golden", "contrib", contribMult, "drop", dropMult));
        }

        closeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active == session) {
                broadcastSession(m.get("join-closed-announce"));
                Bukkit.broadcast(m.get("join-closed-global", "dungeon", def.displayName()));
            }
        }, (window + late) * 20L);

        joinTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active == session && session.state() == DungeonState.WAITING) {
                session.state(DungeonState.RUNNING);
                broadcastSession(m.get("started"));
                Bukkit.broadcast(m.get("join-window-late", "dungeon", def.displayName(),
                        "seconds", late, "minutes", Math.max(1, late / 60)));
                mobs.start(session);
            }
        }, window * 20L);
    }

    /** Goi moi giay boi scheduler: cuong che ket thuc khi qua gio. */
    public void tick() {
        if (active == null) return;
        long max = active.tier().maxDurationMinutes();
        if (Duration.between(active.openedAt(), Instant.now()).toMinutes() >= max) end("time-up");
    }

    /** Het wave (hoac admin ep) => vao tran Boss. */
    public boolean startBoss() {
        DungeonSession s = active;
        var m = plugin.messages();
        if (s == null) return false;
        if (s.state() == DungeonState.BOSS || s.state() == DungeonState.REWARD || s.state() == DungeonState.RESETTING) return false;

        BossDefinition bd = BossDefinition.load(new File(plugin.getDataFolder(), "bosses/" + s.def().bossId() + ".yml"));
        if (bd == null) { Bukkit.broadcast(m.get("boss-missing", "id", s.def().bossId())); return false; }
        Location loc = s.def().bossSpawn();
        if (loc == null) loc = s.def().spawn();
        if (loc == null) { Bukkit.broadcast(m.get("no-spawn")); return false; }

        mobs.stop();
        s.state(DungeonState.BOSS);
        boss = new BossController(plugin, this, s, bd);
        boss.start(loc);
        return true;
    }

    public void killBoss() {
        if (boss != null) boss.die();
    }

    public void onBossKilled(DungeonSession s) {
        if (active != s) return;
        s.state(DungeonState.REWARD);
        mobs.stop(); // don quai phu con lai
        var m = plugin.messages();

        List<Map.Entry<UUID, Double>> top = s.bossDamage().entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed()).limit(3).toList();
        int rank = 1;
        for (Map.Entry<UUID, Double> en : top) {
            String name = Bukkit.getOfflinePlayer(en.getKey()).getName();
            Bukkit.broadcast(m.get("top-damage", "rank", rank++, "name", name == null ? "?" : name,
                    "damage", en.getValue().intValue()));
        }
        long secs = Duration.between(s.openedAt(), Instant.now()).getSeconds();
        plugin.distributor().distribute(s, secs);
        long delay = plugin.getConfig().getLong("dungeon.reward-delay-seconds", 30);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (active == s) end("completed"); }, delay * 20L);
    }

    public void end(String messageKey) {
        DungeonSession s = active;
        if (s == null) return;
        s.state(DungeonState.RESETTING);
        if (joinTask != null) { joinTask.cancel(); joinTask = null; }
        if (closeTask != null) { closeTask.cancel(); closeTask = null; }
        if (boss != null) { boss.cleanup(); boss = null; }
        mobs.stop();
        plugin.revive().clearAll();
        Bukkit.broadcast(plugin.messages().get(messageKey));
        Location lobby = lobby();
        for (UUID id : s.players()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && lobby != null) p.teleport(lobby);
        }
        active = null;
    }

    // ---------- nguoi choi ----------
    public void join(Player p) {
        var m = plugin.messages();
        DungeonSession s = active;
        if (s == null) { p.sendMessage(m.get("no-active")); return; }
        if (s.eliminated().contains(p.getUniqueId())) { p.sendMessage(m.get("eliminated-block")); return; }
        if (plugin.tiers().enforceLevel() && !p.hasPermission("astrax.dungeon.admin")) {
            var st = plugin.ranking().stats(p.getUniqueId());
            int lvl = com.astrax.dungeon.ranking.RankingManager.levelOf(st == null ? 0 : st.points());
            if (lvl < s.tier().minLevel()) {
                p.sendMessage(m.get("level-too-low", "need", s.tier().minLevel(), "have", lvl));
                return;
            }
        }
        if (s.players().contains(p.getUniqueId())) { p.sendMessage(m.get("already-in")); return; }
        // nguoi da tham gia truoc do (mat ket noi) duoc vao lai bat ke cong; nguoi moi chi vao khi cong con mo
        boolean returning = s.participants().contains(p.getUniqueId()) && !s.left().contains(p.getUniqueId());
        boolean afterStart = s.state() != DungeonState.WAITING;
        if (!returning && (!s.joinOpen() || (afterStart && !plugin.getConfig().getBoolean("dungeon.late-join", true)))) {
            p.sendMessage(m.get("join-closed")); return;
        }
        if (s.state() == DungeonState.REWARD || s.state() == DungeonState.RESETTING) {
            p.sendMessage(m.get("no-active")); return;
        }
        if (s.players().size() >= s.def().maxPlayers()) { p.sendMessage(m.get("full")); return; }
        Location spawn = s.def().spawn();
        if (spawn == null) { p.sendMessage(m.get("no-spawn")); return; }
        s.players().add(p.getUniqueId());
        s.participants().add(p.getUniqueId());
        s.left().remove(p.getUniqueId());
        s.touch(p.getUniqueId());
        p.teleport(spawn);
        p.sendMessage(m.get("joined", "dungeon", s.def().displayName()));
        plugin.quests().onDungeonJoin(p);
    }

    public void leave(Player p) {
        var m = plugin.messages();
        if (!isInDungeon(p.getUniqueId())) { p.sendMessage(m.get("not-in")); return; }
        plugin.revive().release(p.getUniqueId());
        active.players().remove(p.getUniqueId());
        active.left().add(p.getUniqueId());
        Location lobby = lobby();
        if (lobby != null) p.teleport(lobby);
        p.sendMessage(m.get("left"));
    }

    public void broadcastSession(Component msg) {
        if (active == null) return;
        for (UUID id : active.players()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(msg);
        }
    }
}
