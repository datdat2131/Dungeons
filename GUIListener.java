package com.astrax.dungeon.listener;

import com.astrax.dungeon.gui.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Chong lay vat pham khoi menu: huy click, shift-click, drag, phim so, double-click...
 * Menu admin (allowBottomClicks) cho phep nhat vat pham trong tui do cua minh len con tro, nhung van chan shift-click
 * va double-click (COLLECT_TO_CURSOR) de khong the keo vat pham hien thi ra khoi menu.
 */
public final class GUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof Menu menu)) return;
        Inventory clicked = e.getClickedInventory();

        if (menu.allowBottomClicks() && clicked != null && !clicked.equals(top)) {
            InventoryAction a = e.getAction();
            if (e.isShiftClick() || a == InventoryAction.COLLECT_TO_CURSOR || a == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
            }
            return;
        }

        e.setCancelled(true);
        if (clicked == null || !clicked.equals(top)) return;
        if (e.getWhoClicked() instanceof Player p) {
            ItemStack cursor = e.getCursor();
            boolean has = cursor != null && !cursor.getType().isAir();
            menu.clickWithCursor(p, e.getRawSlot(), e.getClick(), has ? cursor.clone() : null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Menu) e.setCancelled(true);
    }
}
