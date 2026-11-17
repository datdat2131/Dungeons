package com.astrax.dungeon.dungeon;

import java.util.concurrent.ThreadLocalRandom;

/** Bien the ngau nhien moi luot. Hieu ung thuc te se gan o Giai doan 3 (Boss/Mob). */
public enum Modifier {
    FAST_BOSS, EXPLOSIVE_MOBS, NO_REGEN, DARKNESS, GLASS_CANNON, DOUBLE_LOOT;

    public static Modifier random() {
        Modifier[] v = values();
        return v[ThreadLocalRandom.current().nextInt(v.length)];
    }
}
