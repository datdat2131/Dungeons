package com.astrax.dungeon.mob;

import java.util.List;

/** Mo ta 1 con quai: loai (EntityType hoac "mythic:Ten"), dac tinh, tinh anh, he so mau, ten rieng, ky nang chu dong. */
public record MobSpec(String type, List<String> traits, boolean elite, double hpMult, String name, List<String> skills) {
    public MobSpec(String type, List<String> traits, boolean elite, double hpMult, String name) {
        this(type, traits, elite, hpMult, name, List.of());
    }
}
