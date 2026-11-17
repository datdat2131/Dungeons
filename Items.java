package com.astrax.dungeon.gui;

import com.astrax.dungeon.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;

public final class Items {
    private Items() {}

    public static Component line(String s) { return Text.c(s).decoration(TextDecoration.ITALIC, false); }

    public static Material material(String name, Material def) {
        Material m = Material.matchMaterial(name == null ? "" : name);
        return m == null || m.isAir() ? def : m;
    }

    public static ItemStack make(Material m, String name, String... lore) { return make(m, name, Arrays.asList(lore)); }

    public static ItemStack make(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(line(name));
        if (!lore.isEmpty()) meta.lore(lore.stream().map(Items::line).toList());
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack glow(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack head(OfflinePlayer p, String name, List<String> lore) {
        ItemStack it = make(Material.PLAYER_HEAD, name, lore);
        SkullMeta sm = (SkullMeta) it.getItemMeta();
        sm.setOwningPlayer(p);
        it.setItemMeta(sm);
        return it;
    }
}
