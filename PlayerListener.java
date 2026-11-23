package com.astrax.dungeon.listener;

import com.astrax.dungeon.AstraXDungeon;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class PlayerListener implements Listener {
    private final AstraXDungeon plugin;

    public PlayerListener(AstraXDungeon plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        plugin.ranking().loadStats(id);
        plugin.quests().loadPlayer(id);
        plugin.mailbox().notifyJoin(p);

        // thoat game luc dang guc (spectator) => tra lai che do choi, dua ve sanh
        if (p.getPersistentDataContainer().has(plugin.downKey(), PersistentDataType.BYTE)
                && !plugin.dungeons().isInDungeon(id)) {
            p.getPersistentDataContainer().remove(plugin.downKey());
            p.setGameMode(GameMode.SURVIVAL);
            var lobby = plugin.dungeons().lobby();
            if (lobby != null) p.teleport(lobby);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        var s = plugin.dungeons().active();
        plugin.revive().release(id);
        // go khoi danh sach hien tai nhung GIU diem dong gop: neu dong gop du van nhan thuong qua hop thu
        if (s != null && s.players().remove(id)) s.markQuit(id);
        plugin.quests().unload(id);
        plugin.ranking().unload(id);
        plugin.parties().handleQuit(id);
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

    private boolean blocked(Player p) {
        return !plugin.getConfig().getBoolean("dungeon.block-edit", false)
                && plugin.dungeons().isInDungeon(p.getUniqueId())
                && !p.hasPermission("astrax.dungeon.admin");
    }
}
