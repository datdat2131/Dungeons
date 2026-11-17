package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PartyInviteMenu extends Menu {
    private final Map<Integer, String> bySlot = new HashMap<>();

    public PartyInviteMenu(AstraXDungeon plugin) { super(plugin, 6, "&8Mời vào Party"); }

    @Override
    protected void build() {
        bySlot.clear();
        int i = 0;
        for (Player t : Bukkit.getOnlinePlayers()) {
            if (i >= 45) break;
            if (plugin.parties().partyOf(t.getUniqueId()) != null) continue;
            inv.setItem(i, Items.head(t, "&f" + t.getName(), List.of("&e▶ Bấm để mời")));
            bySlot.put(i, t.getName());
            i++;
        }
        inv.setItem(49, Items.make(org.bukkit.Material.ARROW, "&7Quay lại"));
    }

    @Override
    public void click(Player p, int slot, ClickType type) {
        if (slot == 49) { new PartyMenu(plugin, p).open(p); return; }
        String name = bySlot.get(slot);
        if (name == null || name.equalsIgnoreCase(p.getName())) return;
        plugin.parties().invite(p, name);
        p.closeInventory();
    }
}
