package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.quest.QuestManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QuestMenu extends Menu {
    private static final int[] DAILY = {11, 13, 15};
    private static final int[] WEEKLY = {29, 31, 33};
    private final Player viewer;
    private final Map<Integer, QuestManager.Quest> bySlot = new HashMap<>();

    public QuestMenu(AstraXDungeon plugin, Player viewer) {
        super(plugin, 5, "&8Nhiệm vụ");
        this.viewer = viewer;
    }

    @Override
    protected void build() {
        border();
        bySlot.clear();
        var qm = plugin.quests();
        var id = viewer.getUniqueId();

        int streak = qm.effectiveStreak(id);
        inv.setItem(4, Items.make(Material.BLAZE_POWDER, "&6Chuỗi ngày: &f" + streak,
                "&7Kỷ lục: &f" + qm.bestStreak(id) + " ngày",
                "&7Bonus tỷ lệ rơi: &a+" + (int) Math.round(qm.streakBonus(id) * 100) + "%",
                "&8Vào Dungeon mỗi ngày để duy trì"));

        place(DAILY, qm.active("DAILY"), "&b[NGÀY]");
        place(WEEKLY, qm.active("WEEKLY"), "&d[TUẦN]");
        inv.setItem(40, Items.make(Material.ARROW, "&7Quay lại"));
    }

    private void place(int[] slots, List<QuestManager.Quest> quests, String tag) {
        var qm = plugin.quests();
        for (int i = 0; i < quests.size() && i < slots.length; i++) {
            QuestManager.Quest q = quests.get(i);
            int[] st = qm.state(viewer.getUniqueId(), q);
            boolean done = st[0] >= q.amount();
            boolean claimed = st[1] == 1;

            int filled = (int) Math.round(10.0 * Math.min(st[0], q.amount()) / Math.max(1, q.amount()));
            String bar = "&a" + "▮".repeat(filled) + "&8" + "▮".repeat(10 - filled);

            List<String> lore = new ArrayList<>();
            lore.add(tag);
            lore.add("&7Tiến độ: &f" + Math.min(st[0], q.amount()) + "/" + q.amount());
            lore.add(bar);
            lore.add(" ");
            lore.add("&7Phần thưởng:");
            if (q.money() > 0) lore.add("&a - " + (long) q.money() + " tiền");
            for (var d : q.items()) lore.add("&a - " + d.amount() + "x " + d.name());
            lore.add(" ");
            lore.add(claimed ? "&8Đã nhận" : done ? "&e▶ Bấm để nhận thưởng" : "&7Chưa hoàn thành");

            Material mat = claimed ? Material.GRAY_DYE : done ? Material.EMERALD : Material.PAPER;
            ItemStack item = Items.make(mat, (claimed ? "&8" : "&f") + q.name(), lore);
            if (done && !claimed) Items.glow(item);
            inv.setItem(slots[i], item);
            bySlot.put(slots[i], q);
        }
    }

    @Override
    public void click(Player p, int slot, ClickType type) {
        if (slot == 40) { new MainMenu(plugin, p).open(p); return; }
        QuestManager.Quest q = bySlot.get(slot);
        if (q == null) return;
        plugin.quests().claim(p, q);
        refresh();
    }
}
