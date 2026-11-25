package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.reward.LootTable;
import com.astrax.dungeon.reward.Rarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Chinh 1 muc loot: ty le, so luong, do hiem, luon nhan, chi top, pity, doi vat pham, xoa. */
public final class DropEditMenu extends AdminBase {
    private final String bossId;
    private final LootTable table;
    private final String dropId;
    private final Menu back;

    public DropEditMenu(AstraXDungeon plugin, String bossId, LootTable table, String dropId, Menu back) {
        super(plugin, 5, "&8Admin - Chỉnh vật phẩm rơi");
        this.bossId = bossId;
        this.table = table;
        this.dropId = dropId;
        this.back = back;
    }

    private int index() {
        for (int i = 0; i < table.drops().size(); i++) if (table.drops().get(i).id().equals(dropId)) return i;
        return -1;
    }

    private void edit(Consumer<Map<String, Object>> change) {
        int i = index();
        if (i < 0) return;
        Map<String, Object> m = LootTable.toMap(table.drops().get(i));
        change.accept(m);
        table.drops().set(i, LootTable.parseDrop(m));
        plugin.rewards().save(bossId, table);
        refresh();
    }

    @Override
    protected void build() {
        border();
        int i = index();
        if (i < 0) { inv.setItem(22, Items.make(Material.BARRIER, "&cMục này đã bị xóa")); inv.setItem(40, Items.make(Material.ARROW, "&7Quay lại")); return; }
        LootTable.Drop d = table.drops().get(i);

        inv.setItem(4, display(d, d.amount(), List.of(" ", "&7ID: &f" + d.id())));
        inv.setItem(10, Items.make(Material.PAPER, "&bTỷ lệ rơi: &f" + d.chance() + "%",
                "&7Trái +1, phải -1", "&7Shift: ±0.1"));
        inv.setItem(11, Items.make(Material.CHEST, "&bSố lượng tối thiểu: &f" + d.amount(), "&7Trái +1, phải -1, Shift ±8"));
        inv.setItem(12, Items.make(Material.ENDER_CHEST, "&bSố lượng tối đa: &f" + d.maxAmount(),
                "&7Trái +1, phải -1, Shift ±8", "&8Số lượng ngẫu nhiên giữa tối thiểu và tối đa"));
        inv.setItem(13, Items.make(Material.NETHER_STAR, "&bĐộ hiếm: " + d.rarity().color() + d.rarity().name(),
                "&7Bấm để đổi (phải = lùi lại)"));
        inv.setItem(14, Items.make(d.guaranteed() ? Material.LIME_DYE : Material.GRAY_DYE,
                "&bLuôn nhận: " + (d.guaranteed() ? "&aBẬT" : "&cTẮT"), "&7Bật = mọi người đủ điều kiện đều nhận, không roll"));
        inv.setItem(15, Items.make(Material.DIAMOND_SWORD, "&bChỉ top sát thương: &f" + (d.onlyTop() > 0 ? d.onlyTop() : "không"),
                "&7Trái +1, phải -1", "&80 = ai cũng được roll"));
        inv.setItem(16, Items.make(Material.CLOCK, "&bPity mỗi lần xui: &f+" + d.pityStep() + "%",
                "&7Trái/phải ±0.05, Shift ±0.5"));
        inv.setItem(19, Items.make(Material.COMPARATOR, "&bPity tối đa: &f+" + d.pityMax() + "%", "&7Trái/phải ±1, Shift ±5"));
        inv.setItem(20, Items.make(Material.GLOWSTONE_DUST, "&bPhát sáng: " + (d.glow() ? "&aBẬT" : "&cTẮT"),
                "&8Chỉ áp dụng cho vật phẩm tạo từ config (không phải vật phẩm tự làm)"));
        inv.setItem(21, Items.make(Material.ANVIL, "&bBất tử (unbreakable): " + (d.unbreakable() ? "&aBẬT" : "&cTẮT"),
                "&8Chỉ áp dụng cho vật phẩm tạo từ config"));
        inv.setItem(23, Items.make(Material.HOPPER, "&dĐổi vật phẩm",
                "&7Cầm vật phẩm mới lên con trỏ rồi bấm vào đây", "&7(giữ nguyên tỷ lệ, độ hiếm)"));
        inv.setItem(25, Items.make(Material.BARRIER, "&cXóa mục này", "&7Giữ &eShift &7+ bấm để xác nhận"));
        inv.setItem(40, Items.make(Material.ARROW, "&7Quay lại"));
    }

    @Override
    protected void handle(Player p, int slot, ClickType type, ItemStack cursor) {
        int i = index();
        if (slot == 40 || i < 0) { back.open(p); return; }
        LootTable.Drop d = table.drops().get(i);
        switch (slot) {
            case 10 -> edit(m -> m.put("chance", Math.max(0, Math.min(100, round2(d.chance() + delta(type, 1, 0.1))))));
            case 11 -> edit(m -> m.put("amount", Math.max(1, d.amount() + (int) delta(type, 1, 8))));
            case 12 -> edit(m -> m.put("max-amount", Math.max(d.amount(), d.maxAmount() + (int) delta(type, 1, 8))));
            case 13 -> {
                Rarity[] all = Rarity.values();
                int step = type.isRightClick() ? -1 : 1;
                Rarity next = all[(d.rarity().ordinal() + step + all.length) % all.length];
                edit(m -> m.put("rarity", next.name()));
            }
            case 14 -> edit(m -> m.put("guaranteed", !d.guaranteed()));
            case 15 -> edit(m -> m.put("only-top", Math.max(0, Math.min(20, d.onlyTop() + (int) delta(type, 1, 1)))));
            case 16 -> edit(m -> {
                double nv = Math.max(0, round2(d.pityStep() + delta(type, 0.05, 0.5)));
                m.put("pity-step", nv);
                m.put("pity-max", d.pityMax() > 0 ? d.pityMax() : 5.0);
            });
            case 19 -> edit(m -> {
                m.put("pity-max", Math.max(0, round2(d.pityMax() + delta(type, 1, 5))));
                if (d.pityStep() <= 0) m.put("pity-step", 0.1);
            });
            case 20 -> edit(m -> m.put("glow", !d.glow()));
            case 21 -> edit(m -> m.put("unbreakable", !d.unbreakable()));
            case 23 -> {
                if (cursor == null) { tell(p, "&cHãy cầm vật phẩm mới lên con trỏ trước."); return; }
                edit(m -> plugin.rewards().applyItem(m, cursor));
                tell(p, "&aĐã đổi vật phẩm.");
            }
            case 25 -> {
                if (!type.isShiftClick()) { tell(p, "&7Giữ Shift + bấm để xóa mục này."); return; }
                table.drops().remove(i);
                plugin.rewards().save(bossId, table);
                tell(p, "&aĐã xóa khỏi bảng thưởng.");
                back.open(p);
            }
            default -> { }
        }
    }
}
