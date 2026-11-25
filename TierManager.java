package com.astrax.dungeon.tier;

import com.astrax.dungeon.AstraXDungeon;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** tiers.yml tu sinh ra tu code lan dau; moi gia tri deu sua duoc, thieu thi lay mac dinh. */
public final class TierManager {
    private final AstraXDungeon plugin;
    private final Map<String, Tier> tiers = new LinkedHashMap<>();
    private boolean enforceLevel;

    public TierManager(AstraXDungeon plugin) { this.plugin = plugin; }

    public void load() {
        File f = new File(plugin.getDataFolder(), "tiers.yml");
        YamlConfiguration builtin = builtin();
        if (!f.exists()) {
            try {
                f.getParentFile().mkdirs();
                builtin.save(f);
            } catch (IOException e) {
                plugin.getLogger().warning("Khong tao duoc tiers.yml: " + e.getMessage());
            }
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        enforceLevel = y.getBoolean("enforce-level", false);

        tiers.clear();
        ConfigurationSection user = y.getConfigurationSection("tiers");
        ConfigurationSection defs = builtin.getConfigurationSection("tiers");
        for (String k : defs.getKeys(false)) {
            tiers.put(k, new Tier(k, user == null ? null : user.getConfigurationSection(k), defs.getConfigurationSection(k)));
        }
        if (user != null) {
            for (String k : user.getKeys(false)) {
                String up = k.toUpperCase();
                if (!tiers.containsKey(up)) {
                    tiers.put(up, new Tier(up, user.getConfigurationSection(k), defs.getConfigurationSection("COMMON")));
                }
            }
        }
    }

    public Tier get(String key) {
        Tier t = key == null ? null : tiers.get(key.toUpperCase());
        return t != null ? t : tiers.get("COMMON");
    }

    public Collection<Tier> all() { return tiers.values(); }
    public boolean enforceLevel() { return enforceLevel; }

    // ---------- gia tri mac dinh ----------
    private static void t(YamlConfiguration y, String name, Object... kv) {
        for (int i = 0; i + 1 < kv.length; i += 2) y.set("tiers." + name + "." + kv[i], kv[i + 1]);
    }

    private static YamlConfiguration builtin() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("enforce-level", false);

        t(y, "COMMON",
            "display-name", "&7Common", "color", "&7", "waves", 5, "min-level", 1, "lives", -1,
            "max-duration-minutes", 30, "wave-delay-seconds", 8, "mob-cap", 30, "wave-clear-points", 20.0,
            "mob.health-mult", 1.0, "mob.damage-mult", 1.0, "mob.speed-mult", 1.0,
            "mob.wave-health-growth", 0.05, "mob.wave-damage-growth", 0.03,
            "mob.amount-base", 6.0, "mob.amount-growth", 1.2, "mob.elite-chance", 0.05, "mob.trait-chance", 0.10,
            "mob.gear", "LEATHER", "mob.gear-chance", 0.4, "mob.effects", List.of(), "mob.skill-chance", 0.0,
            "boss.health-mult", 1.0, "boss.damage-mult", 1.0, "boss.skill-damage-mult", 1.0, "boss.enrage-seconds", 600,
            "reward.money-mult", 1.0, "reward.chance-mult", 1.0, "reward.points-mult", 1.0, "reward.min-points", 30.0,
            "golden-only", false);

        t(y, "EPIC",
            "display-name", "&5Epic", "color", "&5", "waves", 10, "min-level", 2, "lives", 5,
            "max-duration-minutes", 40, "wave-delay-seconds", 8, "mob-cap", 36, "wave-clear-points", 30.0,
            "mob.health-mult", 2.5, "mob.damage-mult", 1.6, "mob.speed-mult", 1.05,
            "mob.wave-health-growth", 0.07, "mob.wave-damage-growth", 0.04,
            "mob.amount-base", 8.0, "mob.amount-growth", 1.5, "mob.elite-chance", 0.10, "mob.trait-chance", 0.25,
            "mob.gear", "IRON", "mob.gear-chance", 0.7, "mob.effects", List.of(), "mob.skill-chance", 0.15,
            "boss.health-mult", 1.0, "boss.damage-mult", 1.0, "boss.skill-damage-mult", 1.0, "boss.enrage-seconds", 720,
            "reward.money-mult", 1.0, "reward.chance-mult", 1.0, "reward.points-mult", 1.5, "reward.min-points", 60.0,
            "golden-only", false);

        t(y, "LEGENDARY",
            "display-name", "&6Legendary", "color", "&6", "waves", 12, "min-level", 4, "lives", 3,
            "max-duration-minutes", 50, "wave-delay-seconds", 10, "mob-cap", 40, "wave-clear-points", 40.0,
            "mob.health-mult", 5.0, "mob.damage-mult", 2.4, "mob.speed-mult", 1.10,
            "mob.wave-health-growth", 0.08, "mob.wave-damage-growth", 0.05,
            "mob.amount-base", 10.0, "mob.amount-growth", 1.7, "mob.elite-chance", 0.15, "mob.trait-chance", 0.40,
            "mob.gear", "DIAMOND", "mob.gear-chance", 0.85, "mob.effects", List.of("RESISTANCE:0"), "mob.skill-chance", 0.30,
            "boss.health-mult", 1.0, "boss.damage-mult", 1.0, "boss.skill-damage-mult", 1.0, "boss.enrage-seconds", 720,
            "reward.money-mult", 1.0, "reward.chance-mult", 1.0, "reward.points-mult", 2.0, "reward.min-points", 100.0,
            "golden-only", false);

        t(y, "MYTHICAL",
            "display-name", "&cMythical", "color", "&c", "waves", 15, "min-level", 7, "lives", 2,
            "max-duration-minutes", 60, "wave-delay-seconds", 10, "mob-cap", 50, "wave-clear-points", 60.0,
            "mob.health-mult", 10.0, "mob.damage-mult", 3.5, "mob.speed-mult", 1.15,
            "mob.wave-health-growth", 0.10, "mob.wave-damage-growth", 0.06,
            "mob.amount-base", 12.0, "mob.amount-growth", 2.0, "mob.elite-chance", 0.25, "mob.trait-chance", 0.65,
            "mob.gear", "NETHERITE", "mob.gear-chance", 1.0, "mob.effects", List.of("RESISTANCE:1", "STRENGTH:0"), "mob.skill-chance", 0.50,
            "boss.health-mult", 1.0, "boss.damage-mult", 1.0, "boss.skill-damage-mult", 1.0, "boss.enrage-seconds", 600,
            "reward.money-mult", 1.0, "reward.chance-mult", 1.0, "reward.points-mult", 3.0, "reward.min-points", 200.0,
            "golden-only", false);

        y.setComments("enforce-level", List.of("true = phai du cap Dungeon (min-level cua tier) moi vao duoc. Cap tinh tu tong diem dong gop."));
        y.setComments("tiers.COMMON.waves", List.of("So dot (wave). Dot cuoi luon la Mini-Boss, moi 5 dot co dot Tinh Anh, dot 3,7,11 la dot Bay Dan."));
        y.setComments("tiers.COMMON.lives", List.of("So mang moi nguoi trong 1 luot. -1 = khong gioi han. Het mang bi dua ve sanh."));
        y.setComments("tiers.COMMON.mob.health-mult", List.of("He so mau/sat thuong/toc do quai cua tier; wave-*-growth = tang them moi dot."));
        y.setComments("tiers.COMMON.mob.gear", List.of("Bo giap quai: LEATHER, CHAINMAIL, IRON, DIAMOND, NETHERITE. effects dang TYPE:cap (vo han)."));
        y.setComments("tiers.COMMON.boss.enrage-seconds", List.of("Boss danh qua lau se NOI DIEN. 0 = tat."));
        y.setComments("tiers.COMMON.reward.points-mult", List.of("Nhan diem dong gop. min-points = diem toi thieu de nhan thuong chinh."));
        y.setComments("tiers.COMMON.golden-only", List.of("true = tier nay chi mo vao khung Gio Vang."));
        return y;
    }
}
