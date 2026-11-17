package com.astrax.dungeon.schedule;

import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Khung gio vang: x diem dong gop, x ty le roi. Dinh dang "20:00-22:00", ho tro qua nua dem. */
public final class GoldenHour {
    private final boolean enabled;
    private final double contribution;
    private final double drop;
    private final List<LocalTime[]> windows = new ArrayList<>();

    public GoldenHour(ConfigurationSection sec) {
        enabled = sec != null && sec.getBoolean("enabled", false);
        contribution = sec == null ? 1.0 : sec.getDouble("contribution-multiplier", 1.0);
        drop = sec == null ? 1.0 : sec.getDouble("drop-multiplier", 1.0);
        if (sec == null) return;
        for (String w : sec.getStringList("windows")) {
            try {
                String[] p = w.split("-");
                windows.add(new LocalTime[]{LocalTime.parse(p[0].trim()), LocalTime.parse(p[1].trim())});
            } catch (Exception ignored) { /* bo qua dong sai dinh dang */ }
        }
    }

    public boolean isActive(ZonedDateTime t) {
        if (!enabled) return false;
        LocalTime n = t.toLocalTime();
        for (LocalTime[] w : windows) {
            boolean in = w[0].isBefore(w[1])
                    ? (!n.isBefore(w[0]) && n.isBefore(w[1]))
                    : (!n.isBefore(w[0]) || n.isBefore(w[1]));
            if (in) return true;
        }
        return false;
    }

    public double contribution() { return contribution; }
    public double drop() { return drop; }
}
