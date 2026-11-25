package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.shop.ShopManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Danh sach muc cua hang: bam de chinh, cam vat pham len con tro roi bam nut "Them" de them vat pham tu lam. */
public final class ShopEditMenu extends AdminBase {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private final Map<Integer, ShopManager.Entry> bySlot = new HashMap<>();
    private int page;

    public ShopEditMenu(AstraXDungeon plugin) { super(plugin, 6, "&8Admin - Cửa hàng Token"); }

    @Override
    protected void build() {
        border();
        bySlot.clear();
        List<ShopManager.Entry> all = plugin.shop().entries();
        int maxPage = Math.max(0, (all.size() - 1) / SLOTS.length);
        page = Math.max(0, Math.min(page, maxPage));

        for (int i = 0; i < SLOTS.length; i++) {
            int idx = page * SLOTS.length + i;
            if (idx >= all.size()) break;
            ShopManager.Entry e = all.get(idx);
            List<String> extra = new ArrayList<>();
            extra.add(" ");
            extra.add("&7Giá:");
            for (Map.Entry<String, Integer> c : e.cost().entrySet()) {
                extra.add("&e - " + c.getValue() + "x " + ShopManager.pretty(c.getKey()));
            }
            if (e.cost().isEmpty()) extra.add("&a - Miễn phí");
            extra.add("&7Giới hạn/ngày: &f" + (e.dailyLimit() > 0 ? e.dailyLimit() : "không"));
            extra.add(" ");
            extra.add("&e▶ Bấm để chỉnh sửa");
            inv.setItem(SLOTS[i], display(e.drop(), e.drop().amount(), extra));
            bySlot.put(SLOTS[i], e);
        }

        inv.setItem(4, Items.make(Material.BOOK, "&6Cửa hàng: &f" + all.size() + " mục",
                "&7Trang " + (page + 1) + "/" + (maxPage + 1)));
        inv.setItem(49, Items.make(Material.LIME_DYE, "&a➕ Thêm vật phẩm",
                "&71. Cầm vật phẩm bạn tự làm lên con trỏ", "&7   (nhặt từ túi đồ bên dưới)",
                "&72. Bấm vào đây", "&8Giữ nguyên tên, lore, enchant, NBT"));
        inv.setItem(45, Items.make(Material.ARROW, "&7Quay lại"));
        if (page > 0) inv.setItem(48, Items.make(Material.SPECTRAL_ARROW, "&7◀ Trang trước"));
        if (page < maxPage) inv.setItem(50, Items.make(Material.SPECTRAL_ARROW, "&7Trang sau ▶"));
    }

    @Override
    protected void handle(Player p, int slot, ClickType type, ItemStack cursor) {
        switch (slot) {
            case 45 -> { new AdminPanelMenu(plugin).open(p); return; }
            case 48 -> { page--; refresh(); return; }
            case 50 -> { page++; refresh(); return; }
            case 49 -> {
                if (cursor == null) { tell(p, "&cHãy cầm vật phẩm lên con trỏ trước rồi bấm vào nút này."); return; }
                ShopManager.Entry e = plugin.shop().addEntry(cursor);
                tell(p, "&aĐã thêm vào cửa hàng. Giờ hãy chỉnh giá.");
                new ShopEntryMenu(plugin, e, this).open(p);
                return;
            }
            default -> { }
        }
        ShopManager.Entry e = bySlot.get(slot);
        if (e != null) new ShopEntryMenu(plugin, e, this).open(p);
    }
}
