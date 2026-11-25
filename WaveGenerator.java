package com.astrax.dungeon.mob;

import com.astrax.dungeon.dungeon.DungeonDefinition;
import com.astrax.dungeon.tier.Tier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sinh cac dot wave tu mob-pool cua Dungeon + so dot cua Tier. Moi lan mo Dungeon la 1 bo wave khac nhau.
 * Dot cuoi = MINIBOSS, moi 5 dot = ELITE, dot 3,7,11... = SWARM. Neu Dungeon co 'waves:' thu cong thi dung no.
 */
public final class WaveGenerator {
    private record Pool(String type, int weight, int minWave, List<String> traits, List<String> skills) {}

    private WaveGenerator() {}

    public static List<Wave> generate(DungeonDefinition def, Tier tier) {
        // 1) wave thu cong (kieu cu)
        List<Wave> explicit = new ArrayList<>();
        int idx = 1;
        for (List<String> w : def.waves()) {
            List<MobSpec> mobs = new ArrayList<>();
            for (String entry : w) {
                String spec = entry;
                int amount = 1;
                int i = entry.lastIndexOf(':');
                if (i > 0) {
                    try {
                        amount = Integer.parseInt(entry.substring(i + 1).trim());
                        spec = entry.substring(0, i).trim();
                    } catch (NumberFormatException ignored) { /* ten mythic co dau ':' */ }
                }
                for (int k = 0; k < amount; k++) mobs.add(new MobSpec(spec, List.of(), false, 1.0, null));
            }
            explicit.add(new Wave(idx++, "NORMAL", mobs));
        }
        if (!explicit.isEmpty()) return explicit;

        // 2) sinh tu mob-pool
        YamlConfiguration c = def.config();
        List<Pool> pool = new ArrayList<>();
        for (Map<?, ?> m : c.getMapList("mob-pool")) {
            String type = m.get("type") == null ? "ZOMBIE" : String.valueOf(m.get("type"));
            int weight = m.get("weight") instanceof Number wn ? wn.intValue() : 10;
            int minWave = m.get("min-wave") instanceof Number mn ? mn.intValue() : 1;
            List<String> traits = new ArrayList<>();
            if (m.get("traits") instanceof List<?> l) for (Object o : l) traits.add(String.valueOf(o).toLowerCase());
            List<String> skl = new ArrayList<>();
            if (m.get("skills") instanceof List<?> sl) for (Object o : sl) skl.add(String.valueOf(o));
            pool.add(new Pool(type, Math.max(1, weight), minWave, traits, skl));
        }
        if (pool.isEmpty()) pool.add(new Pool("ZOMBIE", 10, 1, List.of(), List.of()));

        ConfigurationSection mb = c.getConfigurationSection("miniboss");
        double base = c.getDouble("wave-scaling.amount-base", tier.amountBase());
        double growth = c.getDouble("wave-scaling.amount-growth", tier.amountGrowth());
        int total = tier.waves();
        ThreadLocalRandom r = ThreadLocalRandom.current();

        List<Wave> out = new ArrayList<>();
        for (int n = 1; n <= total; n++) {
            String kind = "NORMAL";
            if (n == total) kind = "MINIBOSS";
            else if (n % 5 == 0) kind = "ELITE";
            else if (n >= 3 && n % 4 == 3) kind = "SWARM";

            double amt = base + growth * (n - 1);
            switch (kind) {
                case "ELITE" -> amt *= 0.6;
                case "SWARM" -> amt *= 1.8;
                case "MINIBOSS" -> amt *= 0.5;
                default -> { }
            }
            int amount = Math.max(2, (int) Math.round(amt));

            List<MobSpec> mobs = new ArrayList<>();
            if (kind.equals("MINIBOSS")) {
                if (mb != null) {
                    List<String> traits = new ArrayList<>();
                    for (String t : mb.getStringList("traits")) traits.add(t.toLowerCase());
                    mobs.add(new MobSpec(mb.getString("type", "RAVAGER"), traits, true,
                            mb.getDouble("hp-mult", 6.0), mb.getString("name"), mb.getStringList("skills")));
                } else {
                    Pool p = pick(pool, n, r);
                    mobs.add(new MobSpec(p.type(), p.traits(), true, 6.0, null, p.skills()));
                }
            }
            for (int i = 0; i < amount; i++) {
                Pool p = pick(pool, n, r);
                mobs.add(new MobSpec(p.type(), p.traits(), kind.equals("ELITE"), kind.equals("SWARM") ? 0.6 : 1.0, null, p.skills()));
            }
            out.add(new Wave(n, kind, mobs));
        }
        return out;
    }

    private static Pool pick(List<Pool> pool, int wave, ThreadLocalRandom r) {
        List<Pool> ok = new ArrayList<>();
        for (Pool p : pool) if (p.minWave() <= wave) ok.add(p);
        if (ok.isEmpty()) ok = pool;
        int total = 0;
        for (Pool p : ok) total += p.weight();
        int roll = r.nextInt(total);
        for (Pool p : ok) {
            roll -= p.weight();
            if (roll < 0) return p;
        }
        return ok.get(0);
    }
}
