# PBL4-refactor — Hướng dẫn toàn diện

Tài liệu này tóm tắt cách phát triển (development), đóng gói/triển khai (deployment) và cách chạy hệ thống (Agent, Manager, ManagerWeb) cho repository PBL4-refactor.

Nội dung chính được viết bằng tiếng Việt để dễ theo dõi trong quá trình phát triển và vận hành.

## Mục lục
- Tổng quan
- Yêu cầu tiên quyết
- Cấu trúc repository
- Hướng dẫn phát triển (build & run) cho từng module
- Control Panels (CLI / GUI) — xây dựng và chạy
- Triển khai / Packaging releases
- Chạy deployment (Deploy scripts)
- Ghi chú vận hành & xử lý sự cố (Troubleshooting)
- Góp phần & phát triển mở rộng

---

## Tổng quan

Kiến trúc chính của dự án gồm ba thành phần:

- Agent: thu thập dữ liệu hệ thống (CPU, RAM, process) và báo cáo về Manager.
- Manager: thu thập, lưu trữ (SQLite) và cung cấp giao diện CLI/GUI để quản lý.
- ManagerWeb: giao diện Web (JSP/Servlets) hiển thị dashboard và truy vấn dữ liệu từ Manager.

Mục tiêu README này là tập hợp các hướng dẫn thực tế để bạn có thể phát triển, đóng gói và triển khai hệ thống trên môi trường thực tế.

## Yêu cầu tiên quyết

- Java JDK 8+ (thực tế đã test trên JDK 8, 11, 17)
- Maven 3.6+
- Go 1.20+ (để build ControlPanel CLI / GUI)
- Git
- zip (để tạo Releases bằng script)

Lưu ý nền tảng: nhiều script trong `Deploy/` có cả bản `.sh` (macOS/Linux) và `.bat` (Windows). GUI Control Panel dùng thư viện Fyne và có các phụ thuộc native (OpenGL) nên cross-compile GUI có thể phức tạp.

## Cấu trúc repository (tóm tắt)

Các thư mục chính:

- Agent/ — mã nguồn và pom.xml cho Agent (Java)
- Manager/ — mã nguồn và pom.xml cho Manager (Java)
- ManagerWeb/ — webapp (JSP), pom.xml để build WAR
- ControlPanel/ — Go-based control panels cho Agent & Manager (CLI + GUI builds)
- Deploy/ — các script platform-specific dùng để chạy và cài đặt (Agent/Manager)
- scripts/ — các script tiện ích (ví dụ: `create_releases.sh`)

## Hướng dẫn phát triển (build + run)

Phần này hướng dẫn làm việc nhanh với từng module.

1) Build Agent (Java)

```bash
cd Agent
mvn clean package
# kết quả: target/Agent.jar
```

2) Build Manager (Java)

```bash
cd Manager
mvn clean package
# kết quả: target/Manager.jar
```

3) Build ManagerWeb (webapp)

```bash
cd ManagerWeb
mvn clean package
# kết quả: target/ManagerWeb.war
```

4) ControlPanel (Go) — CLI & GUI

ControlPanel chứa các chương trình nhỏ bằng Go để dễ quản lý cục bộ (Control Panel cho Agent/Manager). Mỗi control panel có hai entry points được phân biệt bằng build tags `cli` và `gui`.

Ví dụ build cho Manager control panel (CLI):

```bash
cd ControlPanel/Manager
go build -tags=cli -o ManagerControlPanel-cli .
```

Và GUI (nên build trên máy chủ có hỗ trợ GUI/OpenGL):

```bash
go build -tags=gui -o ManagerControlPanel-gui .
```

Lưu ý: GUI build sử dụng `fyne.io/fyne/v2` và có thể yêu cầu thêm toolchain native khi cross-compile. Nếu gặp lỗi liên quan đến OpenGL/go-gl, hãy build GUI trực tiếp trên cùng nền tảng mục tiêu hoặc chỉ phân phối bản CLI.

## Chạy trong môi trường development

1) Bật Manager (bắt buộc trước khi Agent gửi dữ liệu):

- GUI Mode (Manager):

```bash
cd Manager
java -jar target/Manager.jar --gui
```

- CLI Mode (Manager):

```bash
java -jar target/Manager.jar
# dùng các lệnh tương tác: scan, list agents, help
```

2) Khởi động Agent(s):

```bash
cd Agent
java -jar target/Agent.jar
# hoặc GUI: java -jar target/Agent.jar --gui
```

