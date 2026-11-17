package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.ranking.RankingManager.Category;
import com.astrax.dungeon.ranking.RankingManager.Entry;
import com.astrax.dungeon.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public final class RankingMenu extends Menu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22};
    private static final int[] TAB_SLOTS = {38, 39, 40, 41, 42};
    private static final Material[] TAB_ICONS = {Material.NETHER_STAR, Material.EMERALD, Material.DIAMOND_SWORD,
            Material.EXPERIENCE_BOTTLE, Material.CLOCK};
    private Category category;

    public RankingMenu(AstraXDungeon plugin, Category category) {
        super(plugin, 5, "&8Bảng xếp hạng");
        this.category = category;
    }

    @Override
    protected void build() {
        border();
        List<Entry> top = plugin.ranking().top(category, SLOTS.length);
        inv.setItem(4, Items.make(Material.GOLD_BLOCK, "&6Top: &f" + category.label,
                top.isEmpty() ? List.of("&7Chưa có dữ liệu (cập nhật sau mỗi Boss)") : List.of("&7Cập nhật sau mỗi Boss")));
        for (int i = 0; i < top.size(); i++) {
            Entry e = top.get(i);
            String value = category == Category.BEST_TIME ? Text.duration((long) e.value()) : String.valueOf((long) e.value());
            inv.setItem(SLOTS[i], Items.head(Bukkit.getOfflinePlayer(e.uuid()), "&6#" + (i + 1) + " &f" + e.name(),
                    List.of("&7" + category.label + ": &e" + value)));
        }
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            var item = Items.make(TAB_ICONS[i], (cats[i] == category ? "&a▶ " : "&7") + cats[i].label);
            if (cats[i] == category) Items.glow(item);
            inv.setItem(TAB_SLOTS[i], item);
        }
        inv.setItem(44, Items.make(Material.ARROW, "&7Quay lại"));
    }

    @Override
    public void click(Player p, int slot, ClickType type) {
        if (slot == 44) { new MainMenu(plugin, p).open(p); return; }
        Category[] cats = Category.values();
        for (int i = 0; i < TAB_SLOTS.length; i++) {
            if (slot == TAB_SLOTS[i]) { category = cats[i]; refresh(); return; }
        }
    }
}
