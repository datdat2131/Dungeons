package com.astrax.dungeon.util;

/** Ban mac dinh cua dungeons/ bosses/ rewards/ - dung khi jar thieu resource (khong bao gio crash). */
public final class DefaultFiles {
    private DefaultFiles() {}

    // id, ten, weight, boss, level, icon, waves ("A:8,B:4;C:2" => cac dot cach nhau bang ';')
    private static final String[][] DUNGEONS = {
        {"ancient_temple", "&6Ancient Temple", "10", "ancient_guardian", "1-10", "STONE_BRICKS",
            "ZOMBIE:8,SKELETON:4;SKELETON:8,SPIDER:4;ZOMBIE:10,HUSK:6"},
        {"infernal_fortress", "&cInfernal Fortress", "10", "infernal_king", "1-15", "NETHER_BRICKS",
            "ZOMBIFIED_PIGLIN:8,MAGMA_CUBE:4;WITHER_SKELETON:6,BLAZE:3;WITHER_SKELETON:10,BLAZE:5"},
        {"abyssal_temple", "&bAbyssal Temple", "10", "ocean_lord", "1-15", "PRISMARINE",
            "DROWNED:10;DROWNED:8,STRAY:4;DROWNED:12,SILVERFISH:6"},
        {"cursed_forest", "&2Cursed Forest", "10", "cursed_tree", "1-10", "DARK_OAK_LOG",
            "SPIDER:8,CAVE_SPIDER:6;WITCH:3,ZOMBIE:8;WITCH:5,SPIDER:10"},
        {"lost_pyramid", "&eLost Pyramid", "10", "pharaoh", "1-15", "SANDSTONE",
            "HUSK:10;HUSK:8,SKELETON:6;HUSK:12,SPIDER:6"},
        {"void_dimension", "&5Void Dimension", "6", "void_emperor", "5-20", "END_STONE",
            "ENDERMITE:10;ENDERMAN:4,SILVERFISH:8;ENDERMAN:6,ENDERMITE:10"},
    };

    // id, ten, entity, mau, scale, mau bossbar, quai phu
    private static final String[][] BOSSES = {
        {"ancient_guardian", "&6Ancient Guardian", "IRON_GOLEM", "5000", "2.0", "YELLOW", "ZOMBIE"},
        {"infernal_king", "&cInfernal King", "WITHER_SKELETON", "6000", "2.5", "RED", "BLAZE"},
        {"ocean_lord", "&bOcean Lord", "DROWNED", "6000", "2.5", "BLUE", "DROWNED"},
        {"cursed_tree", "&2Cursed Tree", "RAVAGER", "6000", "1.5", "GREEN", "SPIDER"},
        {"pharaoh", "&ePharaoh", "HUSK", "6500", "2.5", "YELLOW", "HUSK"},
        {"void_emperor", "&5Void Emperor", "ENDERMAN", "8000", "2.0", "PURPLE", "ENDERMITE"},
    };

    // boss, tien, idLoi, tenLoi, matLoi, idVuKhi, tenVuKhi, matVuKhi, idTitle, tenTitle
    private static final String[][] REWARDS = {
        {"ancient_guardian", "1000", "ancient_core", "Ancient Core", "NETHER_STAR",
            "guardian_sword", "Guardian Sword", "DIAMOND_SWORD", "ancient_title", "Ancient Slayer"},
        {"infernal_king", "1500", "infernal_core", "Infernal Core", "MAGMA_CREAM",
            "infernal_blade", "Infernal Blade", "NETHERITE_SWORD", "infernal_title", "Infernal Conqueror"},
        {"ocean_lord", "1500", "abyss_shard", "Abyss Shard", "PRISMARINE_SHARD",
            "ocean_trident", "Ocean Trident", "TRIDENT", "abyss_title", "Abyss Walker"},
        {"cursed_tree", "1500", "cursed_essence", "Cursed Essence", "SPIDER_EYE",
            "cursed_bow", "Cursed Bow", "BOW", "cursed_title", "Cursed Hunter"},
        {"pharaoh", "1800", "pharaoh_relic", "Pharaoh Relic", "GOLD_INGOT",
            "pharaoh_blade", "Pharaoh Blade", "GOLDEN_SWORD", "pharaoh_title", "Pharaohs Bane"},
        {"void_emperor", "2500", "void_shard", "Void Shard", "ENDER_EYE",
            "void_scythe", "Void Scythe", "NETHERITE_HOE", "void_title", "Void Destroyer"},
    };

