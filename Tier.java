package com.astrax.dungeon.tier;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/** Mot cap do Dungeon (Common/Epic/Legendary/Mythical). Doc tu tiers.yml, thieu thi dung gia tri mac dinh. */
public final class Tier {
    private final String key;
    private final ConfigurationSection cfg;  // gia tri nguoi dung (co the null)
    private final ConfigurationSection def;  // gia tri mac dinh

    public Tier(String key, ConfigurationSection cfg, ConfigurationSection def) {
        this.key = key;
        this.cfg = cfg;
        this.def = def;
    }

    private boolean has(String p) { return cfg != null && cfg.contains(p); }
    private double d(String p) { return has(p) ? cfg.getDouble(p) : def.getDouble(p); }
    private int i(String p) { return has(p) ? cfg.getInt(p) : def.getInt(p); }
    private String s(String p) { return has(p) ? cfg.getString(p, "") : def.getString(p, ""); }
    private boolean b(String p) { return has(p) ? cfg.getBoolean(p) : def.getBoolean(p); }
    private List<String> l(String p) { return has(p) ? cfg.getStringList(p) : def.getStringList(p); }

    public String key() { return key; }
    public String displayName() { return s("display-name"); }
    public String color() { return s("color"); }
    public int waves() { return Math.max(1, i("waves")); }
    public int minLevel() { return i("min-level"); }
    /** -1 = khong gioi han mang. */
    public int lives() { return i("lives"); }
    public int maxDurationMinutes() { return Math.max(1, i("max-duration-minutes")); }
    public int waveDelaySeconds() { return Math.max(1, i("wave-delay-seconds")); }
    public int mobCap() { return Math.max(1, i("mob-cap")); }
    public double waveClearPoints() { return d("wave-clear-points"); }
    public boolean goldenOnly() { return b("golden-only"); }

    public double mobHealthMult() { return d("mob.health-mult"); }
    public double mobDamageMult() { return d("mob.damage-mult"); }
    public double mobSpeedMult() { return d("mob.speed-mult"); }
    public double waveHealthGrowth() { return d("mob.wave-health-growth"); }
    public double waveDamageGrowth() { return d("mob.wave-damage-growth"); }
    public double amountBase() { return d("mob.amount-base"); }
    public double amountGrowth() { return d("mob.amount-growth"); }
    public double eliteChance() { return d("mob.elite-chance"); }
    public double traitChance() { return d("mob.trait-chance"); }
    public String gear() { return s("mob.gear"); }
    public double gearChance() { return d("mob.gear-chance"); }
    public List<String> mobEffects() { return l("mob.effects"); }

    public double bossHealthMult() { return d("boss.health-mult"); }
    public double bossDamageMult() { return d("boss.damage-mult"); }
    public double bossSkillDamageMult() { return d("boss.skill-damage-mult"); }
    public int bossEnrageSeconds() { return i("boss.enrage-seconds"); }

    public double moneyMult() { return d("reward.money-mult"); }
    public double chanceMult() { return d("reward.chance-mult"); }
    public double pointsMult() { return d("reward.points-mult"); }
    public double minPoints() { return d("reward.min-points"); }
}
