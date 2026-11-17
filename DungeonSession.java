package com.astrax.dungeon.dungeon;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DungeonSession {
    private final String sessionId = UUID.randomUUID().toString();
    private final DungeonDefinition def;
    private final Modifier modifier;
    private final double contributionMultiplier;
    private final double dropMultiplier;
    private final Instant openedAt = Instant.now();
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Double> bossDamage = new ConcurrentHashMap<>();
    private final Map<UUID, Double> points = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActive = new ConcurrentHashMap<>();
    private final AtomicBoolean rewarded = new AtomicBoolean(false);
    private volatile DungeonState state = DungeonState.WAITING;

    public DungeonSession(DungeonDefinition def, Modifier modifier, double contribMult, double dropMult) {
        this.def = def;
        this.modifier = modifier;
        this.contributionMultiplier = contribMult;
        this.dropMultiplier = dropMult;
    }

    public String sessionId() { return sessionId; }
    public DungeonDefinition def() { return def; }
    public Modifier modifier() { return modifier; }
    public double contributionMultiplier() { return contributionMultiplier; }
    public double dropMultiplier() { return dropMultiplier; }
    public Instant openedAt() { return openedAt; }
    public Set<UUID> players() { return players; }
    public DungeonState state() { return state; }
    public void state(DungeonState s) { this.state = s; }

    public void addBossDamage(UUID id, double d) { bossDamage.merge(id, d, Double::sum); }
    /** Diem dong gop, da nhan he so Gio Vang. */
    public void addPoints(UUID id, double p) { points.merge(id, p * contributionMultiplier, Double::sum); }
    public Map<UUID, Double> bossDamage() { return bossDamage; }
    public Map<UUID, Double> points() { return points; }

    /** Ghi nhan hoat dong that (danh quai/boss) de chong AFK. */
    public void touch(UUID id) { lastActive.put(id, System.currentTimeMillis()); }
    public long lastActive(UUID id) { return lastActive.getOrDefault(id, 0L); }

    /** Chi tra ve true dung 1 lan => Boss chet khong the phat thuong 2 lan. */
    public boolean markRewarded() { return rewarded.compareAndSet(false, true); }
}