3) Chạy ManagerWeb (tùy chọn):

```bash
cd ManagerWeb
mvn jetty:run
# truy cập: http://localhost:8080/ManagerWeb
```

## Control Panel: chi tiết (CLI & GUI)

Control panel được thiết kế để gọi các script trong thư mục `Deploy/` để cài đặt hoặc khởi động các phần mềm.

- Ví dụ script Manager (unix): `Deploy/Manager/run_manager_cli.sh`, `run_manager_gui.sh`, `install_deploy.sh`, `start_web.sh`.
- ControlPanel ghi các marker files (ví dụ `.java_ok`, `.manager_running`, `.deploy_installed`, `.web_running`) vào cùng thư mục với executable để duy trì trạng thái giữa các lần chạy.

CLI:

- Chạy CLI build (xem phần build ở trên) và chạy `./ManagerControlPanel-cli`.
- Menu CLI đã implement các hành động tuần tự: cài Java → chạy Manager (foreground) hoặc chạy background (độc quyền với foreground) → cài Web Deploy → start web.

GUI:

- GUI build phải chạy trên nền tảng có hỗ trợ GUI. GUI sẽ hiển thị các nút tuần tự và chỉ bật nút tiếp theo khi marker tương ứng tồn tại.

## Triển khai / Packaging releases

### Phương pháp 1: Tự động với GitHub Actions (Khuyến nghị)

Repository đã được cấu hình với GitHub Actions workflow để tự động build và release khi bạn push tag:

```bash
# Tạo và push tag version
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# GitHub Actions sẽ tự động:
# 1. Build tất cả Java modules (Agent, Manager, ManagerWeb)
# 2. Build Control Panels (CLI)
# 3. Tạo packages cho các nền tảng
# 4. Tạo GitHub Release với artifacts
```

Workflow sẽ tạo release tự động với các file zip sẵn sàng để tải về.

### Phương pháp 2: Script thủ công (Local)

#### A. Automation script đầy đủ (build + đẩy lên GitHub)

Script `scripts/release_to_github.sh` tự động hoá toàn bộ quy trình:

**Yêu cầu:**
- GitHub CLI (`gh`) đã cài đặt và authenticate:
  ```bash
  brew install gh
  gh auth login
  ```
- Maven, Go, zip

**Sử dụng:**

```bash
# Release bình thường
./scripts/release_to_github.sh v1.0.0

# Tạo draft release (để review trước khi publish)
./scripts/release_to_github.sh v1.0.1 --draft

# Đánh dấu là pre-release
./scripts/release_to_github.sh v1.0.2-beta --prerelease
```

Script này sẽ:
1. Build tất cả modules (Agent, Manager, ManagerWeb)
2. Build Control Panels (CLI + GUI nếu có thể)
3. Copy artifacts vào Deploy/
4. Chạy `create_releases.sh` để tạo packages
5. Tạo git tag
6. Push tag lên GitHub
7. Tạo GitHub Release và upload tất cả .zip files

#### B. Chỉ tạo packages local (không push lên GitHub)

Script: `scripts/create_releases.sh`

```bash
cd <repo-root>
scripts/create_releases.sh 0.0.1
# Kết quả: Releases/0.0.1/*.zip và Releases/current_version.txt cập nhật
```

Script này sẽ tìm các binary trong `ControlPanel/*` và các script trong `Deploy/*` để tạo gói theo nền tảng. Nếu một số artifact GUI không tồn tại trên nền tảng hiện tại, script vẫn tiếp tục và báo missing items.

## Chạy deployment (Deploy scripts)

Thư mục `Deploy/` chứa các script platform-specific để cài đặt và chạy dịch vụ.

Ví dụ (Unix/macOS):

- Cài Java (nếu cần): `Deploy/Manager/install_java.sh`
- Chạy Manager CLI: `Deploy/Manager/run_manager_cli.sh`
- Chạy Manager GUI: `Deploy/Manager/run_manager_gui.sh`
- Cài deploy web (Jetty + WAR): `Deploy/Manager/install_deploy.sh`
- Start Jetty server: `Deploy/Manager/start_web.sh`

Trên Windows tương ứng là các file `.bat` cùng tên.

Lưu ý: ControlPanel gọi những script này. Bạn cũng có thể chạy trực tiếp nếu muốn kiểm soát chặt chẽ hơn.

## Ví dụ chạy release đã đóng gói

1) Giải nén package (ví dụ cho Manager trên macOS):

