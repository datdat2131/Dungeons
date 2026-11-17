package com.astrax.dungeon.command;

import com.astrax.dungeon.AstraXDungeon;
import com.astrax.dungeon.dungeon.DungeonDefinition;
import com.astrax.dungeon.gui.MainMenu;
import com.astrax.dungeon.gui.RankingMenu;
import com.astrax.dungeon.ranking.RankingManager;
import com.astrax.dungeon.dungeon.Modifier;
import com.astrax.dungeon.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class DungeonCommand implements CommandExecutor, TabCompleter {
    private final AstraXDungeon plugin;

    public DungeonCommand(AstraXDungeon plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            if (s instanceof Player p0) new MainMenu(plugin, p0).open(p0); else help(s);
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "next" -> next(s);
            case "list" -> list(s);
            case "info" -> info(s, a);
            case "join" -> {
                if (!(s instanceof Player p)) return true;
                plugin.dungeons().join(p);
                plugin.parties().pullMembers(p);
            }
            case "leave" -> {
                if (!(s instanceof Player p)) return true;
                plugin.dungeons().leave(p);
            }
            case "party" -> {
                if (!(s instanceof Player p)) return true;
                party(p, a);
            }
            case "rewards" -> rewards(s, a);
            case "stats" -> {
                if (!(s instanceof Player p)) return true;
                stats(p);
            }
            case "menu" -> {
                if (!(s instanceof Player p)) return true;
                new MainMenu(plugin, p).open(p);
            }
            case "ranking" -> {
                if (!(s instanceof Player p)) return true;
                new RankingMenu(plugin, RankingManager.Category.BOSS_KILLS).open(p);
            }
            case "admin" -> admin(s, a);
            default -> help(s);
        }
        return true;
    }

    private void help(CommandSender s) {
        s.sendMessage(Text.c("&6/dungeon &enext &7| &elist &7| &einfo <id> &7| &ejoin &7| &eleave &7| &erewards &7| &estats &7| &eparty"));
        if (s.hasPermission("astrax.dungeon.admin"))
            s.sendMessage(Text.c("&c/dungeon admin &7reload | start <id> | stop | setspawn <id> | setbossspawn <id> | setlobby | boss spawn/kill"));
    }

    private void party(Player p, String[] a) {
        var pm = plugin.parties();
        String sub = a.length < 2 ? "info" : a[1].toLowerCase();
        switch (sub) {
            case "create" -> pm.create(p);
            case "invite" -> { if (a.length < 3) { help(p); return; } pm.invite(p, a[2]); }
            case "accept" -> pm.accept(p);
            case "leave" -> pm.leave(p);
            case "kick" -> { if (a.length < 3) { help(p); return; } pm.kick(p, a[2]); }
            case "promote" -> { if (a.length < 3) { help(p); return; } pm.promote(p, a[2]); }
            default -> pm.info(p);
        }
    }

    private void rewards(CommandSender s, String[] a) {
        var m = plugin.messages();
        DungeonDefinition d;
        if (a.length >= 2) d = plugin.dungeons().get(a[1]).orElse(null);
        else if (plugin.dungeons().active() != null) d = plugin.dungeons().active().def();
        else d = plugin.scheduler().nextDungeon();
        if (d == null) { s.sendMessage(m.get("unknown-dungeon", "id", a.length >= 2 ? a[1] : "?")); return; }
        var table = plugin.rewards().get(d.bossId());
        if (table == null) { s.sendMessage(m.get("boss-missing", "id", d.bossId())); return; }
        s.sendMessage(m.plain("rewards-header", "dungeon", d.displayName()));
        if (table.baseMoney() > 0) s.sendMessage(m.plain("rewards-money", "money", (long) table.baseMoney()));
        for (var drop : table.drops()) {
            s.sendMessage(m.plain("rewards-line", "item", drop.rarity().color() + drop.name(), "chance", drop.chance()));
        }
    }

    private void stats(Player p) {
        var m = plugin.messages();
        plugin.database().async(() -> plugin.database().loadStats(p.getUniqueId().toString()))
                .whenComplete((st, err) -> org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    if (err != null || st == null) { p.sendMessage(m.get("stats-none")); return; }
                    p.sendMessage(m.get("stats", "c", st.completions(), "b", st.bossKills(), "p", (long) st.points(),
                            "d", (long) st.damage(), "t", st.bestTime() == 0 ? "-" : Text.duration(st.bestTime())));
                }));
    }

    private void next(CommandSender s) {
        var m = plugin.messages();
        var act = plugin.dungeons().active();
        if (act != null) {
            s.sendMessage(m.get("current", "dungeon", act.def().displayName(), "state", act.state(), "count", act.players().size()));
        }
        DungeonDefinition d = plugin.scheduler().nextDungeon();
        if (d == null) return;
        s.sendMessage(m.get("next", "dungeon", d.displayName(), "time", Text.duration(plugin.scheduler().secondsUntilNext())));
        if (plugin.scheduler().nextIsGolden()) s.sendMessage(m.get("golden-soon"));
    }

    private void list(CommandSender s) {
        for (DungeonDefinition d : plugin.dungeons().all()) {
            s.sendMessage(Text.c("&7- " + d.displayName() + " &8(" + d.id() + ") &7Lv " + d.recommendedLevel()
                    + (d.enabled() ? "" : " &c[tat]")));
        }
    }

    private void info(CommandSender s, String[] a) {
        if (a.length < 2) { help(s); return; }
        plugin.dungeons().get(a[1]).ifPresentOrElse(d -> {
            s.sendMessage(Text.c(d.displayName()));
            s.sendMessage(Text.c("&7Boss: &f" + d.bossId() + " &7| Lv: &f" + d.recommendedLevel()
                    + " &7| Toi da: &f" + d.maxPlayers()));
        }, () -> s.sendMessage(plugin.messages().get("unknown-dungeon", "id", a[1])));
    }

    private void admin(CommandSender s, String[] a) {
        var m = plugin.messages();
        if (!s.hasPermission("astrax.dungeon.admin")) { s.sendMessage(m.get("no-perm")); return; }
        if (a.length < 2) { help(s); return; }
        switch (a[1].toLowerCase()) {
            case "reload" -> { plugin.reloadAll(); s.sendMessage(m.get("reloaded")); }
            case "stop" -> plugin.dungeons().end("stopped");
            case "start" -> {
                if (a.length < 3) { help(s); return; }
                plugin.dungeons().get(a[2]).ifPresentOrElse(
                        d -> plugin.dungeons().open(d, Modifier.random(), 1.0, 1.0),
                        () -> s.sendMessage(m.get("unknown-dungeon", "id", a[2])));
            }
            case "setspawn" -> {
                if (!(s instanceof Player p) || a.length < 3) { help(s); return; }
                plugin.dungeons().get(a[2]).ifPresentOrElse(d -> {
                    d.setSpawn(p.getLocation());
                    p.sendMessage(m.get("spawn-set", "id", d.id()));
                }, () -> p.sendMessage(m.get("unknown-dungeon", "id", a[2])));
            }
            case "boss" -> {
                if (a.length < 3) { help(s); return; }
                if (a[2].equalsIgnoreCase("spawn")) {
                    if (!plugin.dungeons().startBoss()) s.sendMessage(m.get("boss-no-active"));
                } else if (a[2].equalsIgnoreCase("kill")) {
                    plugin.dungeons().killBoss();
                }
            }
            case "setbossspawn" -> {
                if (!(s instanceof Player p) || a.length < 3) { help(s); return; }
                plugin.dungeons().get(a[2]).ifPresentOrElse(d -> {
                    d.setBossSpawn(p.getLocation());
                    p.sendMessage(m.get("bossspawn-set", "id", d.id()));
                }, () -> p.sendMessage(m.get("unknown-dungeon", "id", a[2])));
            }
            case "setlobby" -> {
                if (!(s instanceof Player p)) return;
                plugin.dungeons().setLobby(p.getLocation());
                p.sendMessage(m.get("lobby-set"));
            }
            default -> help(s);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.addAll(List.of("next", "list", "info", "join", "leave", "party", "rewards", "stats", "ranking", "menu"));
            if (s.hasPermission("astrax.dungeon.admin")) out.add("admin");
        } else if (a.length == 2 && a[0].equalsIgnoreCase("party")) {
            out.addAll(List.of("create", "invite", "accept", "leave", "kick", "promote", "info"));
        } else if (a.length == 2 && a[0].equalsIgnoreCase("admin") && s.hasPermission("astrax.dungeon.admin")) {
            out.addAll(List.of("reload", "start", "stop", "setspawn", "setbossspawn", "setlobby", "boss"));
        } else if ((a.length == 2 && (a[0].equalsIgnoreCase("info") || a[0].equalsIgnoreCase("rewards")))
                || (a.length == 3 && a[0].equalsIgnoreCase("admin")
                && (a[1].equalsIgnoreCase("start") || a[1].equalsIgnoreCase("setspawn") || a[1].equalsIgnoreCase("setbossspawn")))) {
            plugin.dungeons().all().forEach(d -> out.add(d.id()));
        }
        String last = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(last));
        return out;
    }
}
