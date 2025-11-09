# PBL4 - System Monitoring Project

Multi-agent system monitoring application với real-time data collection và visualization.

## 📋 Tổng quan

Project bao gồm 3 modules chính:

### 1. **Agent** 
Thu thập thông tin hệ thống (CPU, RAM, processes) và gửi về Manager.

**Chức năng:**
- Theo dõi CPU usage real-time
- Theo dõi RAM usage
- Liệt kê các processes đang chạy
- Gửi dữ liệu on-demand đến Manager qua UDP

**Technologies:** Java, OSHI library

### 2. **Manager**
Quản lý nhiều agents, lưu trữ dữ liệu, cung cấp CLI/GUI interface.

**Chức năng:**
- Tự động discover agents trên network
- Thu thập và lưu trữ dữ liệu từ agents (SQLite)
- Interactive CLI với auto-completion
- GUI interface để giám sát
- External scan server cho tích hợp với ManagerWeb

**Technologies:** Java, SQLite, JLine3 (CLI), Swing (GUI)

### 3. **ManagerWeb**
Web dashboard để giám sát agents qua browser.

**Chức năng:**
- Dashboard hiển thị tất cả agents
- Real-time charts (CPU, RAM usage)
- Process monitoring
- Trigger network scans qua web UI

**Technologies:** Java Servlets, JSP, Chart.js, jQuery

## 🚀 Build & Run

### Prerequisites
- JDK 8 hoặc cao hơn
- Maven 3.x
- SQLite3 (tự động cài qua Maven dependencies)

### Build tất cả modules

```bash
# Build Agent
cd Agent
mvn clean package
# Output: Agent/target/Agent.jar

# Build Manager
cd Manager
mvn clean package
# Output: Manager/target/Manager.jar

# Build ManagerWeb
cd ManagerWeb
mvn clean package
# Output: ManagerWeb/target/ManagerWeb.war
```

### Run

#### Agent
```bash
# CLI mode
java -jar Agent/target/Agent.jar

# GUI mode
java -jar Agent/target/Agent.jar --gui
```

#### Manager
```bash
# Interactive CLI mode
java -jar Manager/target/Manager.jar

# GUI mode
java -jar Manager/target/Manager.jar --gui

# Command mode (single command)
java -jar Manager/target/Manager.jar -c "list agents"
java -jar Manager/target/Manager.jar -c scan
```

#### ManagerWeb
```bash
# Deploy WAR file to Tomcat/Jetty
# hoặc sử dụng Maven Jetty plugin:
cd ManagerWeb
mvn jetty:run
# Access: http://localhost:8080/ManagerWeb
```

## 📁 Cấu trúc Project

```
PBL4_refactor/
├── globalrules.md          # Coding standards và guidelines
├── .gitignore              # Git ignore rules
├── Agent/                  # Agent module
│   ├── src/main/java/
│   │   ├── AgentMain.java
│   │   ├── config/
│   │   ├── model/
│   │   ├── service/
│   │   └── util/
│   └── pom.xml
├── Manager/                # Manager module
│   ├── src/main/java/
│   │   ├── ManagerMain.java
│   │   ├── cli/
│   │   ├── config/
│   │   ├── database/
│   │   ├── model/
│   │   ├── service/
│   │   └── util/
│   ├── external_scan_client.py
│   ├── README_CLI.md
│   ├── ARCHITECTURE.md
│   ├── CLI_IMPLEMENTATION.md
│   ├── GUI_FEATURES.md
│   └── pom.xml
└── ManagerWeb/             # Web interface module
    ├── src/main/
    │   ├── java/
    │   │   ├── controller/
    │   │   ├── model/
    │   │   ├── service/
    │   │   └── config/
    │   └── webapp/
    │       ├── index.jsp
    │       ├── agent-detail.jsp
    │       ├── css/
    │       └── js/
    ├── README.md
    ├── DEPENDENCIES.md
    └── pom.xml
```

## 🔧 Kiến trúc

### Communication Flow
```
[Agent] ←--UDP--> [Manager] ←--TCP:8888--> [ManagerWeb]
                      ↓
                  [SQLite DB]
```

### Design Patterns
- **Dependency Injection**: Tất cả dependencies được inject qua constructor
- **Repository Pattern**: Database access layer (ComputerRepository, SessionRepository, etc.)
- **Observer Pattern**: AgentDiscoveryListener cho network scanning
- **MVC**: ManagerWeb sử dụng Servlet (Controller) + JSP (View) + Repository (Model)

## 📚 Documentation

- **Manager CLI Guide**: `Manager/README_CLI.md`
- **Manager Architecture**: `Manager/ARCHITECTURE.md`
- **ManagerWeb Guide**: `ManagerWeb/README.md`
- **Coding Standards**: `globalrules.md`

## ⚙️ Configuration

Mỗi module có file `config/AppConfig.java` để cấu hình:

- **Agent**: UDP port, data collection intervals
- **Manager**: Database path, network scan settings, CLI/GUI options
- **ManagerWeb**: Database path, servlet settings

## 🧪 Development Guidelines

Xem `globalrules.md` để biết chi tiết về:
- SOLID principles
- Dependency Injection requirements
- Code formatting standards
- Comment conventions
- Testing requirements

## 📝 License

Educational project - PBL4, DUT University
