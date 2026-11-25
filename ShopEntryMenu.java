package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.reward.LootTable;
import com.astrax.dungeon.reward.Rarity;
import com.astrax.dungeon.shop.ShopManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Chinh 1 muc cua hang: gia theo tung loai Token, gioi han/ngay, so luong, do hiem, doi vat pham, xoa. */
public final class ShopEntryMenu extends AdminBase {
    private static final String[] TOKENS = {"common_token", "epic_token", "legendary_token", "mythical_token"};
    private static final Material[] TOKEN_ICONS = {Material.GOLD_NUGGET, Material.EMERALD, Material.DIAMOND, Material.NETHERITE_SCRAP};
    private static final int[] TOKEN_SLOTS = {10, 12, 14, 16};

    private final ShopManager.Entry entry;
    private final Menu back;

    public ShopEntryMenu(AstraXDungeon plugin, ShopManager.Entry entry, Menu back) {
        super(plugin, 5, "&8Admin - Chỉnh mục cửa hàng");
        this.entry = entry;
        this.back = back;
    }

    @Override
    protected void build() {
        border();
        LootTable.Drop d = entry.drop();
        inv.setItem(4, display(d, d.amount(), List.of(" ", "&7Đây là vật phẩm sẽ bán")));

        for (int i = 0; i < TOKENS.length; i++) {
            int cur = entry.cost().getOrDefault(TOKENS[i], 0);
            inv.setItem(TOKEN_SLOTS[i], Items.make(TOKEN_ICONS[i], "&e" + ShopManager.pretty(TOKENS[i]) + ": &f" + cur,
                    "&7Chuột trái &a+1&7, phải &c-1", "&7Shift: &a+10&7 / &c-10", "&80 = không dùng loại token này"));
        }
        inv.setItem(28, Items.make(Material.CLOCK, "&bGiới hạn/ngày: &f" + (entry.dailyLimit() > 0 ? entry.dailyLimit() : "không"),
                "&7Trái +1, phải -1, Shift ±5", "&80 = không giới hạn"));
        inv.setItem(30, Items.make(Material.PAPER, "&bSố lượng mỗi lần mua: &f" + d.amount(),
                "&7Trái +1, phải -1, Shift ±8"));
        inv.setItem(32, Items.make(Material.NETHER_STAR, "&bĐộ hiếm: " + d.rarity().color() + d.rarity().name(),
                "&7Bấm để đổi (phải = lùi lại)"));
        inv.setItem(34, Items.make(Material.HOPPER, "&dĐổi vật phẩm",
                "&7Cầm vật phẩm mới lên con trỏ rồi bấm vào đây", "&7(giữ nguyên giá, giới hạn)"));
        inv.setItem(36, Items.make(Material.ARROW, "&7Quay lại"));
        inv.setItem(40, Items.make(Material.BARRIER, "&cXóa mục này", "&7Giữ &eShift &7+ bấm để xác nhận"));
    }

    private void edit(java.util.function.Consumer<Map<String, Object>> change) {
        Map<String, Object> m = LootTable.toMap(entry.drop());
        change.accept(m);
        entry.drop(LootTable.parseDrop(m));
        plugin.shop().save();
        refresh();
    }

    @Override
    protected void handle(Player p, int slot, ClickType type, ItemStack cursor) {
        for (int i = 0; i < TOKEN_SLOTS.length; i++) {
            if (slot != TOKEN_SLOTS[i]) continue;
            int nv = Math.max(0, entry.cost().getOrDefault(TOKENS[i], 0) + (int) delta(type, 1, 10));
            if (nv == 0) entry.cost().remove(TOKENS[i]); else entry.cost().put(TOKENS[i], nv);
            plugin.shop().save();
            refresh();
            return;
        }
        switch (slot) {
            case 28 -> {
                entry.dailyLimit(entry.dailyLimit() + (int) delta(type, 1, 5));
                plugin.shop().save();
                refresh();
            }
            case 30 -> edit(m -> m.put("amount", Math.max(1, entry.drop().amount() + (int) delta(type, 1, 8))));
            case 32 -> {
                Rarity[] all = Rarity.values();
                int step = type.isRightClick() ? -1 : 1;
                Rarity next = all[(entry.drop().rarity().ordinal() + step + all.length) % all.length];
                edit(m -> m.put("rarity", next.name()));
            }
            case 34 -> {
                if (cursor == null) { tell(p, "&cHãy cầm vật phẩm mới lên con trỏ trước."); return; }
                edit(m -> plugin.rewards().applyItem(m, cursor));
                tell(p, "&aĐã đổi vật phẩm.");
            }
            case 36 -> back.open(p);
            case 40 -> {
                if (!type.isShiftClick()) { tell(p, "&7Giữ Shift + bấm để xóa mục này."); return; }
                plugin.shop().remove(entry);
                tell(p, "&aĐã xóa khỏi cửa hàng.");
                back.open(p);
            }
            default -> { }
        }
    }
}
