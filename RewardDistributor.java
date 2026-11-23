package com.astrax.dungeon.reward;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.database.Database;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.dungeon.Modifier;
import com.astrax.dungeon.party.Party;
import com.astrax.dungeon.tier.Tier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chia thuong rieng tung nguoi theo dong gop va Tier. Chong lap: co {@code markRewarded} cua session
 * + bang reward_claims (session_id, uuid) trong DB. Nguoi AFK / it dong gop khong du dieu kien.
 * Nguoi da thoat game (nhung dong gop du) nhan thuong qua HOP THU (/dungeon claim).
 * Ho tro: guaranteed, only-top, pity, party bonus, streak bonus, bonus MVP, tien do nhiem vu, Discord.
 */
public final class RewardDistributor {
    private record Cand(UUID id, String name, double points, double damage, double ratio, double partyBonus, int rank) {}
    private record Got(String itemId, String rarity, double amount) {}
    private record Mail(UUID id, String dropId, double amount) {}

    private final AstraXDungeon plugin;

    public RewardDistributor(AstraXDungeon plugin) { this.plugin = plugin; }

    public void distribute(DungeonSession s, long clearSeconds) {
        if (!s.markRewarded()) return;
        var m = plugin.messages();
        var cfg = plugin.getConfig();
        Tier tier = s.tier();

        LootTable table = plugin.rewards().get(s.def().bossId());
        if (table == null) {
            plugin.getLogger().warning("Khong co rewards/" + s.def().bossId() + ".yml - bo qua phat thuong.");
            return;
        }

        double minPts = tier.minPoints();
        long afkMs = cfg.getLong("reward.afk-seconds", 120) * 1000L;
        long now = System.currentTimeMillis();

        List<UUID> ok = new ArrayList<>();
        for (UUID id : s.participants()) {
            if (s.eliminated().contains(id) || s.left().contains(id)) continue;
            Player p = Bukkit.getPlayer(id);
            boolean online = p != null && p.isOnline();
            if (online && !s.players().contains(id)) continue;       // dang online nhung khong con trong Dungeon
            long ref = online ? now : s.quitAt(id);                   // offline: xet hoat dong truoc luc thoat
            boolean enough = s.points().getOrDefault(id, 0.0) >= minPts;
            boolean active = ref - s.lastActive(id) <= afkMs;
            if (enough && active) ok.add(id);
            else if (online) p.sendMessage(m.get("reward-ineligible"));
        }
        if (ok.isEmpty()) return;

        // xep hang sat thuong len Boss
        List<Map.Entry<UUID, Double>> sorted = s.bossDamage().entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed()).toList();
        Map<UUID, Integer> rank = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) rank.put(sorted.get(i).getKey(), i + 1);

        double total = 0;
        for (UUID id : ok) total += s.points().getOrDefault(id, 0.0);
        double avg = total / ok.size();
        Set<UUID> okSet = new HashSet<>(ok);

        List<Cand> cands = new ArrayList<>();
        for (UUID id : ok) {
            double pts = s.points().getOrDefault(id, 0.0);
            double ratio = avg > 0 ? pts / avg : 1.0;
            Party party = plugin.parties().partyOf(id);
            int cnt = 0;
            if (party != null) for (UUID u : party.members()) if (okSet.contains(u)) cnt++;
            double bonus = cnt > 1 ? Math.min(0.25, 0.05 * (cnt - 1)) : 0;
            String name = Bukkit.getOfflinePlayer(id).getName();
            cands.add(new Cand(id, name == null ? "?" : name, pts, s.bossDamage().getOrDefault(id, 0.0), ratio, bonus,
                    rank.getOrDefault(id, 999)));
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
        Tier tier = s.tier();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Rarity broadcastMin = Rarity.parse(cfg.getString("reward.broadcast-min-rarity", "LEGENDARY"), Rarity.LEGENDARY);
        double modMult = s.modifier() == Modifier.DOUBLE_LOOT ? 2.0 : 1.0;
        double topBonus = cfg.getDouble("reward.top-damage-bonus", 1.25);
        Map<UUID, List<Got>> logs = new HashMap<>();
        List<Mail> mails = new ArrayList<>();

        for (Cand c : cands) {
            Map<String, Integer> pity = pityAll.get(c.id());
            if (pity == null) continue;
            Player p = Bukkit.getPlayer(c.id());
            boolean online = p != null && p.isOnline();
            List<Got> got = new ArrayList<>();

            if (online) {
                p.sendMessage(m.plain("reward-header"));
                if (c.rank() == 1) p.sendMessage(m.plain("reward-mvp"));
            }
            double money = t.baseMoney() * clamp(c.ratio(), 0.5, 2.0) * tier.moneyMult();
            if (money > 0) {
                if (online) {
                    giveMoney(p, money);
                    p.sendMessage(m.plain("reward-money", "money", (long) money));
                } else {
                    mails.add(new Mail(c.id(), "money", money));
                }
                got.add(new Got("money", "COMMON", money));
            }

            double streakBonus = plugin.quests().streakBonus(c.id());
            double chanceMult = clamp(0.75 + 0.25 * c.ratio(), 0.75, 1.25)
                    * s.dropMultiplier() * modMult * tier.chanceMult() * (1 + c.partyBonus()) * (1 + streakBonus)
                    * (c.rank() == 1 ? topBonus : 1.0);

            for (LootTable.Drop d : t.drops()) {
                if (d.onlyTop() > 0 && c.rank() > d.onlyTop()) continue;

                boolean hit;
                int misses = pity.getOrDefault(d.id(), 0);
                if (d.guaranteed()) {
                    hit = true;
                } else {
                    double eff = d.chance() * chanceMult;
                    if (d.pityStep() > 0) eff += Math.min(d.pityMax(), d.pityStep() * misses);
                    eff = Math.min(100.0, eff);
                    hit = r.nextDouble() * 100.0 < eff;
                }

                if (hit) {
                    int amount = d.maxAmount() > d.amount() ? r.nextInt(d.amount(), d.maxAmount() + 1) : d.amount();
                    String shown = d.rarity().color() + d.name();
                    if (online) {
                        give(p, d, amount);
                        p.sendMessage(m.plain("reward-item", "item", shown, "amount", amount));
                    } else {
                        mails.add(new Mail(c.id(), d.id(), amount));
                    }
                    got.add(new Got(d.id(), d.rarity().name(), amount));
                    if (d.pityStep() > 0) pity.put(d.id(), 0);
                    if (!d.guaranteed() && d.rarity().ordinal() >= broadcastMin.ordinal()) {
                        Bukkit.broadcast(m.get("reward-broadcast", "player", c.name(), "item", shown,
                                "dungeon", s.def().displayName()));
                        if (cfg.getBoolean("discord.notify-rare", true)) {
                            plugin.discord().send("★ **" + c.name() + "** vừa nhận **" + d.name() + "** (" + d.rarity().name()
                                    + ") từ " + s.def().displayName());
                        }
                    }
                } else if (d.pityStep() > 0) {
                    pity.put(d.id(), misses + 1);
                }
            }

            if (online) {
                if (c.partyBonus() > 0) p.sendMessage(m.plain("reward-party-bonus", "bonus", (int) Math.round(c.partyBonus() * 100)));
                if (streakBonus > 0) {
                    p.sendMessage(m.plain("reward-streak", "days", plugin.quests().effectiveStreak(c.id()),
                            "bonus", (int) Math.round(streakBonus * 100)));
                }
                // tien do nhiem vu
                plugin.quests().progress(c.id(), "CLEAR_DUNGEON", "", 1);
                plugin.quests().progress(c.id(), "CLEAR_TIER", tier.key(), 1);
                if (s.deaths(c.id()) == 0) plugin.quests().progress(c.id(), "NO_DEATH_CLEAR", "", 1);
            }
            logs.put(c.id(), got);
        }

        // luu DB bat dong bo: pity, thong ke, lich su, hop thu
        final String sid = s.sessionId();
        final String dungeon = s.def().id();
        final String bossId = s.def().bossId();
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
            for (Mail ml : mails) {
                try {
                    db.addMail(ml.id().toString(), bossId, ml.dropId(), ml.amount());
                } catch (Exception ex) {
                    plugin.getLogger().warning("DB loi khi luu hop thu: " + ex.getMessage());
                }
            }
            return null;
        }).thenRun(() -> {
            plugin.ranking().refresh();
            for (Cand c : cands) plugin.ranking().loadStats(c.id());
        });
    }

    public void giveMoney(Player p, double amount) {
        if (plugin.vault() != null) { plugin.vault().give(p, amount); return; }
        String cmd = plugin.getConfig().getString("reward.money-command", "");
        if (cmd == null || cmd.isBlank()) {
            plugin.getLogger().warning("Khong co Vault va reward.money-command trong config => khong the tra tien.");
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                cmd.replace("{player}", p.getName()).replace("{amount}", String.valueOf((long) amount)));
    }

    public void give(Player p, LootTable.Drop d, int amount) {
        switch (d.type()) {
            case "ITEM" -> {
                ItemStack item = plugin.rewards().buildItem(d, amount);
                for (ItemStack left : p.getInventory().addItem(item).values()) {
                    Item drop = p.getWorld().dropItem(p.getLocation(), left);
                    drop.setOwner(p.getUniqueId()); // chi chu so huu nhat duoc
                }
            }
            case "COMMAND" -> {
                for (String cmd : d.commands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            cmd.replace("{player}", p.getName()).replace("{amount}", String.valueOf(amount)));
                }
            }
            case "MONEY" -> giveMoney(p, d.money());
            default -> plugin.getLogger().warning("Loai phan thuong khong ho tro: " + d.type());
        }
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
