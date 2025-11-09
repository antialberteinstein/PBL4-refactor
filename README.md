# PBL4 - System Monitoring Project

A comprehensive multi-agent system monitoring application with real-time data collection, storage, and visualization across CLI, GUI, and Web interfaces.

## 📋 Overview

This project implements a distributed system monitoring solution consisting of three main components that work together to collect, store, and visualize system metrics from multiple computers on a network.

### Architecture Overview

```
┌─────────────┐         ┌──────────────┐         ┌──────────────┐
│   Agent     │ ◄─UDP──►│   Manager    │◄──HTTP──┤ ManagerWeb   │
│  (Monitor)  │         │ (Collector)  │         │  (Dashboard) │
└─────────────┘         └──────┬───────┘         └──────────────┘
                               │
                        ┌──────▼───────┐
                        │  SQLite DB   │
                        │ manager.db   │
                        │  auth.db     │
                        └──────────────┘
```

## 🎯 Features

### Agent Module
**System Resource Monitoring**
- Real-time CPU usage tracking with OSHI library
- Memory (RAM) usage monitoring
- Active process listing with details (PID, name, CPU%, memory)
- Automatic data reporting to Manager
- Both CLI and GUI modes

**Key Capabilities:**
- ✅ On-demand data collection via UDP protocol
- ✅ Network auto-discovery support
- ✅ Lightweight background operation
- ✅ Cross-platform compatibility (Windows, macOS, Linux)

### Manager Module
**Central Management & Storage**

**CLI Mode Features:**
- Interactive command-line interface with JLine3
- Auto-completion for commands and parameters
- Syntax highlighting
- Command history
- Extensive command set (see `Manager/README_CLI.md`)

**GUI Mode Features:**
- Real-time agent monitoring dashboard
- CPU/RAM usage charts with JFreeChart
- Process list visualization
- Network scanning interface
- Settings dialog (language selection)
- macOS native menu bar support

**Core Functions:**
- ✅ Automatic agent discovery via broadcast
- ✅ SQLite database storage (separate auth & data databases)
- ✅ Session management with pagination
- ✅ Authentication system (username/password)
- ✅ External scan server (TCP:8888) for ManagerWeb integration
- ✅ Bilingual support (English/Vietnamese)
- ✅ JSON-based configuration system

### ManagerWeb Module
**Web Dashboard**
- Browser-based monitoring interface
- Real-time charts (Chart.js) for CPU/RAM trends
- Agent overview with status indicators
- Session history with filtering
- Process monitoring per agent
- Web-based network scan trigger
- Settings page for configuration

**Technical Features:**
- ✅ Jakarta Servlets + JSP architecture
- ✅ AJAX updates for real-time data
- ✅ Responsive design (Bootstrap-like CSS)
- ✅ Authentication with session management
- ✅ Shared configuration with Manager GUI
- ✅ Auto-reload configuration (hot-reload)

## 🚀 Quick Start

### Prerequisites
- **JDK 8+** (tested with JDK 8, 11, 17)
- **Maven 3.6+**
- **Network connectivity** (for agent-manager communication)

### Build All Modules

```bash
# Clone repository
git clone <repository-url>
cd PBL4_refactor

# Build Agent
cd Agent
mvn clean package
# Output: target/Agent.jar

# Build Manager
cd ../Manager
mvn clean package
# Output: target/Manager.jar

# Build ManagerWeb
cd ../ManagerWeb
mvn clean package
# Output: target/ManagerWeb.war
```

### Running the System

#### 1. Start Manager (Required First)

**GUI Mode (Recommended):**
```bash
cd Manager
java -jar target/Manager.jar --gui
```

**CLI Mode:**
```bash
java -jar target/Manager.jar
manager> help
manager> scan
manager> list agents
```

**Command Mode (Single Command):**
```bash
java -jar target/Manager.jar -c "scan"
java -jar target/Manager.jar -c "list sessions -n 10"
```

#### 2. Start Agent(s)

**On target machines to monitor:**
```bash
cd Agent
java -jar target/Agent.jar
# or GUI mode:
java -jar target/Agent.jar --gui
```

