package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PartyMenu extends Menu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
    private final Player viewer;
    private final Map<Integer, UUID> bySlot = new HashMap<>();

    public PartyMenu(AstraXDungeon plugin, Player viewer) {
        super(plugin, 4, "&8Party");
        this.viewer = viewer;
    }

    @Override
    protected void build() {
        border();
        bySlot.clear();
        Party party = plugin.parties().partyOf(viewer.getUniqueId());
        inv.setItem(31, Items.make(Material.ARROW, "&7Quay lại"));
        if (party == null) {
            inv.setItem(13, Items.make(Material.EMERALD, "&aTạo Party", "&7Bấm để tạo tổ đội mới"));
            inv.setItem(29, Items.make(Material.WRITABLE_BOOK, "&eMời người chơi", "&7Tự tạo Party và mời"));
            return;
        }
        boolean leader = party.isLeader(viewer.getUniqueId());
        int i = 0;
        for (UUID u : party.members()) {
            if (i >= SLOTS.length) break;
            var op = Bukkit.getOfflinePlayer(u);
            List<String> lore = new ArrayList<>();
            if (party.isLeader(u)) lore.add("&6★ Đội trưởng");
            if (plugin.dungeons().isInDungeon(u)) lore.add("&a[Đang trong Dungeon]");
            if (leader && !u.equals(viewer.getUniqueId())) {
                lore.add(" ");
                lore.add("&cChuột trái: đuổi");
                lore.add("&eChuột phải: nhường đội trưởng");
            }
            inv.setItem(SLOTS[i], Items.head(op, "&f" + (op.getName() == null ? "?" : op.getName()), lore));
            bySlot.put(SLOTS[i], u);
            i++;
        }
        if (leader) inv.setItem(29, Items.make(Material.WRITABLE_BOOK, "&eMời người chơi",
                "&7Còn " + (plugin.getConfig().getInt("party.max-size", 10) - party.size()) + " chỗ"));
        inv.setItem(33, Items.make(Material.BARRIER, "&cRời Party"));
    }

    @Override
    public void click(Player p, int slot, ClickType type) {
        var pm = plugin.parties();
        Party party = pm.partyOf(p.getUniqueId());
        switch (slot) {
            case 31 -> { new MainMenu(plugin, p).open(p); return; }
            case 13 -> { if (party == null) pm.create(p); refresh(); return; }
            case 29 -> { new PartyInviteMenu(plugin).open(p); return; }
            case 33 -> { if (party != null) pm.leave(p); refresh(); return; }
            default -> { }
        }
        UUID target = bySlot.get(slot);
        if (target == null || party == null || !party.isLeader(p.getUniqueId()) || target.equals(p.getUniqueId())) return;
        String name = Bukkit.getOfflinePlayer(target).getName();
        if (name == null) return;
        if (type.isLeftClick()) pm.kick(p, name);
        else if (type.isRightClick()) pm.promote(p, name);
        refresh();
    }
}
