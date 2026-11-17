package com.astrax.dungeon.listener;

import com.astrax.dungeon.AstraXDungeon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final AstraXDungeon plugin;

    public PlayerListener(AstraXDungeon plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.ranking().loadStats(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.ranking().unload(e.getPlayer().getUniqueId());
        // Giai doan 4: giu lai dong gop, chong thoat game ne co che. Hien tai chi go khoi session.
        var s = plugin.dungeons().active();
        // chi go khoi danh sach hien tai; diem dong gop van giu de vao lai khong mat
        if (s != null) s.players().remove(e.getPlayer().getUniqueId());
        plugin.parties().handleQuit(e.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (blocked(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(plugin.messages().get("block-denied"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (blocked(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(plugin.messages().get("block-denied"));
        }
    }

    private boolean blocked(org.bukkit.entity.Player p) {
        return !plugin.getConfig().getBoolean("dungeon.block-edit", false)
                && plugin.dungeons().isInDungeon(p.getUniqueId())
                && !p.hasPermission("astrax.dungeon.admin");
    }
}