#### 3. Start ManagerWeb (Optional)

**Using Jetty Maven Plugin:**
```bash
cd ManagerWeb
mvn jetty:run
# Access: http://localhost:8080/ManagerWeb
# Login: admin/admin (default)
```

**Using Tomcat:**
```bash
cp target/ManagerWeb.war $CATALINA_HOME/webapps/
# Access: http://localhost:8080/ManagerWeb
```

## 📁 Project Structure

```
PBL4_refactor/
├── .gitignore                    # Git ignore configuration
├── README.md                     # This file
├── PRE_GIT_CHECKLIST.md         # Code quality checklist
│
├── Agent/                        # Agent module
│   ├── src/main/java/
│   │   ├── AgentMain.java       # Entry point
│   │   ├── config/
│   │   │   └── AppConfig.java   # Configuration
│   │   ├── model/
│   │   │   ├── Computer.java    # Computer model
│   │   │   ├── Session.java     # Session model
│   │   │   └── Process.java     # Process model
│   │   ├── service/
│   │   │   ├── SessionRetriever.java    # Data collection
│   │   │   ├── NetworkMessageService.java
│   │   │   └── ...
│   │   ├── ui/
│   │   │   └── AgentWindow.java # GUI
│   │   └── util/
│   │       ├── Logger.java
│   │       └── ProtocolManager.java
│   ├── pom.xml                  # Maven config
│   └── REMOVED_DATABASE.md      # Architecture doc
│
├── Manager/                      # Manager module
│   ├── src/main/java/
│   │   ├── ManagerMain.java     # Entry point
│   │   ├── cli/
│   │   │   ├── CliInterface.java
│   │   │   ├── CommandMode.java
│   │   │   ├── CliCompleter.java     # Auto-completion
│   │   │   └── CliHighlighter.java   # Syntax highlighting
│   │   ├── config/
│   │   │   ├── AppConfig.java        # Configuration
│   │   │   └── ConfigManager.java    # JSON config management
│   │   ├── database/
│   │   │   ├── DatabaseManager.java
│   │   │   ├── AuthRepository.java   # Authentication
│   │   │   ├── ComputerManager.java
│   │   │   ├── SessionManager.java
│   │   │   └── ProcessManager.java
│   │   ├── model/
│   │   │   ├── Computer.java
│   │   │   ├── Session.java
│   │   │   ├── Process.java
│   │   │   └── User.java
│   │   ├── service/
│   │   │   ├── HostScanner.java
│   │   │   ├── SessionRetriever.java
│   │   │   ├── ExternalScanServer.java
│   │   │   └── ...
│   │   ├── ui/
│   │   │   ├── AgentWindow.java      # Main GUI
│   │   │   ├── ChartPanel.java       # Charts
│   │   │   └── SettingsDialog.java   # Settings
│   │   └── util/
│   │       ├── Messages.java         # i18n
│   │       └── Logger.java
│   ├── pom.xml
│   ├── README_CLI.md                 # CLI documentation
│   ├── ARCHITECTURE.md               # Architecture overview
│   ├── CONFIG_SYSTEM_IMPLEMENTATION.md
│   ├── COMMAND_MODE_GUIDE.md
│   ├── GUI_FEATURES.md
│   └── external_scan_client.py       # Python client example
│
└── ManagerWeb/                   # Web interface module
    ├── src/main/
    │   ├── java/
    │   │   ├── config/
    │   │   │   ├── AppConfig.java
    │   │   │   ├── ConfigManager.java
    │   │   │   └── AppContextListener.java
    │   │   ├── controller/
    │   │   │   ├── LoginServlet.java
    │   │   │   ├── ScanServlet.java
    │   │   │   ├── SettingsServlet.java
    │   │   │   └── ReloadConfigServlet.java
    │   │   ├── model/
    │   │   │   ├── entity/          # Data models
    │   │   │   └── repository/      # Database access
    │   │   ├── service/
    │   │   │   ├── WebCommandMode.java
    │   │   │   └── ConfigReloadService.java
    │   │   └── util/
    │   │       └── Messages.java    # i18n
    │   └── webapp/
    │       ├── index.jsp            # Dashboard
    │       ├── agent-detail.jsp     # Agent details
    │       ├── login.jsp
    │       ├── settings.jsp
    │       ├── css/                 # Stylesheets
    │       ├── js/                  # JavaScript
    │       └── WEB-INF/
    │           └── web.xml
    ├── pom.xml
    ├── README.md
    ├── SHARED_CONFIG_IMPLEMENTATION.md
    ├── AUTO_RELOAD_CONFIG.md
    └── AUTO_RELOAD_QUICKREF.md
```

