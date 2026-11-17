package com.astrax.dungeon.listener;

import com.astrax.dungeon.gui.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Chong lay vat pham khoi menu: huy click, shift-click, drag, phim so, double-click... */
public final class GUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof Menu menu)) return;
        e.setCancelled(true);
        Inventory clicked = e.getClickedInventory();
        if (clicked == null || !clicked.equals(top)) return;
        if (e.getWhoClicked() instanceof Player p) menu.click(p, e.getRawSlot(), e.getClick());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Menu) e.setCancelled(true);
    }
}
