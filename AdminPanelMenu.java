package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/** /dungeon admin edit: cong vao cac GUI chinh sua. */
public final class AdminPanelMenu extends AdminBase {
    public AdminPanelMenu(AstraXDungeon plugin) { super(plugin, 3, "&8Admin - Chỉnh sửa"); }

    @Override
    protected void build() {
        border();
        inv.setItem(11, Items.make(Material.EMERALD, "&2Cửa hàng Token",
                "&7Thêm / xóa vật phẩm, chỉnh giá, giới hạn mua", "&7Đưa vật phẩm bạn tự làm vào bán", "&e▶ Bấm để mở"));
        inv.setItem(13, Items.make(Material.CHEST, "&aPhần thưởng Boss",
                "&7Thêm / xóa vật phẩm rơi, chỉnh tỷ lệ, độ hiếm", "&7Đưa vật phẩm bạn tự làm vào bảng rơi", "&e▶ Bấm để mở"));
        inv.setItem(15, Items.make(Material.REPEATER, "&bTải lại cấu hình", "&7Đọc lại toàn bộ file YAML"));
    }

    @Override
    protected void handle(Player p, int slot, ClickType type, ItemStack cursor) {
        switch (slot) {
            case 11 -> new ShopEditMenu(plugin).open(p);
            case 13 -> new LootBossMenu(plugin).open(p);
            case 15 -> { plugin.reloadAll(); tell(p, "&aĐã tải lại cấu hình."); }
            default -> { }
        }
    }
}
