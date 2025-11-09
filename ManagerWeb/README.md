# ManagerWeb - Web Monitoring Dashboard

Web-based monitoring dashboard cho hệ thống Manager. Cung cấp giao diện trực quan để giám sát các agent và hiển thị dữ liệu real-time thông qua biểu đồ.

## 🎯 Tính năng chính

### 1. Dashboard Tổng quan
- **Grid View**: Hiển thị tất cả agents dưới dạng card grid
- **Scan Button**: Trigger network scan qua TCP connection (localhost:8888)
- **Real-time Updates**: Tự động refresh danh sách agents mỗi 5 giây
- **Agent Info**: Hiển thị thông tin chi tiết: hostname, IP, MAC, CPU, RAM, OS

### 2. Agent Detail Page
- **Thông tin chi tiết**: Specs đầy đủ của agent (CPU, RAM, OS, manufacturer, etc.)
- **Biểu đồ real-time**:
  - CPU Usage Chart (Line/Bar/Area)
  - RAM Usage Chart (Line/Bar/Area)
  - Combined Chart với zoom/pan capability
- **Process List**: Danh sách processes đang chạy với CPU & RAM usage
- **Auto-refresh**: Cập nhật dữ liệu mỗi 2 giây

### 3. Chart Features
- **Multiple chart types**: Line, Bar, Area
- **Time-series**: Hiển thị dữ liệu theo thời gian với hover tooltip
- **Zoom & Pan**: Zoom in/out và scroll qua historical data
- **Real-time updates**: Tự động thêm data points mới
- **Max 100 data points**: Tự động xóa old data để maintain performance

## 🏗️ Kiến trúc

### Backend Architecture
```
WebCommandMode (Service Layer)
    ├── Scan Command → TCP Client (localhost:8888) → Manager's ExternalScanServer
    └── Query Commands → Database Repositories → SQLite Database

Servlets (Controller Layer)
    ├── ScanServlet (/api/scan) - POST
    ├── AgentListServlet (/api/agents) - GET
    ├── SessionDataServlet (/api/sessions) - GET
    └── ProcessListServlet (/api/processes) - GET
```

### Frontend Architecture
```
JSP Pages
    ├── index.jsp (Dashboard với agent grid)
    └── agent-detail.jsp (Detail page với charts)

JavaScript
    ├── dashboard.js (Agent grid rendering & scan)
    └── agent-detail.js (Charts với Chart.js & real-time updates)

CSS
    └── style.css (Codeforces-inspired design)
```

## 📋 Yêu cầu

### Runtime Requirements
- **Java**: JDK 8 hoặc cao hơn
- **Servlet Container**: Tomcat 9.x, Jetty 9.x, hoặc tương đương
- **Database**: SQLite (shared với Manager)
- **Manager Instance**: Phải có Manager running với ExternalScanServer (port 8888)

### Dependencies (cần thêm vào classpath)
```xml
<!-- Servlet API -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>

<!-- GSON for JSON -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>

<!-- SQLite JDBC -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.41.2.2</version>
</dependency>
```

## 🚀 Deployment

### 1. Build WAR file
```bash
# Nếu có Maven/Gradle setup
mvn clean package
# hoặc
gradle build
```

### 2. Deploy to Servlet Container
```bash
# Copy WAR file to Tomcat webapps
cp ManagerWeb.war /path/to/tomcat/webapps/

# hoặc deploy qua Tomcat Manager
# http://localhost:8080/manager/html
```

### 3. Access Web Interface
```
http://localhost:8080/ManagerWeb/
```

## 📊 API Endpoints

### GET /api/agents
Lấy danh sách tất cả agents.

**Response:**
```json
[
  {
    "macAddress": "BE:CC:D0:78:8A:C9",
    "hostname": "MacBook-Pro",
    "ipAddress": "192.168.1.169",
    "os": "Mac OS X",
    "architecture": "aarch64",
    "cpuName": "Apple M1",
    "cpuMaxFreq": 3200000000,
    "physicalCores": 8,
    "logicalCores": 8
  }
]
```

### POST /api/scan
Trigger network scan qua TCP connection.

**Response:**
```json
{
  "success": true,
  "message": "Scan request sent successfully"
}
```

### GET /api/sessions
Lấy session data cho biểu đồ.

**Parameters:**
- `mac` (required): MAC address của agent
- `limit` (optional): Số lượng sessions (default: all)
- `latest` (optional): `true` để chỉ lấy latest session

**Response:**
```json
[
  {
    "id": 1234,
    "macAddress": "BE:CC:D0:78:8A:C9",
    "cpuUsage": 25.5,
    "totalRam": 17179869184,
    "ramUsage": 10891116544,
    "timestamp": 1761835260576
  }
]
```

### GET /api/processes
Lấy danh sách processes cho một session.

**Parameters:**
- `sessionId` (required): Session ID

**Response:**
```json
[
  {
    "pid": 1234,
    "name": "Google Chrome",
    "cpuUsage": 15.5,
    "ramUsage": 524288000
  }
]
```

## 🎨 Design Philosophy

