package com.astrax.dungeon.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Text {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private Text() {}

    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    /** Chuyen ma &a thanh mau kieu section (dung cho PlaceholderAPI). */
    public static String legacy(String amp) { return SECTION.serialize(c(amp)); }

    public static Component c(String s) { return LEGACY.deserialize(s == null ? "" : s); }

    public static String duration(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        if (h > 0) return h + "g " + m + "p " + s + "s";
        if (m > 0) return m + "p " + s + "s";
        return s + "s";
    }
}
