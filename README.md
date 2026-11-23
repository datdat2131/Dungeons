# AstraXDungeon v0.3.0

Plugin Paper 1.21.4 - Java 21 - Maven - **Flat layout** (moi file `.java` nam ngay thu muc goc).

## 4 cap do - 7 Dungeon
Common (Ancient Temple, Cursed Forest) 5 dot | Epic (Infernal Fortress, Abyssal Temple) 10 dot |
Legendary (Lost Pyramid, Frozen Citadel) 12 dot | Mythical (Void Dimension) 15 dot.

## Tinh nang
- Wave tu sinh: dot Tinh Anh, dot Bay Dan, Mini-Boss; quai co dac tinh (tank, vampire, venom...).
- Boss nhieu giai doan, ky nang canh bao truoc, tu noi dien khi danh qua lau.
- He thong mang + **hoi sinh dong doi** (giu SHIFT canh nguoi guc).
- **Hop thu thuong**: offline luc Boss chet van nhan thuong - `/dungeon claim`.
- **Cua hang Token**: `/dungeon shop` (shop.yml), doi token len cap, gioi han mua moi ngay.
- **Nhiem vu ngay/tuan** + **streak**: `/dungeon quests` (quests.yml), bonus ty le roi theo chuoi ngay.
- Party, bang xep hang, PlaceholderAPI, Vault, **thong bao Discord** (config discord.webhook-url).

## Cau hinh (plugins/AstraXDungeon/)
tiers.yml (tu tao) - dungeons/ - bosses/ - rewards/ - shop.yml - quests.yml - config.yml - messages.yml

## Build
`mvn --batch-mode --no-transfer-progress clean package` -> `target/AstraXDungeon.jar`. GitHub: tab Actions / muc Releases.

## Lenh
`/dungeon` `/dungeon tiers|rewards|shop|quests|claim|stats|ranking` `/dungeon party ...`
`/dungeon admin setlobby|setspawn <id>|setbossspawn <id>|start <id>|boss spawn|reload`
