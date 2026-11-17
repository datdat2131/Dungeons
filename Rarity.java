package com.astrax.dungeon.reward;

public enum Rarity {
    COMMON("&7"), RARE("&9"), EPIC("&5"), LEGENDARY("&6"), MYTHIC("&c"), DIVINE("&b&l");

    private final String color;
    Rarity(String color) { this.color = color; }
    public String color() { return color; }

    public static Rarity parse(String s, Rarity def) {
        try { return valueOf(s.toUpperCase()); } catch (Exception e) { return def; }
    }
}