## 🔧 Technical Architecture

### Communication Protocols

**Agent ↔ Manager (UDP)**
- Port: 5000 (configurable)
- Protocol: Custom text-based protocol
- Messages: HELLO, GET_SESSIONS, SESSION_DATA
- Auto-discovery via broadcast

**Manager ↔ ManagerWeb (TCP)**
- Port: 8888 (External Scan Server)
- Protocol: Simple text commands
- Commands: SCAN, STATUS
- Used for triggering network scans from web

### Database Schema

**manager.db** (Main database)
```sql
-- Agents/Computers
CREATE TABLE computers (
    mac_address TEXT PRIMARY KEY,
    ip_address TEXT,
    computer_name TEXT,
    os TEXT,
    cpu_name TEXT,
    total_ram REAL,
    last_seen DATETIME
);

-- Sessions (CPU/RAM snapshots)
CREATE TABLE sessions (
    id INTEGER PRIMARY KEY,
    mac_address TEXT,
    cpu_usage REAL,
    ram_usage REAL,
    timestamp DATETIME,
    FOREIGN KEY(mac_address) REFERENCES computers(mac_address)
);

-- Processes
CREATE TABLE processes (
    id INTEGER PRIMARY KEY,
    session_id INTEGER,
    pid INTEGER,
    name TEXT,
    cpu_percent REAL,
    memory_mb REAL,
    FOREIGN KEY(session_id) REFERENCES sessions(id)
);
```

**auth.db** (Authentication database - separated for security)
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    username TEXT UNIQUE,
    password_hash TEXT,
    created_at DATETIME,
    last_login DATETIME
);
```

### Design Patterns

1. **Dependency Injection**
   - Constructor-based injection throughout
   - No static dependencies
   - Example: `DatabaseManager` injected into repositories

2. **Repository Pattern**
   - Separation of data access logic
   - `ComputerManager`, `SessionManager`, `ProcessManager`
   - Clean interface for database operations

3. **Service Layer**
   - Business logic separated from UI
   - `HostScanner`, `SessionRetriever`, `AuthService`

4. **MVC (Web)**
   - Servlets (Controller)
   - JSP (View)
   - Repository/Model (Model)

5. **Observer Pattern**
   - `AgentDiscoveryListener` for network events
   - Config change notifications in `ConfigReloadService`

## ⚙️ Configuration System

### Shared Configuration

Both Manager and ManagerWeb share the same configuration file:

**Location:** `~/PBL4DATA/config.json`

**Example:**
```json
{
  "databaseUrl": "jdbc:sqlite:~/PBL4DATA/manager.db",
  "authDatabaseUrl": "jdbc:sqlite:~/PBL4DATA/auth.db",
  "agentUdpPort": 5000,
  "agentTcpPort": 4000,
  "managerUdpPort": 6000,
  "managerTcpPort": 17000,
  "externalScanPort": 8888,
  "sessionRetrievingDelayMs": 50,
  "language": "en"
}
```

### Auto-Reload Feature (ManagerWeb)

ManagerWeb automatically monitors `config.json` and reloads configuration when changed by Manager GUI:

- Check interval: 5 seconds
- Automatic language synchronization
- No restart required
- See `ManagerWeb/AUTO_RELOAD_CONFIG.md` for details

### Default Credentials

**Manager/ManagerWeb:**
- Username: `admin`
- Password: `admin`
- ⚠️ Change after first login in production!

## 🌐 Network Configuration

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