### Codeforces-Inspired Design
- **Clean & Minimal**: Tập trung vào content
- **Professional Color Scheme**: Blue primary color (#0055cc)
- **Grid Layout**: Responsive agent cards
- **Clear Typography**: Easy-to-read fonts
- **Smooth Animations**: Hover effects và transitions

### Responsive Design
- **Desktop**: Grid view với 3-4 columns
- **Tablet**: 2 columns
- **Mobile**: Single column với full-width cards

## 🔧 Configuration

### AppConfig.java
```java
public final String DATABASE_URL = "jdbc:sqlite:" + 
    System.getProperty("user.home") + "/PBL4DATA/manager.db";
public final int EXTERNAL_SCAN_PORT = 8888;
```

### Chart Configuration (agent-detail.js)
```javascript
const CONFIG = {
    updateInterval: 2000,   // Real-time update interval (ms)
    maxDataPoints: 100      // Max points in chart
};
```

## 📝 Coding Standards

Tuân thủ theo `globalrules.md`:
- ✅ **Dependency Injection**: WebCommandMode với DI pattern
- ✅ **SOLID Principles**: Single responsibility cho mỗi servlet
- ✅ **Clean Code**: Comments rõ ràng, tên biến có ý nghĩa
- ✅ **Error Handling**: Try-catch với user-friendly messages
- ✅ **Security**: HTML escaping để prevent XSS

## 🐛 Troubleshooting

### Scan không hoạt động
**Error:** "Cannot connect to Manager"

**Solution:**
- Đảm bảo Manager instance đang chạy
- Check port 8888 không bị block bởi firewall
- Verify ExternalScanServer đã start trong Manager

### Charts không hiển thị
**Issue:** Blank charts hoặc "No data"

**Solution:**
- Check database có sessions không: `sqlite3 ~/PBL4DATA/manager.db "SELECT COUNT(*) FROM session"`
- Verify API endpoint: http://localhost:8080/ManagerWeb/api/sessions?mac=XX:XX:XX:XX:XX:XX
- Check browser console cho errors

### Servlet errors (404/500)
**Issue:** "WebServlet cannot be resolved"

**Solution:**
- Add servlet-api dependency vào classpath
- Ensure servlet container đang chạy
- Check web.xml configuration

## 🔒 Security Considerations

- **XSS Protection**: HTML escaping trong JavaScript
- **SQL Injection**: PreparedStatements trong repositories
- **CORS**: Cấu hình CORS headers nếu cần
- **Session Management**: 30-minute timeout
- **HTTP-Only Cookies**: Prevent XSS attacks

## 📖 Usage Examples

### Monitoring Workflow
1. Access dashboard: `http://localhost:8080/ManagerWeb/`
2. Click "Trigger Scan" để discover agents
3. Wait 5 seconds cho danh sách agents update
4. Click vào agent card để xem details
5. Charts tự động update mỗi 2 giây
6. Switch chart types qua dropdown
7. Zoom/pan trên combined chart

### API Integration Example (JavaScript)
```javascript
// Get agents
fetch('/ManagerWeb/api/agents')
  .then(res => res.json())
  .then(agents => console.log(agents));

// Trigger scan
fetch('/ManagerWeb/api/scan', { method: 'POST' })
  .then(res => res.json())
  .then(result => console.log(result));

// Get latest session
fetch('/ManagerWeb/api/sessions?mac=XX:XX:XX:XX:XX:XX&latest=true')
  .then(res => res.json())
  .then(session => console.log(session));
```

## 📚 Related Documentation

- Manager CLI: `Manager/README_CLI.md`
- Command Mode: `Manager/COMMAND_MODE_GUIDE.md`
- Architecture: `Manager/ARCHITECTURE.md`
- Global Rules: `globalrules.md`

## 🎯 Future Enhancements

- [ ] WebSocket support cho real-time updates (thay vì polling)
- [ ] User authentication & authorization
- [ ] Export charts as images/PDF
- [ ] Historical data comparison
- [ ] Alert notifications
- [ ] Multi-language support
- [ ] Dark mode theme
- [ ] Advanced filtering & search

## 👨‍💻 Development Notes

### Code Structure
```
ManagerWeb/
├── src/main/java/
│   ├── config/AppConfig.java
│   ├── controller/
│   │   ├── ScanServlet.java
│   │   ├── AgentListServlet.java
│   │   ├── SessionDataServlet.java
│   │   └── ProcessListServlet.java
│   ├── model/
│   │   ├── entity/
│   │   │   ├── Computer.java
│   │   │   ├── Session.java
│   │   │   └── Process.java
│   │   └── repository/
│   │       ├── DatabaseRepository.java
│   │       ├── ComputerRepository.java
│   │       ├── SessionRepository.java
│   │       └── ProcessRepository.java
│   ├── service/
│   │   └── WebCommandMode.java
│   └── util/
│       └── Logger.java
└── src/main/webapp/
    ├── WEB-INF/
    │   └── web.xml
    ├── css/
    │   └── style.css
    ├── js/
    │   ├── dashboard.js
    │   └── agent-detail.js
    ├── index.jsp
    ├── agent-detail.jsp
    ├── error-404.jsp
    └── error-500.jsp
```

### Key Design Decisions
- **Chart.js**: Chosen over D3.js cho simplicity và ease of use
- **Polling vs WebSocket**: Polling cho simplicity (WebSocket có thể add later)
- **JSP over SPA**: Server-side rendering cho better SEO và simplicity
- **Bootstrap 5**: Modern, responsive, và easy to customize
- **Shared Database**: Reuse Manager's SQLite database (no duplication)

---

**Version:** 1.0.0  
**Last Updated:** October 31, 2025  
**Author:** PBL4 Team
