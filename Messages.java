package com.astrax.dungeon.util;

import com.astrax.dungeon.AstraXDungeon;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class Messages {
    private final AstraXDungeon plugin;
    private FileConfiguration cfg;

    public Messages(AstraXDungeon plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        java.io.InputStream in = plugin.getResource("messages.yml");
        if (in != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)));
        }
    }

    /** Thay {key} bang value theo cap: raw("opened", "dungeon", "X", "seconds", 120) */
    public String raw(String key, Object... kv) {
        String s = cfg.getString(key);
        if (s == null) s = key;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return s;
    }

    public Component get(String key, Object... kv) {
        return Text.c(cfg.getString("prefix", "") + raw(key, kv));
    }

    public Component plain(String key, Object... kv) { return Text.c(raw(key, kv)); }

    public String modifierName(String id) { return cfg.getString("modifiers." + id + ".name", id); }
    public String modifierDesc(String id) { return cfg.getString("modifiers." + id + ".desc", ""); }
}
