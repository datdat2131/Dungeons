package com.astrax.dungeon.util;

import java.util.HashMap;
import java.util.Map;

/**
 * FILE TU DONG SINH TU CAC FILE .yml TRONG resources - dung khi jar thieu resource (plugin khong bao gio crash).
 * Khong sua tay file nay: sua cac file yml roi sinh lai.
 */
public final class DefaultFiles {
    private static final Map<String, String> M = new HashMap<>();

    private DefaultFiles() {}

    /** Noi dung mac dinh cho duong dan nhu "dungeons/ancient_temple.yml"; null neu khong co. */
    public static String content(String path) { return M.get(path); }

    static {
        M.put("bosses/ancient_guardian.yml", """
id: ancient_guardian
display-name: "&6Ancient Guardian"
type: VANILLA          # doi thanh MYTHICMOBS de dung boss tu MythicMobs
entity: IRON_GOLEM
mythic-id: ""          # ten mob trong MythicMobs (khi type: MYTHICMOBS)
max-health: 12000       # HP co ban cho 1 nguoi, nhan them he so cua tier va (1 + hp-per-player x (so nguoi - 1))
hp-per-player: 0.35
damage: 20          # sat thuong can chien
scale: 2.0
bar-color: YELLOW
# Giai doan: boss vao giai doan khi HP <= at (%). enrage: true = noi dien.
phases:
  - at: 100
    skills: [ground_slam]
  - at: 70
    skills: [ground_slam, summon_minions, energy_wave, shield]
    message: "&7Ancient Guardian &7gầm lên, đá cổ rung chuyển!"
  - at: 35
    skills: [ground_slam, energy_wave, meteor_strike, summon_minions]
    enrage: true
# Ky nang: type = blast | wave | meteor | summon | shield | lightning | pull | teleport
# Moi ky nang deu co canh bao (warn = tick) truoc khi gay sat thuong. effect = "LOAI:cap:giay".
skills:
  ground_slam: {type: blast, cooldown: 8, radius: 6, damage: 10, warn: 40, particle: FLAME}
  energy_wave: {type: wave, cooldown: 10, length: 14, damage: 9, warn: 30}
  meteor_strike: {type: meteor, cooldown: 12, radius: 4, damage: 12, warn: 40, count: 2, particle: FLAME}
  dark_explosion: {type: blast, cooldown: 20, radius: 9, damage: 14, warn: 50, particle: SOUL_FIRE_FLAME}
  poison_cloud: {type: blast, cooldown: 14, radius: 7, damage: 6, warn: 35, particle: WITCH, effect: "POISON:1:6"}
  frost_nova: {type: blast, cooldown: 15, radius: 8, damage: 8, warn: 40, particle: SNOWFLAKE, effect: "SLOWNESS:2:5"}
  wither_nova: {type: blast, cooldown: 15, radius: 8, damage: 8, warn: 40, particle: SOUL_FIRE_FLAME, effect: "WITHER:1:5"}
  lightning_storm: {type: lightning, cooldown: 18, strikes: 5, radius: 3, damage: 10, warn: 30}
  gravity_pull: {type: pull, cooldown: 22, radius: 14, strength: 1.3, damage: 4, warn: 40}
  blink: {type: teleport, cooldown: 14, radius: 5, damage: 12, warn: 20}
  summon_minions: {type: summon, cooldown: 25, mob: ZOMBIE, amount: 4, elite: false}
  shield: {type: shield, cooldown: 35, duration: 8, reduction: 0.7}
""");
        M.put("bosses/cursed_tree.yml", """
id: cursed_tree
display-name: "&2Cursed Tree"
type: VANILLA          # doi thanh MYTHICMOBS de dung boss tu MythicMobs
entity: RAVAGER
mythic-id: ""          # ten mob trong MythicMobs (khi type: MYTHICMOBS)
max-health: 14000       # HP co ban cho 1 nguoi, nhan them he so cua tier va (1 + hp-per-player x (so nguoi - 1))
hp-per-player: 0.35
damage: 22          # sat thuong can chien
scale: 1.6
bar-color: GREEN
# Giai doan: boss vao giai doan khi HP <= at (%). enrage: true = noi dien.
phases:
  - at: 100
    skills: [poison_cloud]
  - at: 65
    skills: [poison_cloud, summon_minions, shield, energy_wave]
    message: "&2Cursed Tree &2triệu hồi bầy nhện độc!"
  - at: 30
    skills: [poison_cloud, lightning_storm, summon_minions, meteor_strike]
    enrage: true
# Ky nang: type = blast | wave | meteor | summon | shield | lightning | pull | teleport
# Moi ky nang deu co canh bao (warn = tick) truoc khi gay sat thuong. effect = "LOAI:cap:giay".
skills:
  ground_slam: {type: blast, cooldown: 8, radius: 6, damage: 10, warn: 40, particle: FLAME}
  energy_wave: {type: wave, cooldown: 10, length: 14, damage: 9, warn: 30}
  meteor_strike: {type: meteor, cooldown: 12, radius: 4, damage: 12, warn: 40, count: 2, particle: FLAME}
  dark_explosion: {type: blast, cooldown: 20, radius: 9, damage: 14, warn: 50, particle: SOUL_FIRE_FLAME}
  poison_cloud: {type: blast, cooldown: 14, radius: 7, damage: 6, warn: 35, particle: WITCH, effect: "POISON:1:6"}
  frost_nova: {type: blast, cooldown: 15, radius: 8, damage: 8, warn: 40, particle: SNOWFLAKE, effect: "SLOWNESS:2:5"}
  wither_nova: {type: blast, cooldown: 15, radius: 8, damage: 8, warn: 40, particle: SOUL_FIRE_FLAME, effect: "WITHER:1:5"}
  lightning_storm: {type: lightning, cooldown: 18, strikes: 5, radius: 3, damage: 10, warn: 30}
  gravity_pull: {type: pull, cooldown: 22, radius: 14, strength: 1.3, damage: 4, warn: 40}
  blink: {type: teleport, cooldown: 14, radius: 5, damage: 12, warn: 20}
  summon_minions: {type: summon, cooldown: 25, mob: CAVE_SPIDER, amount: 5, elite: false, traits: [venom]}
  shield: {type: shield, cooldown: 35, duration: 8, reduction: 0.7}
""");
        M.put("bosses/frost_titan.yml", """
id: frost_titan
display-name: "&bFrost Titan"
type: VANILLA          # doi thanh MYTHICMOBS de dung boss tu MythicMobs
entity: RAVAGER
mythic-id: ""          # ten mob trong MythicMobs (khi type: MYTHICMOBS)
max-health: 135000       # HP co ban cho 1 nguoi, nhan them he so cua tier va (1 + hp-per-player x (so nguoi - 1))
hp-per-player: 0.35
damage: 48          # sat thuong can chien
scale: 3.2
bar-color: WHITE
# Giai doan: boss vao giai doan khi HP <= at (%). enrage: true = noi dien.
phases:
  - at: 100
    skills: [ground_slam, frost_nova]
  - at: 80
    skills: [frost_nova, summon_minions, energy_wave, shield]
    message: "&bFrost Titan &bphủ băng giá khắp đấu trường!"
  - at: 60
    skills: [gravity_pull, lightning_storm, frost_nova, blink]
  - at: 40
    skills: [dark_explosion, meteor_strike, frost_nova, summon_minions, gravity_pull]
  - at: 20
    skills: [dark_explosion, lightning_storm, frost_nova, blink, gravity_pull, meteor_strike]
    enrage: true
# Ky nang: type = blast | wave | meteor | summon | shield | lightning | pull | teleport
# Moi ky nang deu co canh bao (warn = tick) truoc khi gay sat thuong. effect = "LOAI:cap:giay".
skills:
  ground_slam: {type: blast, cooldown: 7, radius: 6, damage: 26, warn: 40, particle: FLAME}
  energy_wave: {type: wave, cooldown: 8, length: 14, damage: 23, warn: 30}
  meteor_strike: {type: meteor, cooldown: 10, radius: 4, damage: 31, warn: 40, count: 4, particle: FLAME}
  dark_explosion: {type: blast, cooldown: 17, radius: 9, damage: 36, warn: 50, particle: SOUL_FIRE_FLAME}
  poison_cloud: {type: blast, cooldown: 12, radius: 7, damage: 16, warn: 35, particle: WITCH, effect: "POISON:1:6"}
  frost_nova: {type: blast, cooldown: 13, radius: 8, damage: 21, warn: 40, particle: SNOWFLAKE, effect: "SLOWNESS:2:5"}
  wither_nova: {type: blast, cooldown: 13, radius: 8, damage: 21, warn: 40, particle: SOUL_FIRE_FLAME, effect: "WITHER:1:5"}
  lightning_storm: {type: lightning, cooldown: 15, strikes: 8, radius: 3, damage: 26, warn: 30}
  gravity_pull: {type: pull, cooldown: 19, radius: 14, strength: 1.3, damage: 10, warn: 40}
  blink: {type: teleport, cooldown: 12, radius: 5, damage: 31, warn: 20}
  summon_minions: {type: summon, cooldown: 21, mob: STRAY, amount: 6, elite: true, traits: [frost]}
  shield: {type: shield, cooldown: 30, duration: 8, reduction: 0.7}
""");
        M.put("bosses/infernal_king.yml", """
id: infernal_king
display-name: "&cInfernal King"
type: VANILLA          # doi thanh MYTHICMOBS de dung boss tu MythicMobs
entity: WITHER_SKELETON
mythic-id: ""          # ten mob trong MythicMobs (khi type: MYTHICMOBS)
max-health: 45000       # HP co ban cho 1 nguoi, nhan them he so cua tier va (1 + hp-per-player x (so nguoi - 1))
hp-per-player: 0.35
damage: 32          # sat thuong can chien
scale: 2.6
bar-color: RED
# Giai doan: boss vao giai doan khi HP <= at (%). enrage: true = noi dien.
phases:
  - at: 100
    skills: [ground_slam, wither_nova]
  - at: 75
    skills: [meteor_strike, summon_minions, energy_wave, ground_slam]
    message: "&cInfernal King &cgọi mưa thiên thạch lửa!"
  - at: 50
    skills: [dark_explosion, meteor_strike, shield, blink, wither_nova]
  - at: 25
    skills: [dark_explosion, meteor_strike, lightning_storm, blink, summon_minions, gravity_pull]
    enrage: true
# Ky nang: type = blast | wave | meteor | summon | shield | lightning | pull | teleport
# Moi ky nang deu co canh bao (warn = tick) truoc khi gay sat thuong. effect = "LOAI:cap:giay".
skills:
  ground_slam: {type: blast, cooldown: 7, radius: 6, damage: 17, warn: 40, particle: FLAME}
  energy_wave: {type: wave, cooldown: 9, length: 14, damage: 15, warn: 30}
  meteor_strike: {type: meteor, cooldown: 11, radius: 4, damage: 20, warn: 40, count: 3, particle: FLAME}
  dark_explosion: {type: blast, cooldown: 18, radius: 9, damage: 24, warn: 50, particle: SOUL_FIRE_FLAME}
  poison_cloud: {type: blast, cooldown: 13, radius: 7, damage: 10, warn: 35, particle: WITCH, effect: "POISON:1:6"}
  frost_nova: {type: blast, cooldown: 14, radius: 8, damage: 14, warn: 40, particle: SNOWFLAKE, effect: "SLOWNESS:2:5"}
  wither_nova: {type: blast, cooldown: 14, radius: 8, damage: 14, warn: 40, particle: SOUL_FIRE_FLAME, effect: "WITHER:1:5"}
  lightning_storm: {type: lightning, cooldown: 17, strikes: 6, radius: 3, damage: 17, warn: 30}
  gravity_pull: {type: pull, cooldown: 20, radius: 14, strength: 1.3, damage: 7, warn: 40}
  blink: {type: teleport, cooldown: 13, radius: 5, damage: 20, warn: 20}
  summon_minions: {type: summon, cooldown: 23, mob: BLAZE, amount: 5, elite: false, traits: [ignite]}
  shield: {type: shield, cooldown: 32, duration: 8, reduction: 0.7}
""");
        M.put("bosses/ocean_lord.yml", """
id: ocean_lord
display-name: "&bOcean Lord"
type: VANILLA          # doi thanh MYTHICMOBS de dung boss tu MythicMobs
entity: DROWNED
mythic-id: ""          # ten mob trong MythicMobs (khi type: MYTHICMOBS)
max-health: 50000       # HP co ban cho 1 nguoi, nhan them he so cua tier va (1 + hp-per-player x (so nguoi - 1))
hp-per-player: 0.35
damage: 30          # sat thuong can chien
scale: 2.6
bar-color: BLUE
# Giai doan: boss vao giai doan khi HP <= at (%). enrage: true = noi dien.
phases:
  - at: 100
    skills: [ground_slam, frost_nova]
  - at: 75
    skills: [gravity_pull, summon_minions, energy_wave, frost_nova]
    message: "&bOcean Lord &bkhuấy động đại dương!"
  - at: 50
    skills: [lightning_storm, frost_nova, shield, blink, gravity_pull]
  - at: 25
    skills: [lightning_storm, dark_explosion, gravity_pull, meteor_strike, frost_nova]
    enrage: true
# Ky nang: type = blast | wave | meteor | summon | shield | lightning | pull | teleport
# Moi ky nang deu co canh bao (warn = tick) truoc khi gay sat thuong. effect = "LOAI:cap:giay".
skills:
  ground_slam: {type: blast, cooldown: 7, radius: 6, damage: 17, warn: 40, particle: FLAME}
  energy_wave: {type: wave, cooldown: 9, length: 14, damage: 15, warn: 30}
  meteor_strike: {type: meteor, cooldown: 11, radius: 4, damage: 20, warn: 40, count: 3, particle: FLAME}
  dark_explosion: {type: blast, cooldown: 18, radius: 9, damage: 24, warn: 50, particle: SOUL_FIRE_FLAME}
  poison_cloud: {type: blast, cooldown: 13, radius: 7, damage: 10, warn: 35, particle: WITCH, effect: "POISON:1:6"}
  frost_nova: {type: blast, cooldown: 14, radius: 8, damage: 14, warn: 40, particle: SNOWFLAKE, effect: "SLOWNESS:2:5"}
  wither_nova: {type: blast, cooldown: 14, radius: 8, damage: 14, warn: 40, particle: SOUL_FIRE_FLAME, effect: "WITHER:1:5"}
  lightning_storm: {type: lightning, cooldown: 17, strikes: 6, radius: 3, damage: 17, warn: 30}
  gravity_pull: {type: pull, cooldown: 20, radius: 14, strength: 1.3, damage: 7, warn: 40}
  blink: {type: teleport, cooldown: 13, radius: 5, damage: 20, warn: 20}
  summon_minions: {type: summon, cooldown: 23, mob: DROWNED, amount: 6, elite: false, traits: [frost]}
  shield: {type: shield, cooldown: 32, duration: 8, reduction: 0.7}
""");
        M.put("bosses/pharaoh.yml", """
id: pharaoh
display-name: "&ePharaoh"
type: VANILLA          # doi thanh MYTHICMOBS de dung boss tu MythicMobs
entity: HUSK
mythic-id: ""          # ten mob trong MythicMobs (khi type: MYTHICMOBS)
max-health: 120000       # HP co ban cho 1 nguoi, nhan them he so cua tier va (1 + hp-per-player x (so nguoi - 1))
hp-per-player: 0.35
damage: 45          # sat thuong can chien
scale: 3.0
bar-color: YELLOW
# Giai doan: boss vao giai doan khi HP <= at (%). enrage: true = noi dien.
phases:
  - at: 100
    skills: [ground_slam, poison_cloud]
  - at: 80
    skills: [summon_minions, energy_wave, meteor_strike, poison_cloud]
    message: "&ePharaoh &eđánh thức đội quân xác ướp!"
  - at: 60
    skills: [gravity_pull, shield, blink, lightning_storm, ground_slam]
  - at: 40
    skills: [dark_explosion, meteor_strike, summon_minions, gravity_pull, poison_cloud]
  - at: 20
    skills: [dark_explosion, meteor_strike, lightning_storm, blink, gravity_pull, summon_minions]
    enrage: true
# Ky nang: type = blast | wave | meteor | summon | shield | lightning | pull | teleport
# Moi ky nang deu co canh bao (warn = tick) truoc khi gay sat thuong. effect = "LOAI:cap:giay".
skills:
  ground_slam: {type: blast, cooldown: 7, radius: 6, damage: 26, warn: 40, particle: FLAME}
  energy_wave: {type: wave, cooldown: 8, length: 14, damage: 23, warn: 30}
  meteor_strike: {type: meteor, cooldown: 10, radius: 4, damage: 31, warn: 40, count: 4, particle: FLAME}
  dark_explosion: {type: blast, cooldown: 17, radius: 9, damage: 36, warn: 50, particle: SOUL_FIRE_FLAME}
  poison_cloud: {type: blast, cooldown: 12, radius: 7, damage: 16, warn: 35, particle: WITCH, effect: "POISON:1:6"}
  frost_nova: {type: blast, cooldown: 13, radius: 8, damage: 21, warn: 40, particle: SNOWFLAKE, effect: "SLOWNESS:2:5"}
  wither_nova: {type: blast, cooldown: 13, radius: 8, damage: 21, warn: 40, particle: SOUL_FIRE_FLAME, effect: "WITHER:1:5"}
  lightning_storm: {type: lightning, cooldown: 15, strikes: 8, radius: 3, damage: 26, warn: 30}
  gravity_pull: {type: pull, cooldown: 19, radius: 14, strength: 1.3, damage: 10, warn: 40}
  blink: {type: teleport, cooldown: 12, radius: 5, damage: 31, warn: 20}
  summon_minions: {type: summon, cooldown: 21, mob: HUSK, amount: 6, elite: true, traits: [venom]}
  shield: {type: shield, cooldown: 30, duration: 8, reduction: 0.7}
""");
        M.put("bosses/void_emperor.yml", """
id: void_emperor
display-name: "&5Void Emperor"
type: VANILLA          # doi thanh MYTHICMOBS de dung boss tu MythicMobs
entity: ENDERMAN
mythic-id: ""          # ten mob trong MythicMobs (khi type: MYTHICMOBS)
max-health: 400000       # HP co ban cho 1 nguoi, nhan them he so cua tier va (1 + hp-per-player x (so nguoi - 1))
hp-per-player: 0.35
damage: 70          # sat thuong can chien
scale: 3.5
bar-color: PURPLE
# Giai doan: boss vao giai doan khi HP <= at (%). enrage: true = noi dien.
phases:
  - at: 100
    skills: [ground_slam, dark_explosion]
  - at: 85
    skills: [blink, energy_wave, meteor_strike, summon_minions]
    message: "&5Void Emperor &5xé toạc không gian!"
  - at: 65
    skills: [gravity_pull, lightning_storm, shield, blink, dark_explosion]
  - at: 45
    skills: [meteor_strike, dark_explosion, summon_minions, gravity_pull, lightning_storm, wither_nova]
  - at: 25
    skills: [meteor_strike, dark_explosion, lightning_storm, blink, gravity_pull, wither_nova, summon_minions, energy_wave]
    message: "&5&lVoid Emperor &5triệu hồi hư vô tuyệt đối!"
  - at: 10
    skills: [meteor_strike, dark_explosion, lightning_storm, blink, gravity_pull, wither_nova, summon_minions, energy_wave, frost_nova]
    enrage: true
# Ky nang: type = blast | wave | meteor | summon | shield | lightning | pull | teleport
# Moi ky nang deu co canh bao (warn = tick) truoc khi gay sat thuong. effect = "LOAI:cap:giay".
skills:
  ground_slam: {type: blast, cooldown: 6, radius: 6, damage: 40, warn: 40, particle: FLAME}
  energy_wave: {type: wave, cooldown: 8, length: 14, damage: 36, warn: 30}
  meteor_strike: {type: meteor, cooldown: 9, radius: 4, damage: 48, warn: 40, count: 5, particle: FLAME}
  dark_explosion: {type: blast, cooldown: 16, radius: 9, damage: 56, warn: 50, particle: SOUL_FIRE_FLAME}
  poison_cloud: {type: blast, cooldown: 11, radius: 7, damage: 24, warn: 35, particle: WITCH, effect: "POISON:1:6"}
  frost_nova: {type: blast, cooldown: 12, radius: 8, damage: 32, warn: 40, particle: SNOWFLAKE, effect: "SLOWNESS:2:5"}
  wither_nova: {type: blast, cooldown: 12, radius: 8, damage: 32, warn: 40, particle: SOUL_FIRE_FLAME, effect: "WITHER:1:5"}
  lightning_storm: {type: lightning, cooldown: 14, strikes: 10, radius: 3, damage: 40, warn: 30}
  gravity_pull: {type: pull, cooldown: 17, radius: 14, strength: 1.3, damage: 16, warn: 40}
  blink: {type: teleport, cooldown: 11, radius: 5, damage: 48, warn: 20}
  summon_minions: {type: summon, cooldown: 20, mob: VEX, amount: 8, elite: true, traits: [swift, vampire]}
  shield: {type: shield, cooldown: 27, duration: 8, reduction: 0.8}
""");
        M.put("config.yml", """
dungeon:
  join-window-seconds: 120   # thời gian đầu để vào (WAITING)
  late-join: true            # cho vào muộn khi đang chạy
  max-duration-minutes: 40   # quá giờ sẽ bị cưỡng chế kết thúc
  block-edit: false          # người trong Dungeon được phá/đặt block?
# lobby được lưu bằng /dungeon admin setlobby
  keep-inventory: true       # chet trong Dungeon khong mat do
  reward-delay-seconds: 30   # sau khi Boss chet, doi bao lau roi dong Dungeon
mobs:
  cap: 40                    # so quai toi da cung luc
  spawn-radius: 14           # quai spawn quanh nguoi choi trong ban kinh nay
  elite-chance: 0.05         # ty le quai Tinh Anh (x3 mau, diem cao hon)
  hp-per-player: 0.1         # +10% mau quai moi nguoi them
  first-wave-delay-seconds: 5   # nghi truoc dot 1
  wave-delay-seconds: 10        # nghi giua cac dot (va truoc Boss)
  wave-clear-points: 20         # diem dong gop khi clear 1 dot
  heal-between-waves: true      # hoi day mau + thuc an sau moi dot
database:
  type: sqlite               # sqlite | mysql
  mysql:
    host: localhost
    port: 3306
    database: astrax
    user: root
    password: ""
reward:
  min-points: 30             # diem dong gop toi thieu de nhan thuong chinh
  afk-seconds: 120           # khong danh quai/boss trong ngan nay => coi la AFK
  money-command: ""          # dung khi khong co Vault, vd: eco give {player} {amount}
  broadcast-min-rarity: LEGENDARY
party:
  max-size: 10
  invite-seconds: 60
revive:
  enabled: true              # het mang nhung con dong doi song => guc cho cuu (thay vi bi loai ngay)
  seconds: 5                 # so giay dong doi phai GIU SHIFT gan de cuu
  timeout-seconds: 60        # qua thoi gian nay khong ai cuu => bi loai
  lives-on-revive: 1         # so mang sau khi duoc cuu
quests:
  streak-bonus-per-day: 0.02 # chuoi ngay tham gia: +2% ty le roi moi ngay lien tiep
  streak-max-bonus: 0.20     # toi da +20%
discord:
  webhook-url: ""            # dan link webhook Discord vao day de bat thong bao
  notify-open: true          # bao khi Dungeon mo
  notify-rare: true          # bao khi co nguoi nhan do hiem
""");
        M.put("dungeons/abyssal_temple.yml", """
id: abyssal_temple
enabled: true
display-name: "&bAbyssal Temple"
tier: EPIC      # COMMON | EPIC | LEGENDARY | MYTHICAL (chi tiet o tiers.yml)
weight: 12           # ty le duoc chon khi mo Dungeon moi gio
max-players: 30
min-players: 1
recommended-level: "5-15"
world: dungeon_world
icon: PRISMARINE
boss:
  id: ocean_lord
rewards:
  enabled: true
  contribution-required: true
# Quai moi dot chon ngau nhien theo weight. min-wave = dot som nhat xuat hien.
# traits: tank brute swift vampire venom frost ignite wither thorns explosive shielded
mob-pool:
  - {type: DROWNED, weight: 10, min-wave: 1}
  - {type: STRAY, weight: 6, min-wave: 1, traits: [frost]}
  - {type: SILVERFISH, weight: 6, min-wave: 1, traits: [venom]}
  - {type: WITCH, weight: 4, min-wave: 2}
  - {type: VEX, weight: 5, min-wave: 3, traits: [swift]}
  - {type: VINDICATOR, weight: 4, min-wave: 3, traits: [brute, vampire]}
# Mini-Boss o dot cuoi (so dot lay tu tier)
miniboss:
  type: DROWNED
  name: "&bTide Warden"
  hp-mult: 12
  traits: [tank, vampire]
# (tuy chon) ghi de so quai moi dot cua tier:
# wave-scaling:
#   amount-base: 6
#   amount-growth: 1.2
""");
        M.put("dungeons/ancient_temple.yml", """
id: ancient_temple
enabled: true
display-name: "&6Ancient Temple"
tier: COMMON      # COMMON | EPIC | LEGENDARY | MYTHICAL (chi tiet o tiers.yml)
weight: 20           # ty le duoc chon khi mo Dungeon moi gio
max-players: 30
min-players: 1
recommended-level: "1-10"
world: dungeon_world
icon: STONE_BRICKS
boss:
  id: ancient_guardian
rewards:
  enabled: true
  contribution-required: true
# Quai moi dot chon ngau nhien theo weight. min-wave = dot som nhat xuat hien.
# traits: tank brute swift vampire venom frost ignite wither thorns explosive shielded
mob-pool:
  - {type: ZOMBIE, weight: 10, min-wave: 1}
  - {type: SKELETON, weight: 8, min-wave: 1, traits: [swift]}
  - {type: HUSK, weight: 6, min-wave: 2}
  - {type: SPIDER, weight: 6, min-wave: 2}
  - {type: STRAY, weight: 4, min-wave: 3, traits: [frost]}
  - {type: VINDICATOR, weight: 3, min-wave: 4, traits: [brute]}
# Mini-Boss o dot cuoi (so dot lay tu tier)
miniboss:
  type: VINDICATOR
  name: "&6Temple Warden"
  hp-mult: 6
  traits: [brute, tank]
# (tuy chon) ghi de so quai moi dot cua tier:
# wave-scaling:
#   amount-base: 6
#   amount-growth: 1.2
""");
        M.put("dungeons/cursed_forest.yml", """
id: cursed_forest
enabled: true
display-name: "&2Cursed Forest"
tier: COMMON      # COMMON | EPIC | LEGENDARY | MYTHICAL (chi tiet o tiers.yml)
weight: 20           # ty le duoc chon khi mo Dungeon moi gio
max-players: 30
min-players: 1
recommended-level: "1-10"
world: dungeon_world
icon: DARK_OAK_LOG
boss:
  id: cursed_tree
rewards:
  enabled: true
  contribution-required: true
# Quai moi dot chon ngau nhien theo weight. min-wave = dot som nhat xuat hien.
# traits: tank brute swift vampire venom frost ignite wither thorns explosive shielded
mob-pool:
  - {type: SPIDER, weight: 10, min-wave: 1}
  - {type: CAVE_SPIDER, weight: 8, min-wave: 1, traits: [venom]}
  - {type: ZOMBIE, weight: 7, min-wave: 1, traits: [tank]}
  - {type: SKELETON, weight: 4, min-wave: 2, traits: [venom]}
  - {type: WITCH, weight: 4, min-wave: 3}
  - {type: VINDICATOR, weight: 3, min-wave: 3}
# Mini-Boss o dot cuoi (so dot lay tu tier)
miniboss:
  type: RAVAGER
  name: "&2Elder Treant"
  hp-mult: 5
  traits: [tank, brute]
# (tuy chon) ghi de so quai moi dot cua tier:
# wave-scaling:
#   amount-base: 6
#   amount-growth: 1.2
""");
        M.put("dungeons/frozen_citadel.yml", """
id: frozen_citadel
enabled: true
display-name: "&bFrozen Citadel"
tier: LEGENDARY      # COMMON | EPIC | LEGENDARY | MYTHICAL (chi tiet o tiers.yml)
weight: 6           # ty le duoc chon khi mo Dungeon moi gio
max-players: 25
min-players: 1
recommended-level: "10-20"
world: dungeon_world
icon: PACKED_ICE
boss:
  id: frost_titan
rewards:
  enabled: true
  contribution-required: true
# Quai moi dot chon ngau nhien theo weight. min-wave = dot som nhat xuat hien.
# traits: tank brute swift vampire venom frost ignite wither thorns explosive shielded
mob-pool:
  - {type: STRAY, weight: 10, min-wave: 1, traits: [frost]}
  - {type: ZOMBIE, weight: 8, min-wave: 1, traits: [thorns, tank]}
  - {type: SKELETON, weight: 6, min-wave: 1, traits: [frost, swift]}
  - {type: WITCH, weight: 4, min-wave: 2}
  - {type: VINDICATOR, weight: 5, min-wave: 3, traits: [brute]}
  - {type: EVOKER, weight: 3, min-wave: 6}
# Mini-Boss o dot cuoi (so dot lay tu tier)
miniboss:
  type: RAVAGER
  name: "&bGlacier Beast"
  hp-mult: 14
  traits: [frost, tank, thorns]
# (tuy chon) ghi de so quai moi dot cua tier:
# wave-scaling:
#   amount-base: 6
#   amount-growth: 1.2
""");
        M.put("dungeons/infernal_fortress.yml", """
id: infernal_fortress
enabled: true
display-name: "&cInfernal Fortress"
tier: EPIC      # COMMON | EPIC | LEGENDARY | MYTHICAL (chi tiet o tiers.yml)
weight: 12           # ty le duoc chon khi mo Dungeon moi gio
max-players: 30
min-players: 1
recommended-level: "5-15"
world: dungeon_world
icon: NETHER_BRICKS
boss:
  id: infernal_king
rewards:
  enabled: true
  contribution-required: true
# Quai moi dot chon ngau nhien theo weight. min-wave = dot som nhat xuat hien.
# traits: tank brute swift vampire venom frost ignite wither thorns explosive shielded
mob-pool:
  - {type: ZOMBIFIED_PIGLIN, weight: 10, min-wave: 1, traits: [ignite]}
  - {type: ZOMBIE, weight: 6, min-wave: 1, traits: [ignite]}
  - {type: SKELETON, weight: 6, min-wave: 1, traits: [ignite]}
  - {type: WITHER_SKELETON, weight: 8, min-wave: 2, traits: [wither]}
  - {type: BLAZE, weight: 6, min-wave: 2, traits: [ignite]}
  - {type: VINDICATOR, weight: 5, min-wave: 4, traits: [brute]}
# Mini-Boss o dot cuoi (so dot lay tu tier)
miniboss:
  type: WITHER_SKELETON
  name: "&cFortress Overseer"
  hp-mult: 10
  traits: [brute, ignite, tank, wither]
# (tuy chon) ghi de so quai moi dot cua tier:
# wave-scaling:
#   amount-base: 6
#   amount-growth: 1.2
""");
        M.put("dungeons/lost_pyramid.yml", """
id: lost_pyramid
enabled: true
display-name: "&eLost Pyramid"
tier: LEGENDARY      # COMMON | EPIC | LEGENDARY | MYTHICAL (chi tiet o tiers.yml)
weight: 6           # ty le duoc chon khi mo Dungeon moi gio
max-players: 25
min-players: 1
recommended-level: "10-20"
world: dungeon_world
icon: SANDSTONE
boss:
  id: pharaoh
rewards:
  enabled: true
  contribution-required: true
# Quai moi dot chon ngau nhien theo weight. min-wave = dot som nhat xuat hien.
# traits: tank brute swift vampire venom frost ignite wither thorns explosive shielded
mob-pool:
  - {type: HUSK, weight: 10, min-wave: 1, traits: [venom]}
  - {type: SKELETON, weight: 8, min-wave: 1, traits: [swift]}
  - {type: SPIDER, weight: 6, min-wave: 1}
  - {type: CAVE_SPIDER, weight: 6, min-wave: 2, traits: [venom]}
  - {type: WITHER_SKELETON, weight: 5, min-wave: 3, traits: [wither]}
  - {type: VINDICATOR, weight: 5, min-wave: 4, traits: [brute, thorns]}
  - {type: EVOKER, weight: 2, min-wave: 6}
# Mini-Boss o dot cuoi (so dot lay tu tier)
miniboss:
  type: HUSK
  name: "&eMummy Lord"
  hp-mult: 14
  traits: [tank, brute, venom]
# (tuy chon) ghi de so quai moi dot cua tier:
# wave-scaling:
#   amount-base: 6
#   amount-growth: 1.2
""");
        M.put("dungeons/void_dimension.yml", """
id: void_dimension
enabled: true
display-name: "&5Void Dimension"
tier: MYTHICAL      # COMMON | EPIC | LEGENDARY | MYTHICAL (chi tiet o tiers.yml)
weight: 3           # ty le duoc chon khi mo Dungeon moi gio
max-players: 20
min-players: 1
recommended-level: "15-30"
world: dungeon_world
icon: END_STONE
boss:
  id: void_emperor
rewards:
  enabled: true
  contribution-required: true
# Quai moi dot chon ngau nhien theo weight. min-wave = dot som nhat xuat hien.
# traits: tank brute swift vampire venom frost ignite wither thorns explosive shielded
mob-pool:
  - {type: ENDERMAN, weight: 10, min-wave: 1, traits: [tank]}
  - {type: ENDERMITE, weight: 8, min-wave: 1, traits: [swift, venom]}
  - {type: VINDICATOR, weight: 5, min-wave: 1, traits: [brute, vampire]}
  - {type: WITHER_SKELETON, weight: 6, min-wave: 2, traits: [wither, brute]}
  - {type: VEX, weight: 6, min-wave: 2, traits: [swift, vampire]}
  - {type: EVOKER, weight: 4, min-wave: 3}
# Mini-Boss o dot cuoi (so dot lay tu tier)
miniboss:
  type: ENDERMAN
  name: "&5Void Walker"
  hp-mult: 16
  traits: [tank, vampire, swift, explosive]
# (tuy chon) ghi de so quai moi dot cua tier:
# wave-scaling:
#   amount-base: 6
#   amount-growth: 1.2
""");
        M.put("messages.yml", """
prefix: "&8[&6AstraX&8] &r"
countdown: "&e{dungeon} &7sẽ mở sau &c{minutes} phút&7! Chuẩn bị trang bị!"
bossbar: "&6{dungeon} &7mở sau &e{time}"
opened: "&a{dungeon} &7đã mở! Gõ &e/dungeon join &7(cổng chính mở &c{seconds}s&7, vào muộn vẫn được)."
modifier: "&dBiến thể lượt này: &f{name} &7- {desc}"
golden: "&6✦ GIỜ VÀNG ✦ &7x{contrib} điểm đóng góp, x{drop} tỷ lệ rơi!"
golden-soon: "&6✦ Lượt sắp tới rơi vào GIỜ VÀNG! ✦"
started: "&cDungeon bắt đầu! Hãy chiến đấu!"
time-up: "&cHết giờ! Dungeon bị đóng, mọi người được đưa về sảnh."
force-end-new: "&cDungeon cũ bị đóng để mở lượt mới."
stopped: "&cDungeon đã bị quản trị viên dừng."
joined: "&aBạn đã vào &e{dungeon}&a."
left: "&7Bạn đã rời Dungeon."
no-active: "&cHiện không có Dungeon nào đang mở. Gõ &e/dungeon next&c."
already-in: "&cBạn đã ở trong Dungeon."
not-in: "&cBạn không ở trong Dungeon."
full: "&cDungeon đã đầy."
closed-late: "&cDungeon đang chạy và không cho vào muộn."
no-spawn: "&cDungeon chưa có điểm spawn. Báo admin dùng /dungeon admin setspawn."
next: "&7Dungeon kế tiếp: &e{dungeon} &7- mở sau &f{time}"
current: "&7Đang mở: &a{dungeon} &7({state}) - &f{count} người"
no-perm: "&cBạn không có quyền."
unknown-dungeon: "&cKhông tìm thấy Dungeon '{id}'."
reloaded: "&aĐã reload cấu hình."
spawn-set: "&aĐã đặt spawn cho {id}."
lobby-set: "&aĐã đặt lobby."
block-denied: "&cKhông thể phá/đặt block trong Dungeon."
modifiers:
  FAST_BOSS: { name: "Boss Thần Tốc", desc: "Boss tăng 25% tốc độ" }
  EXPLOSIVE_MOBS: { name: "Quái Nổ Tung", desc: "Quái phát nổ khi chết" }
  NO_REGEN: { name: "Vết Thương Sâu", desc: "Không hồi máu tự nhiên" }
  DARKNESS: { name: "Bóng Tối", desc: "Tầm nhìn bị hạn chế" }
  GLASS_CANNON: { name: "Pháo Thủy Tinh", desc: "Sát thương x1.5 nhưng máu giảm" }
  DOUBLE_LOOT: { name: "Kho Báu Đôi", desc: "Tỷ lệ rơi đồ x2" }
wave: "&eĐợt &6{n}&e/&6{total} &ebắt đầu! Tiêu diệt toàn bộ quái để sang đợt sau!"
boss-spawn: "&c&l⚔ {boss} &cđã xuất hiện! Cả đội cùng tấn công!"
boss-phase: "&e{boss} &7chuyển sang &cGiai đoạn {n}&7!"
boss-enrage: "&4&l{boss} NỔI ĐIÊN! &cSát thương và tốc độ tăng mạnh!"
boss-shield: "&b{boss} dựng lá chắn! Sát thương giảm trong {sec}s."
boss-shield-down: "&a Lá chắn của {boss} &ađã vỡ!"
boss-dead: "&a&l{boss} &ađã bị tiêu diệt!"
top-damage: "&6#{rank} &f{name} &7- &c{damage} sát thương"
completed: "&aDungeon hoàn thành! Mọi người sẽ được đưa về sảnh."
boss-missing: "&cKhông tìm thấy cấu hình Boss '{id}'."
boss-no-active: "&cKhông thể gọi Boss lúc này (chưa có Dungeon hoặc Boss đã xuất hiện)."
bossspawn-set: "&aĐã đặt vị trí Boss cho {id}."
elite-name: "&6★ Tinh Anh"
wave-bar: "&6Đợt {n}/{total} &7- còn &c{left} &7quái"
wave-wait: "&eĐợt &6{n}&e/&6{total} &ebắt đầu sau &6{sec}s"
boss-wait: "&c&lBoss xuất hiện sau &6{sec}s&c! Hồi sức đi!"
wave-title: "&6&lĐỢT {n}/{total}"
wave-subtitle: "&7{count} quái - hạ hết để qua đợt"
wave-clear: "&aĐã dọn xong đợt &e{n}&a! &7+{points} điểm đóng góp, máu đã hồi đầy."
reward-header: "&6&l═══ PHẦN THƯỞNG ═══"
reward-money: "&7➤ Tiền: &a+{money}"
reward-item: "&7➤ {item} &7x{amount}"
reward-party-bonus: "&d✦ Party bonus: +{bonus}% tỷ lệ rơi"
reward-ineligible: "&cBạn không đủ điều kiện nhận thưởng chính (AFK hoặc đóng góp quá thấp)."
reward-broadcast: "&6★ &f{player} &7vừa nhận {item} &7từ &e{dungeon}&7!"
rewards-header: "&6Phần thưởng &e{dungeon}"
rewards-money: "&7- Tiền cơ bản: &a{money} &8(thay đổi theo đóng góp)"
rewards-line: "&7- {item} &8({chance}%)"
stats: "&6Thống kê: &7Hoàn thành &f{c} &7| Boss hạ &f{b} &7| Điểm &f{p} &7| Sát thương &f{d} &7| Nhanh nhất &f{t}"
stats-none: "&7Bạn chưa hoàn thành Dungeon nào."
player-offline: "&cKhông tìm thấy người chơi đó (hoặc không hợp lệ)."
party-created: "&aĐã tạo Party! Mời bạn bè bằng &e/dungeon party invite <tên>"
party-exists: "&cBạn đã ở trong một Party."
party-none: "&cBạn chưa có Party."
party-leader-only: "&cChỉ đội trưởng mới làm được."
party-target-in-party: "&c{name} đã ở trong Party khác."
party-full: "&cParty đã đầy."
party-invite-sent: "&aĐã mời &e{name}&a."
party-invited: "&e{name} &7mời bạn vào Party. &a&l[BẤM ĐỂ CHẤP NHẬN]"
party-no-invite: "&cKhông có lời mời hợp lệ."
party-joined: "&e{name} &7đã vào Party."
party-left: "&e{name} &7đã rời Party."
party-you-left: "&7Bạn đã rời Party."
party-kicked: "&cBạn đã bị đuổi khỏi Party."
party-promoted: "&e{name} &7là đội trưởng mới."
party-info-header: "&6Party &7({count}/{max}) - Đội trưởng: &e{leader}"
party-info-member: "&7 - &f{name}{tag}"
tier-line: "&7Cấp độ: {tier} &7| &f{waves} đợt &7| Mạng: &f{lives}"
tiers-line: "{tier} &7- &f{waves} đợt &7| mạng &f{lives} &7| cấp &f{level} &7| quái x{mob} &7| tối đa &f{minutes}p"
level-too-low: "&cCần cấp Dungeon &e{need} &cđể vào (bạn đang cấp &e{have}&c). Kiếm thêm điểm đóng góp!"
eliminated: "&4&lBạn đã hết mạng! &cBạn bị loại khỏi Dungeon này."
eliminated-block: "&cBạn đã hết mạng ở lượt này nên không thể vào lại."
lives-left: "&cBạn còn &e{lives} &cmạng."
wave-miniboss: "&c&l☠ MINI-BOSS xuất hiện! Hạ nó để kết thúc đợt!"
wave-kind-NORMAL: "&7{count} quái - hạ hết để qua đợt"
wave-kind-ELITE: "&6★ ĐỢT TINH ANH &7- {count} quái mạnh!"
wave-kind-SWARM: "&e⚡ ĐỢT BẦY ĐÀN &7- {count} quái nhanh, máu thấp!"
wave-kind-MINIBOSS: "&c☠ MINI-BOSS &7cùng {count} quái!"
boss-enrage-timer: "&4&l{boss} NỔI ĐIÊN vì đánh quá lâu! &cSát thương và tốc độ tăng mạnh!"
reward-mvp: "&d★ Bạn là MVP sát thương! &7+25% tỷ lệ rơi."
traits:
  tank: "Giáp Dày"
  brute: "Man Lực"
  swift: "Thần Tốc"
  vampire: "Hút Máu"
  venom: "Độc Dược"
  frost: "Băng Giá"
  ignite: "Lửa Cháy"
  wither: "Héo Úa"
  thorns: "Gai Phản"
  explosive: "Nổ Tung"
  shielded: "Khiên Hộ Mệnh"
mail-claimed: "&aĐã nhận &e{count} &aphần thưởng từ hộp thư!"
mail-empty: "&7Hộp thư thưởng của bạn đang trống."
mail-error: "&cKhông đọc được hộp thư, thử lại sau."
mail-pending: "&6Bạn có &e{count} &6phần thưởng chưa nhận! Gõ &e/dungeon claim"
shop-bought: "&aĐã mua {item}&a!"
shop-not-enough: "&cBạn không đủ Token."
shop-limit: "&cBạn đã mua đủ {limit} lần hôm nay."
shop-error: "&cLỗi cửa hàng, thử lại sau."
quest-complete: "&a✔ Hoàn thành nhiệm vụ: &f{name} &7(mở /dungeon quests để nhận thưởng)"
quest-already: "&7Bạn đã nhận phần thưởng nhiệm vụ này."
quest-incomplete: "&cNhiệm vụ chưa hoàn thành."
quest-claimed: "&aĐã nhận thưởng nhiệm vụ &f{name}&a!"
streak-up: "&6🔥 Chuỗi tham gia: &e{days} ngày &7(+{bonus}% tỷ lệ rơi)"
reward-streak: "&6🔥 Streak {days} ngày: &a+{bonus}% tỷ lệ rơi"
downed: "&4&lBạn đã gục! &cĐồng đội giữ SHIFT gần bạn để cứu."
downed-team: "&c{name} &7đã gục! Chạy tới và &eGIỮ SHIFT &7để cứu!"
downed-title: "&4&lBẠN ĐÃ GỤC"
downed-sub: "&7Chờ đồng đội đến cứu..."
downed-wait: "&cChờ cứu... còn &f{sec}s"
reviving: "&aĐang cứu &f{name}&a... &e{pct}%"
revived: "&a&lBạn đã được cứu sống!"
revive-done: "&a{reviver} &7đã cứu sống &a{name}&7!"
""");
        M.put("quests.yml", """
# Nhiem vu ngay/tuan (/dungeon quests). Moi ngay/tuan chon ngau nhien daily-count / weekly-count nhiem vu
# (giong nhau cho moi nguoi). Loai (type):
#   KILL_MOBS  KILL_ELITES  BOSS_DAMAGE  CLEAR_DUNGEON  CLEAR_TIER (param: COMMON/EPIC/LEGENDARY/MYTHICAL)
#   WAVES  JOIN_DUNGEON  REVIVE  NO_DEATH_CLEAR (clear Dungeon ma khong chet lan nao)
# reward.items dung dinh dang giong rewards/*.yml; id token phai la common_token/epic_token/legendary_token/mythical_token.
daily-count: 3
weekly-count: 3
daily:
  - id: d_kill_100
    name: "&7Săn 100 quái trong Dungeon"
    type: KILL_MOBS
    amount: 100
    reward:
      money: 500
      items:
        - {id: common_token, name: "Common Dungeon Token", material: GOLD_NUGGET, amount: 2, rarity: RARE}
  - id: d_kill_elites
    name: "&6Hạ 10 quái Tinh Anh"
    type: KILL_ELITES
    amount: 10
    reward:
      money: 800
      items:
        - {id: common_token, name: "Common Dungeon Token", material: GOLD_NUGGET, amount: 3, rarity: RARE}
  - id: d_clear_1
    name: "&aHoàn thành 1 Dungeon"
    type: CLEAR_DUNGEON
    amount: 1
    reward:
      money: 1000
      items:
        - {id: common_token, name: "Common Dungeon Token", material: GOLD_NUGGET, amount: 2, rarity: RARE}
  - id: d_join_2
    name: "&bTham gia 2 lượt Dungeon"
    type: JOIN_DUNGEON
    amount: 2
    reward:
      money: 600
      items:
        - {id: common_token, name: "Common Dungeon Token", material: GOLD_NUGGET, amount: 1, rarity: RARE}
  - id: d_waves_20
    name: "&eClear 20 đợt wave"
    type: WAVES
    amount: 20
    reward:
      items:
        - {id: epic_token, name: "Epic Dungeon Token", material: EMERALD, amount: 1, rarity: EPIC}
  - id: d_revive_1
    name: "&dCứu sống 1 đồng đội"
    type: REVIVE
    amount: 1
    reward:
      money: 500
      items:
        - {id: common_token, name: "Common Dungeon Token", material: GOLD_NUGGET, amount: 2, rarity: RARE}
  - id: d_boss_dmg
    name: "&cGây 30.000 sát thương lên Boss"
    type: BOSS_DAMAGE
    amount: 30000
    reward:
      money: 800
      items:
        - {id: common_token, name: "Common Dungeon Token", material: GOLD_NUGGET, amount: 1, rarity: RARE}
weekly:
  - id: w_kill_1000
    name: "&7Săn 1.000 quái"
    type: KILL_MOBS
    amount: 1000
    reward:
      money: 3000
      items:
        - {id: epic_token, name: "Epic Dungeon Token", material: EMERALD, amount: 3, rarity: EPIC}
  - id: w_clear_5
    name: "&aHoàn thành 5 Dungeon"
    type: CLEAR_DUNGEON
    amount: 5
    reward:
      money: 5000
      items:
        - {id: epic_token, name: "Epic Dungeon Token", material: EMERALD, amount: 4, rarity: EPIC}
  - id: w_clear_epic_2
    name: "&5Hoàn thành 2 Dungeon Epic"
    type: CLEAR_TIER
    param: EPIC
    amount: 2
    reward:
      money: 8000
      items:
        - {id: epic_token, name: "Epic Dungeon Token", material: EMERALD, amount: 3, rarity: EPIC}
  - id: w_clear_legendary
    name: "&6Hoàn thành 1 Dungeon Legendary"
    type: CLEAR_TIER
    param: LEGENDARY
    amount: 1
    reward:
      money: 20000
      items:
        - {id: legendary_token, name: "Legendary Dungeon Token", material: DIAMOND, amount: 2, rarity: LEGENDARY}
  - id: w_no_death_3
    name: "&bClear 3 Dungeon không chết lần nào"
    type: NO_DEATH_CLEAR
    amount: 3
    reward:
      money: 10000
      items:
        - {id: mythical_token, name: "Mythical Dungeon Token", material: NETHERITE_SCRAP, amount: 1, rarity: MYTHIC}
  - id: w_boss_dmg
    name: "&cGây 300.000 sát thương lên Boss"
    type: BOSS_DAMAGE
    amount: 300000
    reward:
      money: 5000
      items:
        - {id: epic_token, name: "Epic Dungeon Token", material: EMERALD, amount: 2, rarity: EPIC}
""");
        M.put("rewards/ancient_guardian.yml", """
id: ancient_guardian
# Tien co ban; nhan x0.5 - x2 theo dong gop va he so tien cua tier.
money: 1500
# type: ITEM | COMMAND | MONEY.  chance tinh bang phan tram.
# Tuy chon: guaranteed, only-top, amount/max-amount, enchants, unbreakable, glow, custom-model-data, pity-step/pity-max.
drops:
  # ---- LUON NHAN (guaranteed) ----
  - id: common_token
    name: "Common Dungeon Token"
    type: ITEM
    material: GOLD_NUGGET
    amount: 1
    max-amount: 2
    guaranteed: true
    rarity: EPIC
    glow: true
    lore: ["&7Dung de doi vat pham o cua hang Dungeon."]
  # ---- NGUYEN LIEU ----
  - id: ancient_core
    name: "Ancient Core"
    type: ITEM
    material: AMETHYST_SHARD
    amount: 1
    max-amount: 2
    chance: 35      # phan tram
    rarity: EPIC
    glow: true
    lore: ["&7Nguyen lieu dung de nang cap trang bi."]
  # ---- VU KHI ----
  - id: guardian_sword
    name: "Guardian Sword"
    type: ITEM
    material: IRON_SWORD
    chance: 6
    rarity: LEGENDARY
    glow: true
    enchants: {sharpness: 4, unbreaking: 3, looting: 2, mending: 1}
    lore: ["&7Vu khi hiem tu Ancient Guardian."]
    pity-step: 0.2      # moi lan xui +0.2% ty le
    pity-max: 5
  - id: guardian_bow
    name: "Guardian Bow"
    type: ITEM
    material: BOW
    chance: 5
    rarity: LEGENDARY
    glow: true
    enchants: {power: 4, punch: 1, flame: 1, infinity: 1, unbreaking: 3}
    lore: ["&7Vu khi tam xa tu Ancient Guardian."]
    pity-step: 0.2
    pity-max: 5
  - id: guardian_helmet
    name: "Guardian Helmet"
    type: ITEM
    material: IRON_HELMET
    chance: 7
    rarity: EPIC
    glow: true
    enchants: {protection: 3, unbreaking: 3, mending: 1}
    lore: ["&7Bo giap Guardian."]
    pity-step: 0.1
    pity-max: 4
  - id: guardian_chestplate
    name: "Guardian Chestplate"
    type: ITEM
    material: IRON_CHESTPLATE
    chance: 5.6
    rarity: EPIC
    glow: true
    enchants: {protection: 3, unbreaking: 3, mending: 1}
    lore: ["&7Bo giap Guardian."]
    pity-step: 0.1
    pity-max: 4
  - id: guardian_leggings
    name: "Guardian Leggings"
    type: ITEM
    material: IRON_LEGGINGS
    chance: 6.3
    rarity: EPIC
    glow: true
    enchants: {protection: 3, unbreaking: 3, mending: 1}
    lore: ["&7Bo giap Guardian."]
    pity-step: 0.1
    pity-max: 4
  - id: guardian_boots
    name: "Guardian Boots"
    type: ITEM
    material: IRON_BOOTS
    chance: 7
    rarity: EPIC
    glow: true
    enchants: {protection: 3, unbreaking: 3, mending: 1}
    lore: ["&7Bo giap Guardian."]
    pity-step: 0.1
    pity-max: 4
  # ---- DO DAC BIET ----
  - id: guardian_shield
    name: "Guardian Shield"
    type: ITEM
    material: SHIELD
    chance: 3
    rarity: LEGENDARY
    glow: true
    enchants: {unbreaking: 3, mending: 1}
    lore: ["&7Bao vat cua Ancient Guardian."]
    pity-step: 0.05
    pity-max: 3
  # ---- PET / DANH HIEU: doi lenh thanh plugin pet/danh hieu cua ban ----
  - id: pet_stone_golem
    name: "Stone Golem Pet"
    type: COMMAND
    chance: 3
    rarity: EPIC
    commands:
      - "say {player} vừa nhận được thú cưng Stone Golem!"
    pity-step: 0.05
    pity-max: 3
  - id: ancient_title
    name: "Ancient Slayer"
    type: COMMAND
    chance: 1.5
    rarity: LEGENDARY
    commands:
      - "say {player} vừa mở khóa danh hiệu Ancient Slayer!"
    pity-step: 0.05
    pity-max: 2
""");
        M.put("rewards/cursed_tree.yml", """
id: cursed_tree
# Tien co ban; nhan x0.5 - x2 theo dong gop va he so tien cua tier.
money: 1500
# type: ITEM | COMMAND | MONEY.  chance tinh bang phan tram.
# Tuy chon: guaranteed, only-top, amount/max-amount, enchants, unbreakable, glow, custom-model-data, pity-step/pity-max.
drops:
  # ---- LUON NHAN (guaranteed) ----
  - id: common_token
    name: "Common Dungeon Token"
    type: ITEM
    material: GOLD_NUGGET
    amount: 1
    max-amount: 2
    guaranteed: true
    rarity: EPIC
    glow: true
    lore: ["&7Dung de doi vat pham o cua hang Dungeon."]
  # ---- NGUYEN LIEU ----
  - id: cursed_essence
    name: "Cursed Essence"
    type: ITEM
    material: SPIDER_EYE
    amount: 1
    max-amount: 2
    chance: 35      # phan tram
    rarity: EPIC
    glow: true
    lore: ["&7Nguyen lieu dung de nang cap trang bi."]
  # ---- VU KHI ----
  - id: cursed_axe
    name: "Cursed Axe"
    type: ITEM
    material: IRON_AXE
    chance: 6
    rarity: LEGENDARY
    glow: true
    enchants: {sharpness: 4, efficiency: 3, unbreaking: 3, mending: 1}
    lore: ["&7Vu khi hiem tu Cursed Tree."]
    pity-step: 0.2      # moi lan xui +0.2% ty le
    pity-max: 5
  - id: cursed_crossbow
    name: "Cursed Crossbow"
    type: ITEM
    material: CROSSBOW
    chance: 5
    rarity: LEGENDARY
    glow: true
    enchants: {quick_charge: 1, piercing: 2, unbreaking: 3, mending: 1}
    lore: ["&7Vu khi tam xa tu Cursed Tree."]
    pity-step: 0.2
    pity-max: 5
  - id: cursed_helmet
    name: "Cursed Helmet"
    type: ITEM
    material: IRON_HELMET
    chance: 7
    rarity: EPIC
    glow: true
    enchants: {protection: 3, unbreaking: 3, mending: 1}
    lore: ["&7Bo giap Cursed."]
    pity-step: 0.1
    pity-max: 4
  - id: cursed_chestplate
    name: "Cursed Chestplate"
    type: ITEM
    material: IRON_CHESTPLATE
    chance: 5.6
    rarity: EPIC
    glow: true
    enchants: {protection: 3, unbreaking: 3, mending: 1}
    lore: ["&7Bo giap Cursed."]
    pity-step: 0.1
    pity-max: 4
  - id: cursed_leggings
    name: "Cursed Leggings"
    type: ITEM
    material: IRON_LEGGINGS
    chance: 6.3
    rarity: EPIC
    glow: true
    enchants: {protection: 3, unbreaking: 3, mending: 1}
    lore: ["&7Bo giap Cursed."]
    pity-step: 0.1
    pity-max: 4
  - id: cursed_boots
    name: "Cursed Boots"
    type: ITEM
    material: IRON_BOOTS
    chance: 7
    rarity: EPIC
    glow: true
    enchants: {protection: 3, unbreaking: 3, mending: 1}
    lore: ["&7Bo giap Cursed."]
    pity-step: 0.1
    pity-max: 4
  # ---- DO DAC BIET ----
  - id: cursed_totem
    name: "Cursed Totem"
    type: ITEM
    material: TOTEM_OF_UNDYING
    chance: 3
    rarity: LEGENDARY
    glow: true
    enchants: {}
    lore: ["&7Bao vat cua Cursed Tree."]
    pity-step: 0.05
    pity-max: 3
  # ---- PET / DANH HIEU: doi lenh thanh plugin pet/danh hieu cua ban ----
  - id: pet_treant
    name: "Baby Treant Pet"
    type: COMMAND
    chance: 3
    rarity: EPIC
    commands:
      - "say {player} vừa nhận được thú cưng Baby Treant!"
    pity-step: 0.05
    pity-max: 3
  - id: cursed_title
    name: "Cursed Hunter"
    type: COMMAND
    chance: 1.5
    rarity: LEGENDARY
    commands:
      - "say {player} vừa mở khóa danh hiệu Cursed Hunter!"
    pity-step: 0.05
    pity-max: 2
""");
        M.put("rewards/frost_titan.yml", """
id: frost_titan
# Tien co ban; nhan x0.5 - x2 theo dong gop va he so tien cua tier.
money: 20000
# type: ITEM | COMMAND | MONEY.  chance tinh bang phan tram.
# Tuy chon: guaranteed, only-top, amount/max-amount, enchants, unbreakable, glow, custom-model-data, pity-step/pity-max.
drops:
  # ---- LUON NHAN (guaranteed) ----
  - id: legendary_token
    name: "Legendary Dungeon Token"
    type: ITEM
    material: DIAMOND
    amount: 2
    max-amount: 4
    guaranteed: true
    rarity: EPIC
    glow: true
    lore: ["&7Dung de doi vat pham o cua hang Dungeon."]
  # ---- NGUYEN LIEU ----
  - id: frost_core
    name: "Frost Core"
    type: ITEM
    material: BLUE_ICE
    amount: 1
    max-amount: 4
    chance: 25      # phan tram
    rarity: LEGENDARY
    glow: true
    lore: ["&7Nguyen lieu dung de nang cap trang bi."]
  # ---- VU KHI ----
  - id: titan_cleaver
    name: "Titan Cleaver"
    type: ITEM
    material: NETHERITE_AXE
    chance: 2.5
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {sharpness: 7, efficiency: 5, unbreaking: 6, mending: 1}
    lore: ["&7Vu khi hiem tu Frost Titan."]
    pity-step: 0.1      # moi lan xui +0.1% ty le
    pity-max: 5
  - id: glacier_crossbow
    name: "Glacier Crossbow"
    type: ITEM
    material: CROSSBOW
    chance: 2
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {quick_charge: 3, piercing: 4, unbreaking: 6, mending: 1}
    lore: ["&7Vu khi tam xa tu Frost Titan."]
    pity-step: 0.1
    pity-max: 5
  - id: glacial_helmet
    name: "Glacial Helmet"
    type: ITEM
    material: NETHERITE_HELMET
    chance: 3
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {protection: 6, unbreaking: 6, mending: 1, thorns: 2}
    lore: ["&7Bo giap Glacial."]
    pity-step: 0.1
    pity-max: 4
  - id: glacial_chestplate
    name: "Glacial Chestplate"
    type: ITEM
    material: NETHERITE_CHESTPLATE
    chance: 2.4
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {protection: 6, unbreaking: 6, mending: 1, thorns: 2}
    lore: ["&7Bo giap Glacial."]
    pity-step: 0.1
    pity-max: 4
  - id: glacial_leggings
    name: "Glacial Leggings"
    type: ITEM
    material: NETHERITE_LEGGINGS
    chance: 2.7
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {protection: 6, unbreaking: 6, mending: 1, thorns: 2}
    lore: ["&7Bo giap Glacial."]
    pity-step: 0.1
    pity-max: 4
  - id: glacial_boots
    name: "Glacial Boots"
    type: ITEM
    material: NETHERITE_BOOTS
    chance: 3
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {protection: 6, unbreaking: 6, mending: 1, thorns: 2}
    lore: ["&7Bo giap Glacial."]
    pity-step: 0.1
    pity-max: 4
  # ---- DO DAC BIET ----
  - id: frozen_heart
    name: "Frozen Heart"
    type: ITEM
    material: NETHER_STAR
    chance: 1.2
    rarity: DIVINE
    glow: true
    only-top: 3      # chi top sat thuong moi roll duoc
    enchants: {}
    lore: ["&7Bao vat cua Frost Titan."]
    pity-step: 0.05
    pity-max: 3
  # ---- PET / DANH HIEU: doi lenh thanh plugin pet/danh hieu cua ban ----
  - id: pet_yeti
    name: "Baby Yeti Pet"
    type: COMMAND
    chance: 1.5
    rarity: MYTHIC
    commands:
      - "say {player} vừa nhận được thú cưng Baby Yeti!"
    pity-step: 0.05
    pity-max: 3
  - id: frost_title
    name: "Frost Breaker"
    type: COMMAND
    chance: 0.7
    rarity: DIVINE
    commands:
      - "say {player} vừa mở khóa danh hiệu Frost Breaker!"
    pity-step: 0.05
    pity-max: 2
""");
        M.put("rewards/infernal_king.yml", """
id: infernal_king
# Tien co ban; nhan x0.5 - x2 theo dong gop va he so tien cua tier.
money: 6000
# type: ITEM | COMMAND | MONEY.  chance tinh bang phan tram.
# Tuy chon: guaranteed, only-top, amount/max-amount, enchants, unbreakable, glow, custom-model-data, pity-step/pity-max.
drops:
  # ---- LUON NHAN (guaranteed) ----
  - id: epic_token
    name: "Epic Dungeon Token"
    type: ITEM
    material: EMERALD
    amount: 1
    max-amount: 3
    guaranteed: true
    rarity: EPIC
    glow: true
    lore: ["&7Dung de doi vat pham o cua hang Dungeon."]
  # ---- NGUYEN LIEU ----
  - id: infernal_core
    name: "Infernal Core"
    type: ITEM
    material: MAGMA_CREAM
    amount: 1
    max-amount: 3
    chance: 30      # phan tram
    rarity: EPIC
    glow: true
    lore: ["&7Nguyen lieu dung de nang cap trang bi."]
  # ---- VU KHI ----
  - id: infernal_blade
    name: "Infernal Blade"
    type: ITEM
    material: DIAMOND_SWORD
    chance: 4
    rarity: MYTHIC
    glow: true
    enchants: {sharpness: 5, unbreaking: 4, looting: 3, mending: 1, fire_aspect: 2, sweeping_edge: 3}
    lore: ["&7Vu khi hiem tu Infernal King."]
    pity-step: 0.15      # moi lan xui +0.15% ty le
    pity-max: 5
  - id: hellfire_bow
    name: "Hellfire Bow"
    type: ITEM
    material: BOW
    chance: 3.5
    rarity: MYTHIC
    glow: true
    enchants: {power: 5, punch: 2, flame: 1, infinity: 1, unbreaking: 4}
    lore: ["&7Vu khi tam xa tu Infernal King."]
    pity-step: 0.15
    pity-max: 5
  - id: infernal_helmet
    name: "Infernal Helmet"
    type: ITEM
    material: DIAMOND_HELMET
    chance: 5
    rarity: LEGENDARY
    glow: true
    enchants: {protection: 4, unbreaking: 4, mending: 1, thorns: 1}
    lore: ["&7Bo giap Infernal."]
    pity-step: 0.1
    pity-max: 4
  - id: infernal_chestplate
    name: "Infernal Chestplate"
    type: ITEM
    material: DIAMOND_CHESTPLATE
    chance: 4.0
    rarity: LEGENDARY
    glow: true
    enchants: {protection: 4, unbreaking: 4, mending: 1, thorns: 1}
    lore: ["&7Bo giap Infernal."]
    pity-step: 0.1
    pity-max: 4
  - id: infernal_leggings
    name: "Infernal Leggings"
    type: ITEM
    material: DIAMOND_LEGGINGS
    chance: 4.5
    rarity: LEGENDARY
    glow: true
    enchants: {protection: 4, unbreaking: 4, mending: 1, thorns: 1}
    lore: ["&7Bo giap Infernal."]
    pity-step: 0.1
    pity-max: 4
  - id: infernal_boots
    name: "Infernal Boots"
    type: ITEM
    material: DIAMOND_BOOTS
    chance: 5
    rarity: LEGENDARY
    glow: true
    enchants: {protection: 4, unbreaking: 4, mending: 1, thorns: 1}
    lore: ["&7Bo giap Infernal."]
    pity-step: 0.1
    pity-max: 4
  # ---- DO DAC BIET ----
  - id: ember_totem
    name: "Ember Totem"
    type: ITEM
    material: TOTEM_OF_UNDYING
    chance: 2
    rarity: MYTHIC
    glow: true
    enchants: {}
    lore: ["&7Bao vat cua Infernal King."]
    pity-step: 0.05
    pity-max: 3
  # ---- PET / DANH HIEU: doi lenh thanh plugin pet/danh hieu cua ban ----
  - id: pet_hellhound
    name: "Hellhound Pet"
    type: COMMAND
    chance: 2
    rarity: LEGENDARY
    commands:
      - "say {player} vừa nhận được thú cưng Hellhound!"
    pity-step: 0.05
    pity-max: 3
  - id: infernal_title
    name: "Infernal Conqueror"
    type: COMMAND
    chance: 1
    rarity: MYTHIC
    commands:
      - "say {player} vừa mở khóa danh hiệu Infernal Conqueror!"
    pity-step: 0.05
    pity-max: 2
""");
        M.put("rewards/ocean_lord.yml", """
id: ocean_lord
# Tien co ban; nhan x0.5 - x2 theo dong gop va he so tien cua tier.
money: 6000
# type: ITEM | COMMAND | MONEY.  chance tinh bang phan tram.
# Tuy chon: guaranteed, only-top, amount/max-amount, enchants, unbreakable, glow, custom-model-data, pity-step/pity-max.
drops:
  # ---- LUON NHAN (guaranteed) ----
  - id: epic_token
    name: "Epic Dungeon Token"
    type: ITEM
    material: EMERALD
    amount: 1
    max-amount: 3
    guaranteed: true
    rarity: EPIC
    glow: true
    lore: ["&7Dung de doi vat pham o cua hang Dungeon."]
  # ---- NGUYEN LIEU ----
  - id: abyss_shard
    name: "Abyss Shard"
    type: ITEM
    material: PRISMARINE_SHARD
    amount: 1
    max-amount: 3
    chance: 30      # phan tram
    rarity: EPIC
    glow: true
    lore: ["&7Nguyen lieu dung de nang cap trang bi."]
  # ---- VU KHI ----
  - id: tidebreaker
    name: "Tidebreaker"
    type: ITEM
    material: DIAMOND_SWORD
    chance: 4
    rarity: MYTHIC
    glow: true
    enchants: {sharpness: 5, unbreaking: 4, looting: 3, mending: 1, fire_aspect: 2, sweeping_edge: 3}
    lore: ["&7Vu khi hiem tu Ocean Lord."]
    pity-step: 0.15      # moi lan xui +0.15% ty le
    pity-max: 5
  - id: ocean_trident
    name: "Ocean Trident"
    type: ITEM
    material: TRIDENT
    chance: 3.5
    rarity: MYTHIC
    glow: true
    enchants: {impaling: 5, loyalty: 3, channeling: 1, unbreaking: 4, mending: 1}
    lore: ["&7Vu khi tam xa tu Ocean Lord."]
    pity-step: 0.15
    pity-max: 5
  - id: abyssal_helmet
    name: "Abyssal Helmet"
    type: ITEM
    material: DIAMOND_HELMET
    chance: 5
    rarity: LEGENDARY
    glow: true
    enchants: {protection: 4, unbreaking: 4, mending: 1, thorns: 1}
    lore: ["&7Bo giap Abyssal."]
    pity-step: 0.1
    pity-max: 4
  - id: abyssal_chestplate
    name: "Abyssal Chestplate"
    type: ITEM
    material: DIAMOND_CHESTPLATE
    chance: 4.0
    rarity: LEGENDARY
    glow: true
    enchants: {protection: 4, unbreaking: 4, mending: 1, thorns: 1}
    lore: ["&7Bo giap Abyssal."]
    pity-step: 0.1
    pity-max: 4
  - id: abyssal_leggings
    name: "Abyssal Leggings"
    type: ITEM
    material: DIAMOND_LEGGINGS
    chance: 4.5
    rarity: LEGENDARY
    glow: true
    enchants: {protection: 4, unbreaking: 4, mending: 1, thorns: 1}
    lore: ["&7Bo giap Abyssal."]
    pity-step: 0.1
    pity-max: 4
  - id: abyssal_boots
    name: "Abyssal Boots"
    type: ITEM
    material: DIAMOND_BOOTS
    chance: 5
    rarity: LEGENDARY
    glow: true
    enchants: {protection: 4, unbreaking: 4, mending: 1, thorns: 1}
    lore: ["&7Bo giap Abyssal."]
    pity-step: 0.1
    pity-max: 4
  # ---- DO DAC BIET ----
  - id: abyss_heart
    name: "Heart of the Abyss"
    type: ITEM
    material: HEART_OF_THE_SEA
    chance: 2
    rarity: MYTHIC
    glow: true
    enchants: {}
    lore: ["&7Bao vat cua Ocean Lord."]
    pity-step: 0.05
    pity-max: 3
  # ---- PET / DANH HIEU: doi lenh thanh plugin pet/danh hieu cua ban ----
  - id: pet_kraken
    name: "Baby Kraken Pet"
    type: COMMAND
    chance: 2
    rarity: LEGENDARY
    commands:
      - "say {player} vừa nhận được thú cưng Baby Kraken!"
    pity-step: 0.05
    pity-max: 3
  - id: abyss_title
    name: "Abyss Walker"
    type: COMMAND
    chance: 1
    rarity: MYTHIC
    commands:
      - "say {player} vừa mở khóa danh hiệu Abyss Walker!"
    pity-step: 0.05
    pity-max: 2
""");
        M.put("rewards/pharaoh.yml", """
id: pharaoh
# Tien co ban; nhan x0.5 - x2 theo dong gop va he so tien cua tier.
money: 20000
# type: ITEM | COMMAND | MONEY.  chance tinh bang phan tram.
# Tuy chon: guaranteed, only-top, amount/max-amount, enchants, unbreakable, glow, custom-model-data, pity-step/pity-max.
drops:
  # ---- LUON NHAN (guaranteed) ----
  - id: legendary_token
    name: "Legendary Dungeon Token"
    type: ITEM
    material: DIAMOND
    amount: 2
    max-amount: 4
    guaranteed: true
    rarity: EPIC
    glow: true
    lore: ["&7Dung de doi vat pham o cua hang Dungeon."]
  # ---- NGUYEN LIEU ----
  - id: pharaoh_relic
    name: "Pharaoh Relic"
    type: ITEM
    material: GOLD_INGOT
    amount: 1
    max-amount: 4
    chance: 25      # phan tram
    rarity: LEGENDARY
    glow: true
    lore: ["&7Nguyen lieu dung de nang cap trang bi."]
  # ---- VU KHI ----
  - id: pharaoh_blade
    name: "Pharaoh Blade"
    type: ITEM
    material: NETHERITE_SWORD
    chance: 2.5
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {sharpness: 7, unbreaking: 6, looting: 4, mending: 1, fire_aspect: 3, sweeping_edge: 4}
    lore: ["&7Vu khi hiem tu Pharaoh."]
    pity-step: 0.1      # moi lan xui +0.1% ty le
    pity-max: 5
  - id: sun_bow
    name: "Sun Bow"
    type: ITEM
    material: BOW
    chance: 2
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {power: 7, punch: 3, flame: 1, infinity: 1, unbreaking: 6}
    lore: ["&7Vu khi tam xa tu Pharaoh."]
    pity-step: 0.1
    pity-max: 5
  - id: pharaoh_helmet
    name: "Pharaoh Helmet"
    type: ITEM
    material: NETHERITE_HELMET
    chance: 3
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {protection: 6, unbreaking: 6, mending: 1, thorns: 2}
    lore: ["&7Bo giap Pharaoh."]
    pity-step: 0.1
    pity-max: 4
  - id: pharaoh_chestplate
    name: "Pharaoh Chestplate"
    type: ITEM
    material: NETHERITE_CHESTPLATE
    chance: 2.4
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {protection: 6, unbreaking: 6, mending: 1, thorns: 2}
    lore: ["&7Bo giap Pharaoh."]
    pity-step: 0.1
    pity-max: 4
  - id: pharaoh_leggings
    name: "Pharaoh Leggings"
    type: ITEM
    material: NETHERITE_LEGGINGS
    chance: 2.7
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {protection: 6, unbreaking: 6, mending: 1, thorns: 2}
    lore: ["&7Bo giap Pharaoh."]
    pity-step: 0.1
    pity-max: 4
  - id: pharaoh_boots
    name: "Pharaoh Boots"
    type: ITEM
    material: NETHERITE_BOOTS
    chance: 3
    rarity: MYTHIC
    glow: true
    unbreakable: true
    enchants: {protection: 6, unbreaking: 6, mending: 1, thorns: 2}
    lore: ["&7Bo giap Pharaoh."]
    pity-step: 0.1
    pity-max: 4
  # ---- DO DAC BIET ----
  - id: ankh_of_ra
    name: "Ankh of Ra"
    type: ITEM
    material: TOTEM_OF_UNDYING
    chance: 1.2
    rarity: DIVINE
    glow: true
    only-top: 3      # chi top sat thuong moi roll duoc
    enchants: {}
    lore: ["&7Bao vat cua Pharaoh."]
    pity-step: 0.05
    pity-max: 3
  # ---- PET / DANH HIEU: doi lenh thanh plugin pet/danh hieu cua ban ----
  - id: pet_scarab
    name: "Golden Scarab Pet"
    type: COMMAND
    chance: 1.5
    rarity: MYTHIC
    commands:
      - "say {player} vừa nhận được thú cưng Golden Scarab!"
    pity-step: 0.05
    pity-max: 3
  - id: pharaoh_title
    name: "Pharaoh's Bane"
    type: COMMAND
    chance: 0.7
    rarity: DIVINE
    commands:
      - "say {player} vừa mở khóa danh hiệu Pharaoh's Bane!"
    pity-step: 0.05
    pity-max: 2
""");
        M.put("rewards/void_emperor.yml", """
id: void_emperor
# Tien co ban; nhan x0.5 - x2 theo dong gop va he so tien cua tier.
money: 80000
# type: ITEM | COMMAND | MONEY.  chance tinh bang phan tram.
# Tuy chon: guaranteed, only-top, amount/max-amount, enchants, unbreakable, glow, custom-model-data, pity-step/pity-max.
drops:
  # ---- LUON NHAN (guaranteed) ----
  - id: mythical_token
    name: "Mythical Dungeon Token"
    type: ITEM
    material: NETHERITE_SCRAP
    amount: 3
    max-amount: 6
    guaranteed: true
    rarity: EPIC
    glow: true
    lore: ["&7Dung de doi vat pham o cua hang Dungeon."]
  # ---- NGUYEN LIEU ----
  - id: void_shard
    name: "Void Shard"
    type: ITEM
    material: ECHO_SHARD
    amount: 1
    max-amount: 5
    chance: 20      # phan tram
    rarity: MYTHIC
    glow: true
    lore: ["&7Nguyen lieu dung de nang cap trang bi."]
  # ---- VU KHI ----
  - id: voidreaver
    name: "Voidreaver"
    type: ITEM
    material: NETHERITE_SWORD
    chance: 1.5
    rarity: DIVINE
    glow: true
    unbreakable: true
    enchants: {sharpness: 10, unbreaking: 10, looting: 5, mending: 1, fire_aspect: 4, sweeping_edge: 5}
    lore: ["&7Vu khi hiem tu Void Emperor."]
    pity-step: 0.08      # moi lan xui +0.08% ty le
    pity-max: 5
  - id: void_mace
    name: "Void Mace"
    type: ITEM
    material: MACE
    chance: 1.2
    rarity: DIVINE
    glow: true
    unbreakable: true
    enchants: {density: 7, breach: 5, unbreaking: 10, mending: 1}
    lore: ["&7Vu khi tam xa tu Void Emperor."]
    pity-step: 0.08
    pity-max: 5
  - id: void_helmet
    name: "Void Helmet"
    type: ITEM
    material: NETHERITE_HELMET
    chance: 2
    rarity: DIVINE
    glow: true
    unbreakable: true
    enchants: {protection: 9, unbreaking: 10, mending: 1, thorns: 3}
    lore: ["&7Bo giap Void."]
    pity-step: 0.1
    pity-max: 4
  - id: void_chestplate
    name: "Void Chestplate"
    type: ITEM
    material: NETHERITE_CHESTPLATE
    chance: 1.6
    rarity: DIVINE
    glow: true
    unbreakable: true
    enchants: {protection: 9, unbreaking: 10, mending: 1, thorns: 3}
    lore: ["&7Bo giap Void."]
    pity-step: 0.1
    pity-max: 4
  - id: void_leggings
    name: "Void Leggings"
    type: ITEM
    material: NETHERITE_LEGGINGS
    chance: 1.8
    rarity: DIVINE
    glow: true
    unbreakable: true
    enchants: {protection: 9, unbreaking: 10, mending: 1, thorns: 3}
    lore: ["&7Bo giap Void."]
    pity-step: 0.1
    pity-max: 4
  - id: void_boots
    name: "Void Boots"
    type: ITEM
    material: NETHERITE_BOOTS
    chance: 2
    rarity: DIVINE
    glow: true
    unbreakable: true
    enchants: {protection: 9, unbreaking: 10, mending: 1, thorns: 3}
    lore: ["&7Bo giap Void."]
    pity-step: 0.1
    pity-max: 4
  # ---- DO DAC BIET ----
  - id: void_wings
    name: "Void Wings"
    type: ITEM
    material: ELYTRA
    chance: 0.8
    rarity: DIVINE
    glow: true
    only-top: 1      # chi top sat thuong moi roll duoc
    enchants: {}
    lore: ["&7Bao vat cua Void Emperor."]
    pity-step: 0.05
    pity-max: 3
  - id: dragon_heart
    name: "Dragon Heart"
    type: ITEM
    material: NETHER_STAR
    chance: 3
    rarity: DIVINE
    glow: true
    only-top: 3
    lore: ["&7Trai tim cua rong, nguyen lieu cuoi cung."]
    pity-step: 0.1
    pity-max: 6
  # ---- PET / DANH HIEU: doi lenh thanh plugin pet/danh hieu cua ban ----
  - id: pet_void_dragon
    name: "Void Dragonling Pet"
    type: COMMAND
    chance: 1
    rarity: DIVINE
    commands:
      - "say {player} vừa nhận được thú cưng Void Dragonling!"
    pity-step: 0.05
    pity-max: 3
  - id: void_title
    name: "Void Destroyer"
    type: COMMAND
    chance: 0.5
    rarity: DIVINE
    commands:
      - "say {player} vừa mở khóa danh hiệu Void Destroyer!"
    pity-step: 0.05
    pity-max: 2
""");
        M.put("schedule.yml", """
enabled: true
timezone: Asia/Ho_Chi_Minh
open-every-hours: 1        # mở mỗi N giờ (căn theo giờ trong ngày)
open-at-minute: 0          # phút mở trong giờ
announce-at-minutes: [10, 5, 1]
no-repeat: true            # không mở lại Dungeon vừa rồi
golden-hour:
  enabled: true
  windows: ["20:00-22:00", "12:00-13:00"]
  contribution-multiplier: 2.0
  drop-multiplier: 1.5
""");
        M.put("shop.yml", """
# Cua hang doi Token (/dungeon shop).
# cost: {id_token: so_luong}. Token: common_token, epic_token, legendary_token, mythical_token.
# daily-limit: gioi han moi nguoi moi ngay (0 hoac bo trong = khong gioi han).
# Muc co id trung id token (epic_token...) la muc DOI TOKEN cap thap len cap cao.
# Cac truong khac giong rewards/*.yml: type, material, amount, rarity, enchants, lore, glow, commands...
items:
  - id: shop_golden_apple
    name: "Golden Apple x8"
    type: ITEM
    material: GOLDEN_APPLE
    amount: 8
    rarity: RARE
    cost: {common_token: 2}
  - id: shop_xp_bottles
    name: "Experience Bottle x32"
    type: ITEM
    material: EXPERIENCE_BOTTLE
    amount: 32
    rarity: RARE
    cost: {common_token: 3}
  - id: shop_diamond_block
    name: "Diamond Block x2"
    type: ITEM
    material: DIAMOND_BLOCK
    amount: 2
    rarity: EPIC
    cost: {common_token: 6}
    daily-limit: 5
  - id: shop_xp_bottles_big
    name: "Experience Bottle x64"
    type: ITEM
    material: EXPERIENCE_BOTTLE
    amount: 64
    rarity: EPIC
    cost: {epic_token: 2}
  - id: shop_enchanted_apple
    name: "Enchanted Golden Apple x2"
    type: ITEM
    material: ENCHANTED_GOLDEN_APPLE
    amount: 2
    rarity: EPIC
    cost: {epic_token: 3}
    daily-limit: 3
  - id: shop_totem
    name: "Totem of Undying"
    type: ITEM
    material: TOTEM_OF_UNDYING
    amount: 1
    rarity: EPIC
    cost: {epic_token: 6}
    daily-limit: 2
  - id: shop_netherite_ingot
    name: "Netherite Ingot x2"
    type: ITEM
    material: NETHERITE_INGOT
    amount: 2
    rarity: LEGENDARY
    cost: {epic_token: 10}
    daily-limit: 2
  - id: shop_beacon
    name: "Beacon"
    type: ITEM
    material: BEACON
    amount: 1
    rarity: LEGENDARY
    cost: {legendary_token: 8}
    daily-limit: 1
  - id: shop_elytra
    name: "Elytra"
    type: ITEM
    material: ELYTRA
    amount: 1
    rarity: LEGENDARY
    cost: {legendary_token: 20}
    daily-limit: 1
  - id: shop_dragon_head
    name: "Dragon Head"
    type: ITEM
    material: DRAGON_HEAD
    amount: 1
    rarity: MYTHIC
    cost: {legendary_token: 15}
    daily-limit: 1
  - id: shop_nether_star
    name: "Nether Star"
    type: ITEM
    material: NETHER_STAR
    amount: 1
    rarity: MYTHIC
    cost: {mythical_token: 5}
    daily-limit: 3
  - id: shop_dragon_egg
    name: "Dragon Egg"
    type: ITEM
    material: DRAGON_EGG
    amount: 1
    rarity: DIVINE
    glow: true
    cost: {mythical_token: 30}
    daily-limit: 1
  - id: shop_pet_crate
    name: "Hộp thú cưng huyền thoại"
    type: COMMAND
    rarity: MYTHIC
    commands:
      - "say {player} vừa mở Hộp thú cưng huyền thoại! (đổi lệnh này sang plugin pet của bạn)"
    cost: {mythical_token: 10}
    daily-limit: 2
  # ---- doi token cap thap len cap cao (id phai trung id token) ----
  - id: epic_token
    name: "Epic Dungeon Token"
    type: ITEM
    material: EMERALD
    amount: 1
    rarity: EPIC
    glow: true
    lore: ["&7Doi tu 10 Common Token."]
    cost: {common_token: 10}
  - id: legendary_token
    name: "Legendary Dungeon Token"
    type: ITEM
    material: DIAMOND
    amount: 1
    rarity: LEGENDARY
    glow: true
    lore: ["&7Doi tu 10 Epic Token."]
    cost: {epic_token: 10}
  - id: mythical_token
    name: "Mythical Dungeon Token"
    type: ITEM
    material: NETHERITE_SCRAP
    amount: 1
    rarity: MYTHIC
    glow: true
    lore: ["&7Doi tu 10 Legendary Token."]
    cost: {legendary_token: 10}
""");
    }
}
