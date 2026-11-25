package com.astrax.dungeon.dungeon;

import com.astrax.dungeon.tier.Tier;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DungeonSession {
    private final String sessionId = UUID.randomUUID().toString();
    private final DungeonDefinition def;
    private final Tier tier;
    private final Modifier modifier;
    private final double contributionMultiplier;
    private final double dropMultiplier;
    private final Instant openedAt = Instant.now();
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();       // dang o trong Dungeon
    private final Set<UUID> participants = ConcurrentHashMap.newKeySet();  // tung tham gia (ke ca da thoat game)
    private final Set<UUID> left = ConcurrentHashMap.newKeySet();          // tu y roi bang /dungeon leave
    private final Set<UUID> eliminated = ConcurrentHashMap.newKeySet();
    private final Set<UUID> downed = ConcurrentHashMap.newKeySet();        // dang guc, cho dong doi cuu
    private final Map<UUID, Integer> lives = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> deaths = new ConcurrentHashMap<>();
    private final Map<UUID, Double> bossDamage = new ConcurrentHashMap<>();
    private final Map<UUID, Double> points = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActive = new ConcurrentHashMap<>();
    private final Map<UUID, Long> quitAt = new ConcurrentHashMap<>();
    private final AtomicBoolean rewarded = new AtomicBoolean(false);
    private volatile DungeonState state = DungeonState.WAITING;
    private volatile long joinClosesAt = Long.MAX_VALUE;

    public DungeonSession(DungeonDefinition def, Tier tier, Modifier modifier, double contribMult, double dropMult) {
        this.def = def;
        this.tier = tier;
        this.modifier = modifier;
        this.contributionMultiplier = contribMult;
        this.dropMultiplier = dropMult;
    }

    public String sessionId() { return sessionId; }

    /** Thoi diem (ms) cong Dungeon dong han: nguoi moi khong the vao nua. */
    public void joinClosesAt(long epochMs) { this.joinClosesAt = epochMs; }
    public boolean joinOpen() { return System.currentTimeMillis() <= joinClosesAt; }
    public long joinSecondsLeft() { return Math.max(0, (joinClosesAt - System.currentTimeMillis()) / 1000); }
    public DungeonDefinition def() { return def; }
    public Tier tier() { return tier; }
    public Modifier modifier() { return modifier; }
    public double contributionMultiplier() { return contributionMultiplier; }
    public double dropMultiplier() { return dropMultiplier; }
    public Instant openedAt() { return openedAt; }
    public Set<UUID> players() { return players; }
    public Set<UUID> participants() { return participants; }
    public Set<UUID> left() { return left; }
    public DungeonState state() { return state; }
    public void state(DungeonState s) { this.state = s; }

    public void addBossDamage(UUID id, double d) { bossDamage.merge(id, d, Double::sum); }
    /** Diem dong gop, nhan he so Gio Vang va he so diem cua Tier. */
    public void addPoints(UUID id, double p) { points.merge(id, p * contributionMultiplier * tier.pointsMult(), Double::sum); }
    public Map<UUID, Double> bossDamage() { return bossDamage; }
    public Map<UUID, Double> points() { return points; }

    /** Ghi nhan hoat dong that (danh quai/boss) de chong AFK. */
    public void touch(UUID id) { lastActive.put(id, System.currentTimeMillis()); }
    public long lastActive(UUID id) { return lastActive.getOrDefault(id, 0L); }
    public void markQuit(UUID id) { quitAt.put(id, System.currentTimeMillis()); }
    public long quitAt(UUID id) { return quitAt.getOrDefault(id, System.currentTimeMillis()); }

    /** Chi tra ve true dung 1 lan => Boss chet khong the phat thuong 2 lan. */
    public boolean markRewarded() { return rewarded.compareAndSet(false, true); }

    // ---- mang / guc / loai ----
    public Set<UUID> eliminated() { return eliminated; }
    public Set<UUID> downed() { return downed; }
    /** -1 = vo han. */
    public int livesLeft(UUID id) { return lives.getOrDefault(id, tier.lives()); }
    public void setLives(UUID id, int n) { lives.put(id, n); }
    /** Tru 1 mang, tra ve so mang con lai (-1 neu vo han). */
    public int loseLife(UUID id) {
        int l = livesLeft(id);
        if (l < 0) return -1;
        l = Math.max(0, l - 1);
        lives.put(id, l);
        return l;
    }
    public void addDeath(UUID id) { deaths.merge(id, 1, Integer::sum); }
    public int deaths(UUID id) { return deaths.getOrDefault(id, 0); }
}
