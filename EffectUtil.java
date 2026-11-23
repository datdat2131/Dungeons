package com.astrax.dungeon.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Dinh dang hieu ung: "TYPE", "TYPE:cap", "TYPE:cap:giay". giay am/thieu + defaultSeconds=-1 => vo han. */
public final class EffectUtil {
    private EffectUtil() {}

    @SuppressWarnings("deprecation")
    public static PotionEffectType type(String name) {
        return PotionEffectType.getByName(name.trim().toUpperCase());
    }

    public static void apply(LivingEntity e, String spec, int defaultSeconds) {
        if (spec == null || spec.isBlank()) return;
        String[] p = spec.split(":");
        PotionEffectType t = type(p[0]);
        if (t == null) return;
        int amp = p.length > 1 ? parse(p[1], 0) : 0;
        int seconds = p.length > 2 ? parse(p[2], defaultSeconds) : defaultSeconds;
        int ticks = seconds < 0 ? PotionEffect.INFINITE_DURATION : seconds * 20;
        e.addPotionEffect(new PotionEffect(t, ticks, amp, false, true));
    }

    private static int parse(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