```bash
unzip Releases/0.0.1/Manager-mac-arm64-0.0.1.zip -d /opt/pbl4/manager
cd /opt/pbl4/manager
./install_java.sh     # nếu cần
./run_manager_cli.sh  # hoặc run_manager_gui.sh
```

2) Trên Windows: chạy tương ứng `.bat` scripts bằng Command Prompt hoặc PowerShell.

## Xử lý sự cố (Troubleshooting)

- Build GUI thất bại khi cross-compile: do Fyne/OpenGL native bindings. Nếu gặp lỗi khi build GUI cho nền tảng khác, build GUI trực tiếp trên nền tảng đó hoặc chỉ phân phối bản CLI.
- Marker/PID file không bị xóa sau stop: kiểm tra script/permissions. Marker files nằm cùng thư mục với executable — xóa thủ công nếu cần.
- Web app không khởi động: kiểm tra log đầu ra của `start_web.sh` (hoặc Jetty/Tomcat output). Đảm bảo `ManagerWeb.war` nằm trong thư mục chờ deploy.
- Java không detect: chạy `java -version` trên máy để đảm bảo PATH và JAVA_HOME đúng.

## Góp phần & phát triển mở rộng

- Nếu bạn muốn đóng góp, fork repo và gửi PR. Tuân thủ `PRE_GIT_CHECKLIST.md` trước khi push.
- Thêm tests cho CLI workflows (đơn vị/integration) sẽ giúp tự động hoá QA cho ControlPanel.

## Liên hệ

- Người duy trì: antialberteinstein
- Repo: PBL4-refactor (branch mặc định: main)

---

Nếu bạn muốn, tôi có thể bổ sung:

- Hướng dẫn chi tiết để cross-compile GUI cho từng nền tảng (macOS/Linux/Windows).
- Các script systemd/service plist example để chạy Manager/Agent như dịch vụ nền trên Linux/macOS.
- Thêm README cụ thể cho `ControlPanel/` (cách build release cho Agent/Manager control panels).

Chọn mục bạn muốn thêm và tôi sẽ cập nhật ngay. 

### Ports Used

| Component | Port | Protocol | Purpose |
|-----------|------|----------|---------|
| Agent UDP | 5000 | UDP | Receive commands from Manager |
| Agent TCP | 4000 | TCP | (Reserved for future use) |
| Manager UDP | 6000 | UDP | Send commands to Agents |
| Manager TCP | 17000 | TCP | (Reserved for future use) |
| External Scan | 8888 | TCP | ManagerWeb integration |
| ManagerWeb | 8080 | HTTP | Web interface (Jetty default) |

### Firewall Configuration

For proper operation, ensure these ports are open:
```bash
# On machines running Agent
sudo ufw allow 5000/udp

# On machine running Manager
sudo ufw allow 6000/udp
sudo ufw allow 8888/tcp
sudo ufw allow 8080/tcp  # If running ManagerWeb
```

## 🌍 Internationalization (i18n)

Supported languages:
- **English** (default)
- **Vietnamese**

**Change language:**
1. Manager GUI: File → Settings → Select language → Save
2. ManagerWeb: Settings → Select language → Save
3. Both applications share the same language preference via `config.json`

## 📊 CLI Commands Reference

See `Manager/README_CLI.md` for complete command reference.

**Quick examples:**
```bash
# Network scanning
manager> scan
manager> scan fast

# List agents
manager> list agents
manager> list agents -v

# View sessions
manager> list sessions
manager> list sessions -n 50 -m 00:11:22:33:44:55

# Process monitoring
manager> list processes --mac 00:11:22:33:44:55

# Agent selection
manager> use 00:11:22:33:44:55
agent[00:11:22:33:44:55]> show sessions
```

## 🧪 Testing

### Unit Tests
```bash
cd Manager
mvn test
```

### Manual Testing Checklist

**Agent:**
- [ ] Starts without errors
- [ ] Responds to Manager scans
- [ ] Sends session data correctly

**Manager CLI:**
- [ ] Scan discovers agents
- [ ] Can list agents/sessions/processes
- [ ] Auto-completion works
- [ ] Database persists data

**Manager GUI:**
- [ ] Charts update in real-time
- [ ] Settings save to config.json
- [ ] macOS menu bar appears correctly

**ManagerWeb:**
- [ ] Login works
- [ ] Charts render correctly
- [ ] Scan trigger works
- [ ] Config auto-reload works (change in Manager GUI)