    private static final String DUNGEON = """
id: @ID@
enabled: true
display-name: "@NAME@"
weight: @WEIGHT@
max-players: 30
min-players: 1
recommended-level: "@LEVEL@"
world: dungeon_world
boss:
  id: @BOSS@
rewards:
  enabled: true
  contribution-required: true
icon: @ICON@
waves:
@WAVES@
""";

    private static final String BOSS = """
id: @ID@
display-name: "@NAME@"
type: VANILLA
entity: @ENTITY@
mythic-id: ""
max-health: @HEALTH@
hp-per-player: 0.35
damage: 25
scale: @SCALE@
bar-color: @COLOR@
phases:
  - at: 100
    skills: [ground_slam]
  - at: 70
    skills: [summon_minions, energy_wave, shield]
    message: "&7@NAME@ &7gam len va trieu hoi dong bon!"
  - at: 35
    skills: [ground_slam, energy_wave, meteor_strike, summon_minions]
    enrage: true
skills:
  ground_slam: {cooldown: 8, radius: 6, damage: 12, warn: 40}
  energy_wave: {cooldown: 10, length: 12, damage: 10, warn: 30}
  meteor_strike: {cooldown: 12, radius: 4, damage: 14, warn: 40}
  dark_explosion: {cooldown: 20, radius: 9, damage: 18, warn: 50}
  summon_minions: {cooldown: 25, mob: @MINION@, amount: 4}
  shield: {cooldown: 35, duration: 8, reduction: 0.7}
""";

    private static final String REWARD = """
id: @ID@
money: @MONEY@
drops:
  - id: @CID@
    name: "@CNAME@"
    type: ITEM
    material: @CMAT@
    amount: 1
    chance: 15
    rarity: EPIC
    lore: ["&7Nguyen lieu dung de nang cap trang bi."]
    pity-step: 0.5
    pity-max: 20
  - id: @WID@
    name: "@WNAME@"
    type: ITEM
    material: @WMAT@
    amount: 1
    chance: 2
    rarity: LEGENDARY
    glow: true
    lore: ["&7Vu khi hiem tu Boss."]
    pity-step: 0.1
    pity-max: 5
  - id: @TID@
    name: "@TNAME@"
    type: COMMAND
    chance: 0.5
    rarity: MYTHIC
    commands:
      - "say {player} vua mo khoa danh hieu @TNAME@!"
""";

    /** Noi dung mac dinh cho duong dan nhu "dungeons/ancient_temple.yml"; null neu khong co. */
    public static String content(String path) {
        int slash = path.indexOf('/');
        if (slash < 0 || !path.endsWith(".yml")) return null;
        String dir = path.substring(0, slash);
        String id = path.substring(slash + 1, path.length() - 4);

        switch (dir) {
            case "dungeons":
                for (String[] r : DUNGEONS) {
                    if (!r[0].equals(id)) continue;
                    StringBuilder waves = new StringBuilder();
                    for (String wave : r[6].split(";")) {
                        StringBuilder mobs = new StringBuilder();
                        for (String m : wave.split(",")) {
                            if (mobs.length() > 0) mobs.append(", ");
                            mobs.append('"').append(m).append('"');
                        }
                        waves.append("  - mobs: [").append(mobs).append("]\n");
                    }
                    return DUNGEON.replace("@ID@", r[0]).replace("@NAME@", r[1]).replace("@WEIGHT@", r[2])
                            .replace("@BOSS@", r[3]).replace("@LEVEL@", r[4]).replace("@ICON@", r[5])
                            .replace("@WAVES@", waves.toString().stripTrailing());
                }
                return null;
            case "bosses":
                for (String[] r : BOSSES) {
                    if (!r[0].equals(id)) continue;
                    return BOSS.replace("@ID@", r[0]).replace("@NAME@", r[1]).replace("@ENTITY@", r[2])
                            .replace("@HEALTH@", r[3]).replace("@SCALE@", r[4]).replace("@COLOR@", r[5])
                            .replace("@MINION@", r[6]);
                }
                return null;
            case "rewards":
                for (String[] r : REWARDS) {
                    if (!r[0].equals(id)) continue;
                    return REWARD.replace("@ID@", r[0]).replace("@MONEY@", r[1]).replace("@CID@", r[2])
                            .replace("@CNAME@", r[3]).replace("@CMAT@", r[4]).replace("@WID@", r[5])
                            .replace("@WNAME@", r[6]).replace("@WMAT@", r[7]).replace("@TID@", r[8])
                            .replace("@TNAME@", r[9]);
                }
                return null;
            default:
                return null;
        }
    }
}
