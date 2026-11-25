package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.reward.LootTable;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Nen cho cac menu chinh sua cua admin: kiem tra quyen moi lan bam, cho phep cam vat pham len con tro. */
public abstract class AdminBase extends Menu {
    protected AdminBase(AstraXDungeon plugin, int rows, String title) { super(plugin, rows, title); }

    @Override public boolean allowBottomClicks() { return true; }

    @Override
    public void open(Player p) {
        if (!p.hasPermission("astrax.dungeon.admin")) return;
        super.open(p);
    }

    @Override
    public void click(Player p, int slot, ClickType type) { clickWithCursor(p, slot, type, null); }

    @Override
    public void clickWithCursor(Player p, int slot, ClickType type, ItemStack cursor) {
        if (!p.hasPermission("astrax.dungeon.admin")) { p.closeInventory(); return; }
        handle(p, slot, type, cursor);
    }

    protected abstract void handle(Player p, int slot, ClickType type, ItemStack cursor);

    /** Chuot trai +small, phai -small, Shift+trai +big, Shift+phai -big. */
    protected static double delta(ClickType t, double small, double big) {
        return switch (t) {
            case LEFT -> small;
            case RIGHT -> -small;
            case SHIFT_LEFT -> big;
            case SHIFT_RIGHT -> -big;
            default -> 0;
        };
    }

    protected static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    /** Hien thi 1 muc: vat pham that (neu type ITEM) kem them cac dong lore. */
    protected ItemStack display(LootTable.Drop d, int amount, List<String> extra) {
        if ("ITEM".equals(d.type())) {
            ItemStack it = plugin.rewards().buildItem(d, amount);
            ItemMeta mm = it.getItemMeta();
            List<Component> lore = mm.lore() == null ? new ArrayList<>() : new ArrayList<>(mm.lore());
            for (String l : extra) lore.add(Items.line(l));
            mm.lore(lore);
            it.setItemMeta(mm);
            return it;
        }
        return Items.make(Material.NAME_TAG, d.rarity().color() + d.name(), extra);
    }

    protected void tell(Player p, String msg) { p.sendMessage(com.astrax.dungeon.util.Text.c("&8[&6AstraX&8] &r" + msg)); }
}
