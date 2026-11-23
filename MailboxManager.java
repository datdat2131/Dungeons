package com.astrax.dungeon.reward;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Hop thu thuong: nguoi offline luc Boss chet van nhan duoc thuong bang /dungeon claim. Moi muc chi nhan 1 lan. */
public final class MailboxManager {
    private final AstraXDungeon plugin;

    public MailboxManager(AstraXDungeon plugin) { this.plugin = plugin; }

    public void claim(Player p) {
        Database db = plugin.database();
        UUID id = p.getUniqueId();
        var m = plugin.messages();
        db.async(() -> {
            List<Database.MailRow> won = new ArrayList<>();
            for (Database.MailRow r : db.loadMail(id.toString())) {
                if (db.deleteMail(r.entryId())) won.add(r); // xoa thanh cong moi duoc nhan
            }
            return won;
        }).whenComplete((rows, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (err != null || rows == null) { p.sendMessage(m.get("mail-error")); return; }
            if (rows.isEmpty()) { p.sendMessage(m.get("mail-empty")); return; }
            if (!p.isOnline()) { // vua thoat: tra lai hop thu
                db.async(() -> {
                    for (Database.MailRow r : rows) db.addMail(id.toString(), r.bossId(), r.dropId(), r.amount());
                    return null;
                });
                return;
            }
            int n = 0;
            for (Database.MailRow r : rows) if (deliver(p, r)) n++;
            p.sendMessage(m.get("mail-claimed", "count", n));
        }));
    }

    private boolean deliver(Player p, Database.MailRow r) {
        if (r.dropId().equals("money")) {
            plugin.distributor().giveMoney(p, r.amount());
            p.sendMessage(plugin.messages().plain("reward-money", "money", (long) r.amount()));
            return true;
        }
        LootTable table = plugin.rewards().get(r.bossId());
        if (table != null) {
            for (LootTable.Drop d : table.drops()) {
                if (d.id().equals(r.dropId())) {
                    plugin.distributor().give(p, d, (int) r.amount());
                    p.sendMessage(plugin.messages().plain("reward-item", "item", d.rarity().color() + d.name(),
                            "amount", (int) r.amount()));
                    return true;
                }
            }
        }
        plugin.getLogger().warning("Hop thu: khong tim thay drop '" + r.dropId() + "' cua boss '" + r.bossId() + "'");
        return false;
    }

    /** Bao khi vao game neu con thuong chua nhan. */
    public void notifyJoin(Player p) {
        Database db = plugin.database();
        db.async(() -> db.countMail(p.getUniqueId().toString())).thenAccept(c -> {
            if (c != null && c > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline()) p.sendMessage(plugin.messages().get("mail-pending", "count", c));
                }, 60L);
            }
        }).exceptionally(e -> null);
    }
}
