package com.astrax.dungeon.gui;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonDefinition;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.ranking.RankingManager;
import com.astrax.dungeon.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

public final class MainMenu extends Menu {
    private final Player viewer;

    public MainMenu(AstraXDungeon plugin, Player viewer) {
        super(plugin, 4, "&8AstraX Dungeon");
        this.viewer = viewer;
    }

    @Override
    protected void build() {
        border();
        inv.setItem(10, Items.make(Material.COMPASS, "&6Danh sách Dungeon", "&7Xem 7 Dungeon, Boss và phần thưởng"));
        inv.setItem(12, statusItem());
        inv.setItem(14, Items.make(Material.WHITE_BANNER, "&bParty", "&7Lập tổ đội, mời bạn bè", "&7Tối đa 10 người"));
        inv.setItem(16, Items.make(Material.GOLD_INGOT, "&eBảng xếp hạng", "&7Top người chơi mạnh nhất"));
        inv.setItem(19, Items.make(Material.CHEST, "&aPhần thưởng", "&7Bảng rơi đồ Dungeon hiện tại / kế tiếp"));
        inv.setItem(20, Items.make(Material.EMERALD, "&2Cửa hàng Token", "&7Đổi Token lấy vật phẩm"));
        inv.setItem(21, Items.make(Material.WRITABLE_BOOK, "&bNhiệm vụ", "&7Nhiệm vụ ngày/tuần và chuỗi ngày"));
        inv.setItem(22, Items.make(Material.ENDER_CHEST, "&dHộp thư thưởng", "&7Nhận thưởng khi bạn offline", "&e▶ Bấm để nhận"));
        inv.setItem(23, Items.make(Material.BOOK, "&fThống kê của bạn", "&7Bấm để xem trong chat"));
        if (plugin.dungeons().isInDungeon(viewer.getUniqueId())) {
            inv.setItem(25, Items.make(Material.BARRIER, "&cRời Dungeon", "&7Về sảnh chờ"));
        }
    }

    private org.bukkit.inventory.ItemStack statusItem() {
        var m = plugin.messages();
        DungeonSession s = plugin.dungeons().active();
        List<String> lore = new ArrayList<>();
        if (s != null) {
            lore.add("&7Cấp độ: " + s.tier().displayName());
            lore.add("&7Trạng thái: &f" + s.state());
            lore.add(s.joinOpen() ? "&7Cổng vào đóng sau: &f" + Text.duration(s.joinSecondsLeft()) : "&cCổng đã đóng, không thể vào nữa");
            lore.add("&7Số người: &f" + s.players().size());
            lore.add("&7Biến thể: &d" + m.modifierName(s.modifier().name()));
            if (s.contributionMultiplier() > 1) lore.add("&6✦ GIỜ VÀNG x" + s.contributionMultiplier());
            lore.add(" ");
            lore.add(plugin.dungeons().isInDungeon(viewer.getUniqueId()) ? "&7Bạn đang ở trong Dungeon"
                    : s.joinOpen() ? "&e▶ Bấm để tham gia" : "&c✖ Cổng đã đóng");
            return Items.glow(Items.make(Material.LIME_CONCRETE, "&aĐang mở: " + s.def().displayName(), lore));
        }
        DungeonDefinition d = plugin.scheduler().nextDungeon();
        lore.add("&7Dungeon: " + (d == null ? "&f-" : d.displayName()));
        if (d != null) lore.add("&7Cấp độ: " + plugin.tiers().get(d.tier()).displayName());
        lore.add("&7Mở sau: &f" + Text.duration(Math.max(0, plugin.scheduler().secondsUntilNext())));
        if (plugin.scheduler().nextIsGolden()) lore.add("&6✦ Lượt này rơi vào GIỜ VÀNG!");
        return Items.make(Material.CLOCK, "&eDungeon kế tiếp", lore);
    }

    @Override
    public void click(Player p, int slot, ClickType type) {
        switch (slot) {
            case 10 -> new DungeonMenu(plugin).open(p);
            case 12 -> {
                var s = plugin.dungeons().active();
                if (s != null && !plugin.dungeons().isInDungeon(p.getUniqueId())) {
                    p.closeInventory();
                    plugin.dungeons().join(p);
                    plugin.parties().pullMembers(p);
                }
            }
            case 14 -> new PartyMenu(plugin, p).open(p);
            case 16 -> new RankingMenu(plugin, RankingManager.Category.BOSS_KILLS).open(p);
            case 19 -> {
                DungeonDefinition d = plugin.dungeons().active() != null ? plugin.dungeons().active().def()
                        : plugin.scheduler().nextDungeon();
                if (d != null) new RewardMenu(plugin, d, this).open(p);
            }
            case 20 -> new ShopMenu(plugin, p).open(p);
            case 21 -> new QuestMenu(plugin, p).open(p);
            case 22 -> { p.closeInventory(); plugin.mailbox().claim(p); }
            case 23 -> { p.closeInventory(); p.performCommand("dungeon stats"); }
            case 25 -> {
                if (plugin.dungeons().isInDungeon(p.getUniqueId())) { p.closeInventory(); plugin.dungeons().leave(p); }
            }
            default -> { }
        }
    }
}