## 🔒 Security Considerations

⚠️ **Important Security Notes:**

1. **Authentication:**
   - Default credentials are `admin/admin`
   - Change immediately after deployment
   - Passwords are SHA-256 hashed

2. **Database Separation:**
   - User credentials stored in separate `auth.db`
   - Can apply different backup/encryption strategies

3. **Network:**
   - No encryption on UDP communication (LAN only)
   - Consider VPN for WAN deployments

4. **Configuration:**
   - Config file contains database paths
   - Protect `~/PBL4DATA/` directory permissions

## 🐛 Troubleshooting

### Common Issues

**Agent not discovered:**
```bash
# Check UDP port is not blocked
netstat -an | grep 5000

# Check broadcast is working
# On Manager machine:
java -jar Manager.jar -c "scan fast"
```

**Database locked:**
```bash
# Stop all Manager/ManagerWeb instances
# Remove lock:
rm ~/PBL4DATA/*.db-shm ~/PBL4DATA/*.db-wal
```

**ManagerWeb can't connect to Manager:**
```bash
# Check External Scan Server is running
netstat -an | grep 8888

# Test connection:
telnet localhost 8888
SCAN
```

**Config not auto-reloading:**
```bash
# Check ManagerWeb logs for:
[ConfigReloadService] Started monitoring config file

# Force reload:
curl -X POST http://localhost:8080/ManagerWeb/admin/reload-config
```

## 📚 Documentation

### Detailed Guides

- **CLI Usage:** `Manager/README_CLI.md`
- **Architecture:** `Manager/ARCHITECTURE.md`
- **Command Mode:** `Manager/COMMAND_MODE_GUIDE.md`
- **GUI Features:** `Manager/GUI_FEATURES.md`
- **Config System:** `Manager/CONFIG_SYSTEM_IMPLEMENTATION.md`
- **Web Shared Config:** `ManagerWeb/SHARED_CONFIG_IMPLEMENTATION.md`
- **Auto-Reload:** `ManagerWeb/AUTO_RELOAD_CONFIG.md`

### Quick References

- **Command Mode:** `Manager/COMMAND_MODE_QUICKREF.md`
- **Server Mode:** `Manager/SERVER_MODE_QUICKREF.md`
- **Auto-Reload:** `ManagerWeb/AUTO_RELOAD_QUICKREF.md`

## 🛠️ Development

### Build System
- **Maven 3.6+**
- Java 8 target compatibility
- All dependencies managed in `pom.xml`

### Key Dependencies

**Manager:**
- Gson 2.10.1 (JSON config)
- SQLite JDBC 3.45.1.0
- JFreeChart 1.5.4 (Charts)
- JLine3 3.24.1 (CLI)

**Agent:**
- OSHI 6.4.6 (System monitoring)

**ManagerWeb:**
- Jakarta Servlet API 5.0.0
- Gson 2.10.1
- SQLite JDBC 3.41.2.2

### Code Style
- **Indentation:** 4 spaces
- **Comments:** Javadoc for public methods
- **SOLID principles** enforced
- See individual files for detailed documentation

## 📝 Version History

### v1.0.0 (November 2025)
- ✅ Initial release
- ✅ Agent, Manager, ManagerWeb modules
- ✅ CLI with auto-completion
- ✅ GUI with charts
- ✅ Web dashboard
- ✅ JSON configuration system
- ✅ Database separation (auth.db)
- ✅ Auto-reload configuration
- ✅ Bilingual support (EN/VI)

## 👥 Contributors

- **Team:** PBL4 Group
- **Institution:** Da Nang University of Technology (DUT)
- **Year:** 2025

## 📄 License

Educational project for PBL4 course at DUT University.

---

**Note:** This is an educational project. For production use, consider:
- Implementing HTTPS for ManagerWeb
- Adding encryption for network communication
- Implementing proper authentication tokens
- Adding comprehensive logging
- Setting up monitoring and alerting
---

**For detailed documentation, see:**
- 📖 [CLI Usage Guide](Manager/README_CLI.md)
- 🏗️ [Architecture Overview](Manager/ARCHITECTURE.md)  
- ⚙️ [Configuration System](Manager/CONFIG_SYSTEM_IMPLEMENTATION.md)
- 🌐 [Web Interface Guide](ManagerWeb/README.md)
- 🔄 [Auto-Reload Feature](ManagerWeb/AUTO_RELOAD_CONFIG.md)
