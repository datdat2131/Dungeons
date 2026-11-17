package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Moi menu la 1 InventoryHolder rieng => GUIListener nhan dien va huy MOI thao tac lay do. */
public abstract class Menu implements InventoryHolder {
    protected final AstraXDungeon plugin;
    protected final Inventory inv;

    protected Menu(AstraXDungeon plugin, int rows, String title) {
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(this, rows * 9, Text.c(title));
    }

    @Override public Inventory getInventory() { return inv; }

    protected abstract void build();
    public abstract void click(Player p, int slot, ClickType type);

    public void open(Player p) {
        inv.clear();
        build();
        p.openInventory(inv);
    }

    protected void refresh() {
        inv.clear();
        build();
    }

    protected void border() {
        ItemStack g = Items.make(Material.GRAY_STAINED_GLASS_PANE, " ");
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, g);
        }
    }
}
