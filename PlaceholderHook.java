package com.astrax.dungeon.integration;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.database.Database;
import com.astrax.dungeon.dungeon.DungeonDefinition;
import com.astrax.dungeon.dungeon.DungeonSession;
import com.astrax.dungeon.party.Party;
import com.astrax.dungeon.ranking.RankingManager;
import com.astrax.dungeon.util.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/** %dungeon_<ten>% - chi doc cache trong bo nho, khong bao gio cham DB tren main thread. */
public final class PlaceholderHook extends PlaceholderExpansion {
    private final AstraXDungeon plugin;

    public PlaceholderHook(AstraXDungeon plugin) { this.plugin = plugin; }

    @Override public String getIdentifier() { return "dungeon"; }
    @Override public String getAuthor() { return "AstraX"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String key = params.toLowerCase();
        DungeonSession active = plugin.dungeons().active();
        DungeonDefinition next = plugin.scheduler().nextDungeon();

        // --- thong tin chung ---
        switch (key) {
            case "next_time": return Text.duration(Math.max(0, plugin.scheduler().secondsUntilNext()));
            case "next_name": return next == null ? "-" : Text.legacy(next.displayName());
            case "next_golden": return plugin.scheduler().nextIsGolden() ? "Co" : "Khong";
            case "current": return active == null ? "-" : Text.legacy(active.def().displayName());
            case "state": return active == null ? "CLOSED" : active.state().name();
            case "players": return String.valueOf(active == null ? 0 : active.players().size());
            default: break;
        }
        if (player == null) return "";

        // --- thong tin ca nhan ---
        UUID id = player.getUniqueId();
        Database.Stats st = plugin.ranking().stats(id);
        switch (key) {
            case "in_dungeon": return plugin.dungeons().isInDungeon(id) ? "Co" : "Khong";
            case "party_size": {
                Party p = plugin.parties().partyOf(id);
                return String.valueOf(p == null ? 0 : p.size());
            }
            case "streak": return String.valueOf(plugin.quests().effectiveStreak(id));
            case "level": return String.valueOf(RankingManager.levelOf(st == null ? 0 : st.points()));
            case "points": return String.valueOf(st == null ? 0 : (long) st.points());
            case "completions": return String.valueOf(st == null ? 0 : st.completions());
            case "boss_kills": return String.valueOf(st == null ? 0 : st.bossKills());
            case "damage": return String.valueOf(st == null ? 0 : (long) st.damage());
            case "best_time": return st == null || st.bestTime() == 0 ? "-" : Text.duration(st.bestTime());
            case "ranking": {
                int r = plugin.ranking().rankOf(RankingManager.Category.BOSS_KILLS, id);
                return r < 0 ? "-" : String.valueOf(r);
            }
            default: return null;
        }
    }
}
