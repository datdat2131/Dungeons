package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonDefinition;
import com.astrax.dungeon.reward.LootTable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Chon Boss/Dungeon de chinh bang phan thuong. */
public final class LootBossMenu extends AdminBase {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private final Map<Integer, String> bySlot = new HashMap<>();

    public LootBossMenu(AstraXDungeon plugin) { super(plugin, 4, "&8Admin - Chọn bảng phần thưởng"); }

    @Override
    protected void build() {
        border();
        bySlot.clear();
        int i = 0;
        for (DungeonDefinition d : plugin.dungeons().all()) {
            if (i >= SLOTS.length) break;
            LootTable t = plugin.rewards().get(d.bossId());
            List<String> lore = new ArrayList<>();
            lore.add("&7Cấp độ: " + plugin.tiers().get(d.tier()).displayName());
            lore.add("&7Boss: &c" + d.bossId());
            lore.add("&7Số mục rơi: &f" + (t == null ? 0 : t.drops().size()));
            lore.add(" ");
            lore.add("&e▶ Bấm để chỉnh sửa");
            inv.setItem(SLOTS[i], Items.make(Items.material(d.icon(), Material.PAPER), d.displayName(), lore));
            bySlot.put(SLOTS[i], d.bossId());
            i++;
        }
        inv.setItem(31, Items.make(Material.ARROW, "&7Quay lại"));
    }

    @Override
    protected void handle(Player p, int slot, ClickType type, ItemStack cursor) {
        if (slot == 31) { new AdminPanelMenu(plugin).open(p); return; }
        String boss = bySlot.get(slot);
        if (boss != null) new LootEditMenu(plugin, boss, this).open(p);
    }
}
