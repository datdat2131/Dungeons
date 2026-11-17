# AstraXDungeon

Plugin Paper 1.21.4 · Java 21 · Maven · Flat layout (mọi file `.java` nằm ngay thư mục gốc).

## Build

Yêu cầu JDK 21 và Maven 3.8+.

```bash
mvn --batch-mode --no-transfer-progress clean package
```

File JAR đầu ra: `target/AstraXDungeon.jar`.

## GitHub Actions

Workflow nằm tại `.github/workflows/main.yml`. Nó tự chạy khi push hoặc pull request vào nhánh `main`, hoặc có thể chạy thủ công bằng **Actions → Build AstraXDungeon → Run workflow**. JAR được xuất trong mục **Artifacts**.

## Cài đặt

Chép `target/AstraXDungeon.jar` vào thư mục `plugins/` của máy chủ Paper 1.21.4 chạy Java 21, sau đó khởi động lại server.

## Lệnh

- `/dungeon`
- `/dungeon admin setlobby`
- `/dungeon admin setspawn <id>`
- `/dungeon admin setbossspawn <id>`

## Cấu trúc

- Các file Java: thư mục gốc.
- `plugin.yml`, `config.yml`, `messages.yml`, `schedule.yml`: thư mục gốc.
- Các cấu hình boss, dungeon và rewards: giữ thư mục con để plugin có thể nạp đúng đường dẫn tài nguyên.
- `pom.xml`: Maven cấu hình sourceDirectory tại thư mục gốc.
