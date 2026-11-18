# AstraXDungeon

Plugin Paper 1.21.4 - Java 21 - Maven - **Flat layout** (moi file `.java` nam ngay thu muc goc).

## Build
```
mvn --batch-mode --no-transfer-progress clean package
```
Jar: `target/AstraXDungeon.jar`. Tren GitHub: tab **Actions** (artifact) hoac muc **Releases**.

## Cau truc
- `*.java`, `pom.xml`, `plugin.yml`, `config.yml`, `messages.yml`, `schedule.yml`: thu muc goc.
- `dungeons/`, `bosses/`, `rewards/`: file cau hinh mac dinh (thieu cung khong sao, plugin tu tao tu `DefaultFiles.java`).
- `.github/workflows/main.yml`: tu dong build.

## Lenh nhanh
`/dungeon` - `/dungeon admin setlobby` - `/dungeon admin setspawn <id>` - `/dungeon admin setbossspawn <id>`
