package com.astrax.dungeon.reward;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.database.Database;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.dungeon.Modifier;
import com.astrax.dungeon.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Chia thuong rieng tung nguoi theo dong gop. Chong lap: co {@code markRewarded} cua session
 * + bang reward_claims (session_id, uuid) trong DB. Nguoi AFK / it dong gop khong du dieu kien.
 */
public final class RewardDistributor {
    private record Cand(UUID id, String name, double points, double damage, double ratio, double partyBonus) {}
    private record Got(String itemId, String rarity, double amount) {}

    private final AstraXDungeon plugin;

    public RewardDistributor(AstraXDungeon plugin) { this.plugin = plugin; }

    public void distribute(DungeonSession s, long clearSeconds) {
        if (!s.markRewarded()) return;
        var m = plugin.messages();
        var cfg = plugin.getConfig();

        LootTable table = plugin.rewards().get(s.def().bossId());
        if (table == null) {
            plugin.getLogger().warning("Khong co rewards/" + s.def().bossId() + ".yml - bo qua phat thuong.");
            return;
        }

        double minPts = cfg.getDouble("reward.min-points", 30);
        long afkMs = cfg.getLong("reward.afk-seconds", 120) * 1000L;
        long now = System.currentTimeMillis();

        List<Player> ok = new ArrayList<>();
        for (UUID id : s.players()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            boolean enough = s.points().getOrDefault(id, 0.0) >= minPts;
            boolean active = now - s.lastActive(id) <= afkMs;
            if (enough && active) ok.add(p); else p.sendMessage(m.get("reward-ineligible"));
        }
        if (ok.isEmpty()) return;

        double total = 0;
        for (Player p : ok) total += s.points().getOrDefault(p.getUniqueId(), 0.0);
        double avg = total / ok.size();
        Set<UUID> okIds = ok.stream().map(Player::getUniqueId).collect(Collectors.toSet());

        List<Cand> cands = new ArrayList<>();
        for (Player p : ok) {
            UUID id = p.getUniqueId();
            double pts = s.points().getOrDefault(id, 0.0);
            double ratio = avg > 0 ? pts / avg : 1.0;
            Party party = plugin.parties().partyOf(id);
            int cnt = 0;
            if (party != null) for (UUID u : party.members()) if (okIds.contains(u)) cnt++;
            double bonus = cnt > 1 ? Math.min(0.25, 0.05 * (cnt - 1)) : 0;
            cands.add(new Cand(id, p.getName(), pts, s.bossDamage().getOrDefault(id, 0.0), ratio, bonus));
        }

        final String sid = s.sessionId();
        Database db = plugin.database();
        db.async(() -> {
            Map<UUID, Map<String, Integer>> pity = new HashMap<>();
            for (Cand c : cands) {
                boolean claimed = true;
                Map<String, Integer> pm = new HashMap<>();
                try {
                    claimed = db.claim(sid, c.id().toString());
                    if (claimed) pm = db.loadPity(c.id().toString());
                } catch (Exception ex) {
                    plugin.getLogger().warning("DB loi khi claim thuong: " + ex.getMessage());
                }
                if (claimed) pity.put(c.id(), pm);
            }
            return pity;
        }).whenComplete((pity, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
            Map<UUID, Map<String, Integer>> pm = pity;
            if (err != null || pm == null) {
                pm = new HashMap<>();
                for (Cand c : cands) pm.put(c.id(), new HashMap<>());
            }
            giveAll(s, table, cands, pm, clearSeconds);
        }));
    }

    private void giveAll(DungeonSession s, LootTable t, List<Cand> cands, Map<UUID, Map<String, Integer>> pityAll, long secs) {
        var m = plugin.messages();
        var cfg = plugin.getConfig();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Rarity broadcastMin = Rarity.parse(cfg.getString("reward.broadcast-min-rarity", "LEGENDARY"), Rarity.LEGENDARY);
        double modMult = s.modifier() == Modifier.DOUBLE_LOOT ? 2.0 : 1.0;
        Map<UUID, List<Got>> logs = new HashMap<>();

        for (Cand c : cands) {
            Map<String, Integer> pity = pityAll.get(c.id());
            Player p = Bukkit.getPlayer(c.id());
            if (pity == null || p == null) continue;
            List<Got> got = new ArrayList<>();

            p.sendMessage(m.plain("reward-header"));
            double money = t.baseMoney() * clamp(c.ratio(), 0.5, 2.0);
            if (money > 0) {
                giveMoney(p, money);
                got.add(new Got("money", "COMMON", money));
                p.sendMessage(m.plain("reward-money", "money", (long) money));
            }

            double chanceMult = clamp(0.75 + 0.25 * c.ratio(), 0.75, 1.25)
                    * s.dropMultiplier() * modMult * (1 + c.partyBonus());
            for (LootTable.Drop d : t.drops()) {
                int misses = pity.getOrDefault(d.id(), 0);
                double eff = d.chance() * chanceMult;
                if (d.pityStep() > 0) eff += Math.min(d.pityMax(), d.pityStep() * misses);
                eff = Math.min(100.0, eff);

                if (r.nextDouble() * 100.0 < eff) {
                    give(p, d);
                    got.add(new Got(d.id(), d.rarity().name(), d.amount()));
                    if (d.pityStep() > 0) pity.put(d.id(), 0);
                    String shown = d.rarity().color() + d.name();
                    p.sendMessage(m.plain("reward-item", "item", shown, "amount", d.amount()));
                    if (d.rarity().ordinal() >= broadcastMin.ordinal()) {
                        Bukkit.broadcast(m.get("reward-broadcast", "player", p.getName(), "item", shown,
                                "dungeon", s.def().displayName()));
                    }
                } else if (d.pityStep() > 0) {
                    pity.put(d.id(), misses + 1);
                }
            }
            if (c.partyBonus() > 0) p.sendMessage(m.plain("reward-party-bonus", "bonus", (int) Math.round(c.partyBonus() * 100)));
            logs.put(c.id(), got);
        }

        // luu DB bat dong bo: pity, thong ke, lich su
        final String sid = s.sessionId();
        final String dungeon = s.def().id();
        Database db = plugin.database();
        db.async(() -> {
            for (Cand c : cands) {
                Map<String, Integer> pity = pityAll.get(c.id());
                if (pity == null) continue;
                String u = c.id().toString();
                try {
                    db.savePity(u, pity);
                    db.addStats(u, c.name(), c.points(), c.damage(), (int) secs);
                    for (Got g : logs.getOrDefault(c.id(), List.of())) db.log(u, sid, dungeon, g.itemId(), g.rarity(), g.amount());
                } catch (Exception ex) {
                    plugin.getLogger().warning("DB loi khi luu thuong " + c.name() + ": " + ex.getMessage());
                }
            }
            return null;
        }).thenRun(() -> {
            plugin.ranking().refresh();
            for (Cand c : cands) plugin.ranking().loadStats(c.id());
        });
    }

    private void giveMoney(Player p, double amount) {
        if (plugin.vault() != null) { plugin.vault().give(p, amount); return; }
        String cmd = plugin.getConfig().getString("reward.money-command", "");
        if (cmd == null || cmd.isBlank()) {
            plugin.getLogger().warning("Khong co Vault va reward.money-command trong config => khong the tra tien.");
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                cmd.replace("{player}", p.getName()).replace("{amount}", String.valueOf((long) amount)));
    }

    private void give(Player p, LootTable.Drop d) {
        switch (d.type()) {
            case "ITEM" -> {
                ItemStack item = plugin.rewards().buildItem(d);
                for (ItemStack left : p.getInventory().addItem(item).values()) {
                    Item drop = p.getWorld().dropItem(p.getLocation(), left);
                    drop.setOwner(p.getUniqueId()); // chi chu so huu nhat duoc
                }
            }
            case "COMMAND" -> {
                for (String cmd : d.commands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName()));
                }
            }
            case "MONEY" -> giveMoney(p, d.money());
            default -> plugin.getLogger().warning("Loai phan thuong khong ho tro: " + d.type());
        }
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
