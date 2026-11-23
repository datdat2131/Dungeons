package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonDefinition;
import com.astrax.dungeon.reward.LootTable;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class RewardMenu extends Menu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private final DungeonDefinition def;
    private final Menu back;

    public RewardMenu(AstraXDungeon plugin, DungeonDefinition def, Menu back) {
        super(plugin, 5, "&8Phần thưởng: " + def.displayName());
        this.def = def;
        this.back = back;
    }

    @Override
    protected void build() {
        border();
        LootTable table = plugin.rewards().get(def.bossId());
        if (table == null) {
            inv.setItem(22, Items.make(Material.BARRIER, "&cChưa có bảng thưởng", "&7Boss: " + def.bossId()));
        } else {
            inv.setItem(4, Items.make(Material.GOLD_INGOT, "&6Tiền cơ bản: &a" + (long) table.baseMoney(),
                    "&7Nhân x0.5 - x2 theo mức đóng góp"));
            int i = 0;
            for (LootTable.Drop d : table.drops()) {
                if (i >= SLOTS.length) break;
                inv.setItem(SLOTS[i++], display(d));
            }
        }
        inv.setItem(inv.getSize() - 5, Items.make(Material.ARROW, "&7Quay lại"));
    }

    private ItemStack display(LootTable.Drop d) {
        List<String> extra = new ArrayList<>();
        extra.add(" ");
        if (d.guaranteed()) extra.add("&aLuôn nhận"); else extra.add("&7Tỷ lệ: &f" + d.chance() + "%");
        if (d.onlyTop() > 0) extra.add("&dChỉ dành cho top " + d.onlyTop() + " sát thương");
        if (d.pityStep() > 0) extra.add("&8Pity: +" + d.pityStep() + "%/lần xui (tối đa +" + d.pityMax() + "%)");
        if ("ITEM".equals(d.type())) {
            ItemStack it = plugin.rewards().buildItem(d);
            ItemMeta mm = it.getItemMeta();
            List<Component> lore = mm.lore() == null ? new ArrayList<>() : new ArrayList<>(mm.lore());
            for (String l : extra) lore.add(Items.line(l));
            mm.lore(lore);
            it.setItemMeta(mm);
            return it;
        }
        extra.add(0, "&7Phần thưởng đặc biệt (" + d.rarity().color() + d.rarity().name() + "&7)");
        return Items.make(Material.NAME_TAG, d.rarity().color() + d.name(), extra);
    }

    @Override
    public void click(Player p, int slot, ClickType type) {
        if (slot == inv.getSize() - 5) {
            if (back != null) back.open(p); else new MainMenu(plugin, p).open(p);
        }
    }
}
