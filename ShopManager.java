package com.astrax.dungeon.shop;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.database.Database;
import com.astrax.dungeon.reward.LootTable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cua hang doi Token (shop.yml). Token la vat pham co PDC 'dungeon_item' = id token (vd common_token).
 * Ho tro gioi han mua moi ngay (daily-limit) va doi token cap thap len cap cao.
 */
public final class ShopManager {
    public record Entry(String id, LootTable.Drop drop, Map<String, Integer> cost, int dailyLimit) {}

    private final AstraXDungeon plugin;
    private final List<Entry> entries = new ArrayList<>();

    public ShopManager(AstraXDungeon plugin) { this.plugin = plugin; }

    public void load() {
        entries.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "shop.yml"));
        for (Map<?, ?> m : y.getMapList("items")) {
            LootTable.Drop d = LootTable.parseDrop(m);
            Map<String, Integer> cost = new LinkedHashMap<>();
            if (m.get("cost") instanceof Map<?, ?> cm) {
                for (Map.Entry<?, ?> en : cm.entrySet()) {
                    cost.put(String.valueOf(en.getKey()), en.getValue() instanceof Number n ? n.intValue() : 1);
                }
            }
            int limit = m.get("daily-limit") instanceof Number ln ? ln.intValue() : 0;
            entries.add(new Entry(d.id(), d, cost, limit));
        }
    }

    public List<Entry> entries() { return entries; }

    public static String pretty(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    // ---------- token trong tui do ----------
    private boolean isToken(ItemStack it, String tokenId) {
        if (it == null || !it.hasItemMeta()) return false;
        String id = it.getItemMeta().getPersistentDataContainer().get(plugin.itemKey(), PersistentDataType.STRING);
        return tokenId.equals(id);
    }

    public int count(Player p, String tokenId) {
        int n = 0;
        for (ItemStack it : p.getInventory().getContents()) if (isToken(it, tokenId)) n += it.getAmount();
        return n;
    }

    private void take(Player p, String tokenId, int amount) {
        PlayerInventory inv = p.getInventory();
        int need = amount;
        for (int i = 0; i < inv.getSize() && need > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (!isToken(it, tokenId)) continue;
            int rm = Math.min(need, it.getAmount());
            if (rm >= it.getAmount()) {
                inv.setItem(i, null);
            } else {
                ItemStack copy = it.clone();
                copy.setAmount(it.getAmount() - rm);
                inv.setItem(i, copy);
            }
            need -= rm;
        }
    }

    private boolean affordable(Player p, Entry e) {
        for (Map.Entry<String, Integer> c : e.cost().entrySet()) if (count(p, c.getKey()) < c.getValue()) return false;
        return true;
    }

    /** Tru token + trao vat pham. false neu khong du token. */
    private boolean complete(Player p, Entry e) {
        if (!affordable(p, e)) return false;
        for (Map.Entry<String, Integer> c : e.cost().entrySet()) take(p, c.getKey(), c.getValue());
        plugin.distributor().give(p, e.drop(), e.drop().amount());
        p.sendMessage(plugin.messages().get("shop-bought", "item", e.drop().rarity().color() + e.drop().name()));
        return true;
    }

    public void buy(Player p, Entry e) {
        var m = plugin.messages();
        if (!affordable(p, e)) { p.sendMessage(m.get("shop-not-enough")); return; }
        if (e.dailyLimit() <= 0) { complete(p, e); return; }

        Database db = plugin.database();
        String uuid = p.getUniqueId().toString();
        String day = LocalDate.now().toString();
        db.async(() -> db.tryPurchase(uuid, e.id(), day, e.dailyLimit())).whenComplete((ok, err) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (err != null || ok == null) { p.sendMessage(m.get("shop-error")); return; }
                    if (!ok) { p.sendMessage(m.get("shop-limit", "limit", e.dailyLimit())); return; }
                    if (!p.isOnline() || !complete(p, e)) {
                        // khong du token nua / da thoat: hoan luot mua
                        db.async(() -> { db.refundPurchase(uuid, e.id(), day); return null; });
                        if (p.isOnline()) p.sendMessage(m.get("shop-not-enough"));
                    }
                }));
    }
}
