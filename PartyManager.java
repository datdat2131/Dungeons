package com.astrax.dungeon.party;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Party toi da 10 nguoi. Party KHONG tao Dungeon rieng: chi keo nhau vao Dungeon chung + bonus ty le roi. */
public final class PartyManager {
    private record Invite(UUID leader, long expires) {}

    private final AstraXDungeon plugin;
    private final Map<UUID, Party> byPlayer = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();

    public PartyManager(AstraXDungeon plugin) { this.plugin = plugin; }

    public Party partyOf(UUID id) { return byPlayer.get(id); }
    private int max() { return plugin.getConfig().getInt("party.max-size", 10); }
    private static String name(UUID id) {
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n == null ? "?" : n;
    }

    public void create(Player p) {
        var m = plugin.messages();
        if (byPlayer.containsKey(p.getUniqueId())) { p.sendMessage(m.get("party-exists")); return; }
        byPlayer.put(p.getUniqueId(), new Party(p.getUniqueId()));
        p.sendMessage(m.get("party-created"));
    }

    public void invite(Player leader, String targetName) {
        var m = plugin.messages();
        UUID id = leader.getUniqueId();
        if (!byPlayer.containsKey(id)) create(leader); // tu tao Party khi moi
        Party party = byPlayer.get(id);
        if (!party.isLeader(id)) { leader.sendMessage(m.get("party-leader-only")); return; }
        Player t = Bukkit.getPlayerExact(targetName);
        if (t == null || t.equals(leader)) { leader.sendMessage(m.get("player-offline")); return; }
        if (byPlayer.containsKey(t.getUniqueId())) { leader.sendMessage(m.get("party-target-in-party", "name", t.getName())); return; }
        if (party.size() >= max()) { leader.sendMessage(m.get("party-full")); return; }

        long ttl = plugin.getConfig().getLong("party.invite-seconds", 60) * 1000L;
        invites.put(t.getUniqueId(), new Invite(id, System.currentTimeMillis() + ttl));
        leader.sendMessage(m.get("party-invite-sent", "name", t.getName()));
        t.sendMessage(Text.c(m.raw("prefix") + m.raw("party-invited", "name", leader.getName()))
                .clickEvent(ClickEvent.runCommand("/dungeon party accept")));
    }

    public void accept(Player p) {
        var m = plugin.messages();
        UUID id = p.getUniqueId();
        Invite inv = invites.remove(id);
        if (inv == null || inv.expires() < System.currentTimeMillis()) { p.sendMessage(m.get("party-no-invite")); return; }
        Party party = byPlayer.get(inv.leader());
        if (party == null) { p.sendMessage(m.get("party-no-invite")); return; }
        if (byPlayer.containsKey(id)) { p.sendMessage(m.get("party-exists")); return; }
        if (party.size() >= max()) { p.sendMessage(m.get("party-full")); return; }
        party.members().add(id);
        byPlayer.put(id, party);
        broadcast(party, m.get("party-joined", "name", p.getName()));
    }

    public void leave(Player p) {
        if (!byPlayer.containsKey(p.getUniqueId())) { p.sendMessage(plugin.messages().get("party-none")); return; }
        p.sendMessage(plugin.messages().get("party-you-left"));
        remove(p.getUniqueId());
    }

    public void kick(Player leader, String targetName) {
        var m = plugin.messages();
        Party party = byPlayer.get(leader.getUniqueId());
        if (party == null) { leader.sendMessage(m.get("party-none")); return; }
        if (!party.isLeader(leader.getUniqueId())) { leader.sendMessage(m.get("party-leader-only")); return; }
        UUID target = find(party, targetName);
        if (target == null || target.equals(leader.getUniqueId())) { leader.sendMessage(m.get("player-offline")); return; }
        Player tp = Bukkit.getPlayer(target);
        if (tp != null) tp.sendMessage(m.get("party-kicked"));
        remove(target);
    }

    public void promote(Player leader, String targetName) {
        var m = plugin.messages();
        Party party = byPlayer.get(leader.getUniqueId());
        if (party == null) { leader.sendMessage(m.get("party-none")); return; }
        if (!party.isLeader(leader.getUniqueId())) { leader.sendMessage(m.get("party-leader-only")); return; }
        UUID target = find(party, targetName);
        if (target == null) { leader.sendMessage(m.get("player-offline")); return; }
        party.leader(target);
        broadcast(party, m.get("party-promoted", "name", name(target)));
    }

    public void info(Player p) {
        var m = plugin.messages();
        Party party = byPlayer.get(p.getUniqueId());
        if (party == null) { p.sendMessage(m.get("party-none")); return; }
        p.sendMessage(m.plain("party-info-header", "count", party.size(), "max", max(), "leader", name(party.leader())));
        for (UUID u : party.members()) {
            p.sendMessage(m.plain("party-info-member", "name", name(u),
                    "tag", plugin.dungeons().isInDungeon(u) ? " &a[trong Dungeon]" : ""));
        }
    }

    /** Doi truong vao Dungeon thi keo ca doi (member online, chua o trong Dungeon). */
    public void pullMembers(Player leader) {
        Party party = byPlayer.get(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) return;
        if (!plugin.dungeons().isInDungeon(leader.getUniqueId())) return;
        for (UUID u : party.members()) {
            Player mp = Bukkit.getPlayer(u);
            if (mp != null && !u.equals(leader.getUniqueId()) && !plugin.dungeons().isInDungeon(u)) {
                plugin.dungeons().join(mp);
            }
        }
    }

    public void handleQuit(UUID id) {
        invites.remove(id);
        remove(id);
    }

    private void remove(UUID id) {
        Party party = byPlayer.remove(id);
        if (party == null) return;
        party.members().remove(id);
        if (party.members().isEmpty()) return;
        var m = plugin.messages();
        broadcast(party, m.get("party-left", "name", name(id)));
        if (party.isLeader(id)) {
            party.leader(party.members().iterator().next());
            broadcast(party, m.get("party-promoted", "name", name(party.leader())));
        }
    }

    private UUID find(Party party, String n) {
        for (UUID u : party.members()) if (name(u).equalsIgnoreCase(n)) return u;
        return null;
    }

    private void broadcast(Party party, Component msg) {
        for (UUID u : party.members()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(msg);
        }
    }
}
