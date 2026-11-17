package com.astrax.dungeon.party;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    public UUID leader() { return leader; }
    public void leader(UUID id) { this.leader = id; }
    public boolean isLeader(UUID id) { return leader.equals(id); }
    public Set<UUID> members() { return members; }
    public int size() { return members.size(); }
}
