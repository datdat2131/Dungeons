package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonDefinition;
import com.astrax.dungeon.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DungeonMenu extends Menu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private final Map<Integer, DungeonDefinition> bySlot = new HashMap<>();

    public DungeonMenu(AstraXDungeon plugin) { super(plugin, 4, "&8Danh sách Dungeon"); }

    @Override
    protected void build() {
        border();
        bySlot.clear();
        var active = plugin.dungeons().active();
        var next = plugin.scheduler().nextDungeon();
        int i = 0;
        for (DungeonDefinition d : plugin.dungeons().all()) {
            if (i >= SLOTS.length) break;
            List<String> lore = new ArrayList<>();
            lore.add("&7Cấp đề xuất: &f" + d.recommendedLevel());
            lore.add("&7Boss: &c" + d.bossId().replace('_', ' '));
            lore.add("&7Tối đa: &f" + d.maxPlayers() + " người");
            lore.add(" ");
            if (!d.enabled()) lore.add("&c[Đang tắt]");
            else if (active != null && active.def().id().equals(d.id())) lore.add("&a▶ ĐANG MỞ");
            else if (next != null && next.id().equals(d.id()))
                lore.add("&e⏳ Sắp mở sau " + Text.duration(Math.max(0, plugin.scheduler().secondsUntilNext())));
            lore.add("&8Bấm để xem phần thưởng");
            var item = Items.make(Items.material(d.icon(), Material.PAPER), d.displayName(), lore);
            if (active != null && active.def().id().equals(d.id())) Items.glow(item);
            inv.setItem(SLOTS[i], item);
            bySlot.put(SLOTS[i], d);
            i++;
        }
        inv.setItem(31, Items.make(Material.ARROW, "&7Quay lại"));
    }

    @Override
    public void click(Player p, int slot, ClickType type) {
        if (slot == 31) { new MainMenu(plugin, p).open(p); return; }
        DungeonDefinition d = bySlot.get(slot);
        if (d != null) new RewardMenu(plugin, d, this).open(p);
    }
}
