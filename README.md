# AstraXDungeon v0.4.0

Plugin Paper 1.21.4 - Java 21 - Maven - **Flat layout** (moi file `.java` nam ngay thu muc goc).

## Moi trong v0.4.0
- **Quai khong danh nhau**: huy sat thuong/muc tieu giua quai cung Dungeon (tien ban, no, phep, nanh Evoker, binh phep).
- **Ky nang cho quai thuong** (mob-skills.yml): leap, fireball, volley, blast, heal, buff, self_buff, summon, teleport.
  Gan trong dungeons/*.yml (`skills: [...]`) hoac ngau nhien theo tiers.yml (`mob.skill-chance`). Boss dung chung duoc.
- **Thoi gian vao**: bat dau sau 2 phut, con 3 phut cho nguoi vao muon, sau do dong cong han (dungeon.join-window-seconds / late-join-seconds).
- **GUI admin** de tu dua do minh lam vao cua hang va bang phan thuong:
  `/dungeon admin edit` | `shopedit` | `lootedit [boss]`.

## 4 cap do - 7 Dungeon
Common (Ancient Temple, Cursed Forest) 5 dot | Epic (Infernal Fortress, Abyssal Temple) 10 dot |
Legendary (Lost Pyramid, Frozen Citadel) 12 dot | Mythical (Void Dimension) 15 dot.

## Tinh nang khac
Wave tu sinh (Tinh Anh/Bay Dan/Mini-Boss), dac tinh quai, Boss nhieu giai doan, he thong mang + hoi sinh dong doi,
hop thu thuong (/dungeon claim), cua hang Token, nhiem vu ngay/tuan + streak, Party, BXH, PlaceholderAPI, Vault, Discord.

## Cau hinh (plugins/AstraXDungeon/)
tiers.yml (tu tao) - dungeons/ - bosses/ - rewards/ - shop.yml - quests.yml - mob-skills.yml - config.yml - messages.yml

## Build
`mvn --batch-mode --no-transfer-progress clean package` -> `target/AstraXDungeon.jar`.
