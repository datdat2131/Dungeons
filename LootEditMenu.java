package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.reward.LootTable;
import com.astrax.dungeon.reward.Rarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/** Bang phan thuong cua 1 Boss: tien co ban, danh sach vat pham, them vat pham tu lam bang cach cam len con tro. */
public final class LootEditMenu extends AdminBase {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private final String bossId;
    private final Menu back;
    private final LootTable table;
    private final Map<Integer, String> bySlot = new HashMap<>();
    private int page;

    public LootEditMenu(AstraXDungeon plugin, String bossId, Menu back) {
        super(plugin, 6, "&8Admin - Phần thưởng: " + bossId);
        this.bossId = bossId;
        this.back = back;
        LootTable t = plugin.rewards().get(bossId);
        if (t == null) {
            t = LootTable.empty();
            plugin.rewards().save(bossId, t);
        }
        this.table = t;
    }

    @Override
    protected void build() {
        border();
        bySlot.clear();
        List<LootTable.Drop> all = table.drops();
        int maxPage = Math.max(0, (all.size() - 1) / SLOTS.length);
        page = Math.max(0, Math.min(page, maxPage));

        for (int i = 0; i < SLOTS.length; i++) {
            int idx = page * SLOTS.length + i;
            if (idx >= all.size()) break;
            LootTable.Drop d = all.get(idx);
            List<String> extra = new ArrayList<>();
            extra.add(" ");
            extra.add(d.guaranteed() ? "&aLuôn nhận" : "&7Tỷ lệ: &f" + d.chance() + "%");
            extra.add("&7Số lượng: &f" + d.amount() + (d.maxAmount() > d.amount() ? "-" + d.maxAmount() : ""));
            if (d.onlyTop() > 0) extra.add("&dChỉ top " + d.onlyTop() + " sát thương");
            if (d.pityStep() > 0) extra.add("&8Pity +" + d.pityStep() + "%/lần (tối đa +" + d.pityMax() + "%)");
            extra.add(" ");
            extra.add("&e▶ Bấm để chỉnh sửa");
            inv.setItem(SLOTS[i], display(d, d.amount(), extra));
            bySlot.put(SLOTS[i], d.id());
        }

        inv.setItem(4, Items.make(Material.GOLD_INGOT, "&6Tiền cơ bản: &a" + (long) table.baseMoney(),
                "&7Trái +100, phải -100", "&7Shift: ±1000", "&8Nhân x0.5 - x2 theo đóng góp"));
        inv.setItem(49, Items.make(Material.LIME_DYE, "&a➕ Thêm vật phẩm",
                "&71. Cầm vật phẩm bạn tự làm lên con trỏ", "&72. Bấm vào đây", "&8Mặc định: tỷ lệ 5%, độ hiếm RARE"));
        inv.setItem(45, Items.make(Material.ARROW, "&7Quay lại"));
        inv.setItem(53, Items.make(Material.BOOK, "&7Trang " + (page + 1) + "/" + (maxPage + 1), "&7" + all.size() + " mục"));
        if (page > 0) inv.setItem(48, Items.make(Material.SPECTRAL_ARROW, "&7◀ Trang trước"));
        if (page < maxPage) inv.setItem(50, Items.make(Material.SPECTRAL_ARROW, "&7Trang sau ▶"));
    }

    @Override
    protected void handle(Player p, int slot, ClickType type, ItemStack cursor) {
        switch (slot) {
            case 4 -> {
                table.setBaseMoney(table.baseMoney() + delta(type, 100, 1000));
                plugin.rewards().save(bossId, table);
                refresh();
                return;
            }
            case 45 -> {
                if (back != null) back.open(p); else new LootBossMenu(plugin).open(p);
                return;
            }
            case 48 -> { page--; refresh(); return; }
            case 50 -> { page++; refresh(); return; }
            case 49 -> {
                if (cursor == null) { tell(p, "&cHãy cầm vật phẩm lên con trỏ trước rồi bấm vào nút này."); return; }
                Set<String> used = new HashSet<>();
                for (LootTable.Drop d : table.drops()) used.add(d.id());
                LootTable.Drop nd = plugin.rewards().dropFromItem(cursor, 5.0, Rarity.RARE, used);
                table.drops().add(nd);
                plugin.rewards().save(bossId, table);
                tell(p, "&aĐã thêm vào bảng thưởng. Giờ hãy chỉnh tỷ lệ.");
                new DropEditMenu(plugin, bossId, table, nd.id(), this).open(p);
                return;
            }
            default -> { }
        }
        String id = bySlot.get(slot);
        if (id != null) new DropEditMenu(plugin, bossId, table, id, this).open(p);
    }
}
