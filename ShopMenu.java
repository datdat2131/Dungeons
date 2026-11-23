package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.shop.ShopManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ShopMenu extends Menu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final String[] TOKENS = {"common_token", "epic_token", "legendary_token", "mythical_token"};
    private final Player viewer;
    private final Map<Integer, ShopManager.Entry> bySlot = new HashMap<>();

    public ShopMenu(AstraXDungeon plugin, Player viewer) {
        super(plugin, 6, "&8Cửa hàng Token");
        this.viewer = viewer;
    }

    @Override
    protected void build() {
        border();
        bySlot.clear();
        ShopManager shop = plugin.shop();

        List<String> mine = new ArrayList<>();
        for (String t : TOKENS) mine.add("&7" + ShopManager.pretty(t) + ": &f" + shop.count(viewer, t));
        inv.setItem(4, Items.make(Material.NETHER_STAR, "&eToken của bạn", mine));

        int i = 0;
        for (ShopManager.Entry e : shop.entries()) {
            if (i >= SLOTS.length) break;
            List<String> extra = new ArrayList<>();
            extra.add(" ");
            extra.add("&7Giá:");
            for (Map.Entry<String, Integer> c : e.cost().entrySet()) {
                int have = shop.count(viewer, c.getKey());
                extra.add((have >= c.getValue() ? "&a" : "&c") + " " + c.getValue() + "x " + ShopManager.pretty(c.getKey())
                        + " &8(bạn có " + have + ")");
            }
            if (e.dailyLimit() > 0) extra.add("&7Giới hạn: &f" + e.dailyLimit() + " lần/ngày");
            extra.add(" ");
            extra.add("&e▶ Bấm để mua");

            ItemStack it;
            if ("ITEM".equals(e.drop().type())) {
                it = plugin.rewards().buildItem(e.drop(), e.drop().amount());
                ItemMeta mm = it.getItemMeta();
                List<Component> lore = mm.lore() == null ? new ArrayList<>() : new ArrayList<>(mm.lore());
                for (String l : extra) lore.add(Items.line(l));
                mm.lore(lore);
                it.setItemMeta(mm);
            } else {
                it = Items.make(Material.NAME_TAG, e.drop().rarity().color() + e.drop().name(), extra);
            }
            inv.setItem(SLOTS[i], it);
            bySlot.put(SLOTS[i], e);
            i++;
        }
        inv.setItem(49, Items.make(Material.ARROW, "&7Quay lại"));
    }

    @Override
    public void click(Player p, int slot, ClickType type) {
        if (slot == 49) { new MainMenu(plugin, p).open(p); return; }
        ShopManager.Entry e = bySlot.get(slot);
        if (e == null) return;
        plugin.shop().buy(p, e);
        Bukkit.getScheduler().runTaskLater(plugin, this::refresh, 6L);
    }
}
