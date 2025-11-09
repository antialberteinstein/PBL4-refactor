package util;

import java.util.HashMap;
import java.util.Map;

/**
 * Internationalization (i18n) support for Manager application.
 * Provides text messages in multiple languages.
 */
public class Messages {
    
    private static String currentLanguage = "en"; // Default language
    private static final Map<String, Map<String, String>> translations = new HashMap<>();
    
    static {
        initializeTranslations();
    }
    
    /**
     * Initialize all translations.
     */
    private static void initializeTranslations() {
        // English translations
        Map<String, String> en = new HashMap<>();
        en.put("app.title", "System Monitor Manager");
        en.put("app.name", "Manager");
        
        // Authentication - GUI
        en.put("auth.login", "Login");
        en.put("auth.logout", "Logout");
        en.put("auth.username", "Username");
        en.put("auth.password", "Password");
        en.put("auth.cancel", "Cancel");
        en.put("auth.title", "Manager - Login");
        en.put("auth.failed", "Authentication Failed");
        en.put("auth.gui.header", "Manager Authentication");
        en.put("auth.gui.invalid", "Invalid credentials.");
        en.put("auth.gui.empty.username", "Username cannot be empty");
        en.put("auth.gui.empty.password", "Password cannot be empty");
        
        // Authentication - CLI
        en.put("auth.cli.header", "Manager Authentication");
        en.put("auth.cli.invalid", "Invalid credentials.");
        en.put("auth.cli.empty.username", "Username cannot be empty");
        en.put("auth.cli.success", "Login successful!");
        en.put("auth.cli.prompt.username", "Username (or 'exit' to quit): ");
        en.put("auth.cli.prompt.password", "Password: ");
        
        // Main Window
        en.put("window.title", "Manager - System Monitoring Dashboard");
        en.put("window.actions", "Actions");
        en.put("window.scan.refresh", "Scan & Refresh");
        en.put("window.scan.tooltip", "Scan network for new Agents and refresh list");
        en.put("window.scanning", "Scanning...");
        en.put("window.clear.log", "Clear Log");
        en.put("window.agent.details", "Agent Details");
        en.put("window.select.agent", "Select an Agent to view details");
        en.put("window.discovered.agents", "Discovered Agents");
        en.put("window.activity.log", "Activity Log");
        en.put("window.process.list", "Process List");
        en.put("window.no.processes", "No processes loaded");
        en.put("window.cpu.usage", "CPU Usage");
        en.put("window.ram.usage", "RAM Usage");
        
        // Menu
        en.put("menu.file", "File");
        en.put("menu.file.settings", "Settings...");
        en.put("menu.file.exit", "Exit");
        
        en.put("menu.view", "View");
        en.put("menu.view.refresh", "Refresh All");
        en.put("menu.view.clear.log", "Clear Log");
        en.put("menu.view.agents", "Show Agents");
        en.put("menu.view.sessions", "Show Sessions");
        
        en.put("menu.tools", "Tools");
        en.put("menu.tools.scan", "Scan Network");
        en.put("menu.tools.database", "Database Manager");
        en.put("menu.tools.export", "Export Data...");
        en.put("menu.tools.import", "Import Data...");
        
        en.put("menu.help", "Help");
        en.put("menu.help.documentation", "Documentation");
        en.put("menu.help.about", "About Manager");
        en.put("menu.help.version", "Version Info");
        
        // Legacy menu items (for compatibility)
        en.put("menu.exit", "Exit");
        en.put("menu.refresh", "Refresh");
        en.put("menu.settings", "Settings");
        en.put("menu.about", "About");
        
        // Agent Management
        en.put("agent.list", "Agent List");
        en.put("agent.id", "Agent ID");
        en.put("agent.hostname", "Hostname");
        en.put("agent.ip", "IP Address");
        en.put("agent.os", "Operating System");
        en.put("agent.status", "Status");
        en.put("agent.last.seen", "Last Seen");
        en.put("agent.online", "Online");
        en.put("agent.offline", "Offline");
        en.put("agent.connect", "Connect");
        en.put("agent.disconnect", "Disconnect");
        en.put("agent.scan", "Scan");
        en.put("agent.details", "Agent Details");
        en.put("agent.no.selection", "No agent selected");
        
        // Session Management
        en.put("session.list", "Session List");
        en.put("session.id", "Session ID");
        en.put("session.agent", "Agent");
        en.put("session.start", "Start Time");
        en.put("session.end", "End Time");
        en.put("session.duration", "Duration");
        en.put("session.active", "Active");
        en.put("session.ended", "Ended");
        en.put("session.view", "View Session");
        en.put("session.delete", "Delete Session");
        
        // CLI Commands
        en.put("cli.welcome", "Welcome to System Monitor Manager");
        en.put("cli.mode.select", "Select mode:");
        en.put("cli.mode.interactive", "Interactive Mode");
        en.put("cli.mode.gui", "GUI Mode");
        en.put("cli.mode.server", "Server Mode");
        en.put("cli.mode.command", "Command Mode");
        en.put("cli.prompt", "manager> ");
        en.put("cli.command.help", "Available commands:");
        en.put("cli.command.exit", "Exit the application");
        en.put("cli.invalid.command", "Invalid command. Type 'help' for available commands.");
        en.put("cli.goodbye", "Goodbye!");
        
        // CLI Interactive - Error Messages
        en.put("cli.error.no.agent", "Error: No Agent selected. Use 'use agent <MAC>' first.");
        en.put("cli.error.no.session", "Error: No Session selected. Use 'use session <ID>' first.");
        en.put("cli.error.no.agent.or.session", "Error: No Session or Agent selected. Use 'use session <ID>' or 'use agent <MAC>'");
        en.put("cli.error.invalid.mac", "Error: Invalid MAC address format: %s");
        en.put("cli.error.invalid.session.id", "Error: Invalid session ID: %s");
        en.put("cli.error.invalid.limit", "Error: Invalid limit value. Must be a positive number.");
        en.put("cli.error.unknown.command", "Unknown command: '%s'. Type 'help' for available commands.");
        en.put("cli.error.unknown.list.target", "Unknown list target: '%s'. Type 'help' for available commands.");
        en.put("cli.error.agent.not.found", "Error: Agent not found: %s");
        en.put("cli.error.session.not.found", "Error: Session not found: %d");
        en.put("cli.error.command.failed", "Error: %s");
        
        // CLI Interactive - Success/Info Messages
        en.put("cli.success.context.cleared", "Context cleared.");
        en.put("cli.success.agent.selected", "Selected Agent: %s");
        en.put("cli.success.session.selected", "Selected Session: %d (Agent: %s)");
        en.put("cli.info.scanning", "Scanning network for Agents...\nCheck discovered Agents with: list agents");
        en.put("cli.info.no.agents", "No Agents discovered yet. Run 'scan' to discover Agents.");
        en.put("cli.info.no.sessions", "No sessions found for Agent: %s");
        en.put("cli.info.no.processes", "No processes found for Session: %d");
        en.put("cli.info.showing.top.processes", "Showing top %d processes by resource usage:");
        en.put("cli.info.showing.top.agents", "Showing first %d agents:");
        en.put("cli.info.showing.latest.sessions", "Showing latest %d sessions:");
        
        // CLI Interactive - Help Text
        en.put("cli.help.title", "=== Manager CLI Commands ===");
        en.put("cli.help.discovery.title", "Discovery:");
        en.put("cli.help.scan", "Scan network for Agents");
        en.put("cli.help.list.agents", "Show all discovered Agents with IDs");
        en.put("cli.help.context.title", "Context-based commands (MySQL-style):");
        en.put("cli.help.use.agent", "Select an Agent (by ID or MAC)");
        en.put("cli.help.use.session", "Select a Session");
        en.put("cli.help.list.sessions.context", "List sessions (requires agent context)");
        en.put("cli.help.list.processes.context", "List processes (requires session OR agent context)");
        en.put("cli.help.clear", "Clear context");
        en.put("cli.help.explicit.title", "Explicit commands:");
        en.put("cli.help.list.sessions.agent", "List sessions for specific Agent");
        en.put("cli.help.list.sessions.limit", "Limit number of sessions shown");
        en.put("cli.help.list.processes.session", "List processes for specific Session");
        en.put("cli.help.list.processes.agent", "List latest processes for specific Agent");
        en.put("cli.help.list.current", "Show current processes (uses agent context)");
        en.put("cli.help.list.current.agent", "Show current processes for specific Agent");
        en.put("cli.help.head.title", "Head commands (show first N results):");
        en.put("cli.help.head.agents", "Show first N agents");
        en.put("cli.help.head.sessions", "Show latest N sessions (requires agent context)");
        en.put("cli.help.head.sessions.agent", "Show latest N sessions for specific Agent");
        en.put("cli.help.head.processes", "Show top N processes by resource usage");
        en.put("cli.help.head.processes.session", "Show top N processes for specific Session");
        en.put("cli.help.head.processes.agent", "Show top N processes for specific Agent");
        en.put("cli.help.tail.title", "Tail commands (show last N results):");
        en.put("cli.help.tail.agents", "Show last N agents");
        en.put("cli.help.tail.sessions", "Show oldest N sessions (requires agent context)");
        en.put("cli.help.tail.sessions.agent", "Show oldest N sessions for specific Agent");
        en.put("cli.help.tail.processes", "Show bottom N processes (lowest resource usage)");
        en.put("cli.help.tail.processes.session", "Show bottom N processes for specific Session");
        en.put("cli.help.tail.processes.agent", "Show bottom N processes for specific Agent");
        en.put("cli.help.pagination.title", "Pagination & Range commands:");
        en.put("cli.help.list.sessions.range", "Show sessions from index A to B");
        en.put("cli.help.list.sessions.offset", "Show M sessions starting from offset N");
        en.put("cli.help.list.sessions.limit.only", "Show N most recent sessions");
        en.put("cli.help.system.title", "System:");
        en.put("cli.help.status", "Show Manager status");
        en.put("cli.help.help", "Show this help message");
        en.put("cli.help.exit", "Exit Manager");
        en.put("cli.help.note", "Note: You can use either Agent ID (e.g., 1, 2, 3) or MAC address.");
        en.put("cli.help.note.detail", "      Run 'list agents' to see IDs assigned to each agent.");
        
        // CLI Interactive - Examples
        en.put("cli.examples.title", "Examples:");
        en.put("cli.examples.use.id", "Use agent by ID");
        en.put("cli.examples.use.mac", "Or use MAC address");
        en.put("cli.examples.list.sessions", "Shows up to 100, with pagination info");
        en.put("cli.examples.tail.sessions", "Show oldest 10");
        en.put("cli.examples.tail.processes", "Lowest resource usage");
        
        // CLI Interactive - Status
        en.put("cli.status.title", "=== Manager Status ===");
        en.put("cli.status.context", "Context");
        en.put("cli.status.session", "Session %d");
        en.put("cli.status.agent", "Agent %s");
        en.put("cli.status.none", "None");
        
        // CLI Command Mode
        en.put("cli.cmd.scan.success", "✓ Scan request sent successfully");
        en.put("cli.cmd.scan.progress", "  Network scan is now in progress on the Manager instance");
        en.put("cli.cmd.scan.failed", "✗ Scan request failed: %s");
        en.put("cli.cmd.scan.unexpected", "✗ Unexpected response from Manager: %s");
        en.put("cli.cmd.error.no.response", "Error: No response from Manager");
        en.put("cli.cmd.error.cannot.connect", "✗ Cannot connect to Manager");
        en.put("cli.cmd.error.make.sure", "  Make sure a Manager instance is running in interactive or GUI mode");
        en.put("cli.cmd.error.start.manager", "  Start Manager with: java ManagerMain --verbose");
        en.put("cli.cmd.error.timeout", "✗ Connection timeout");
        en.put("cli.cmd.error.not.responding", "  Manager is not responding on port %d");
        en.put("cli.cmd.error.scan", "✗ Error executing scan command: %s");
        en.put("cli.cmd.error.command", "Error executing command: %s");
        en.put("cli.cmd.error.empty", "Error: Command cannot be empty");
        en.put("cli.cmd.usage.title", "Command Mode Usage:");
        en.put("cli.cmd.usage.format1", "  java ManagerMain -c <command>");
        en.put("cli.cmd.usage.format2", "  java ManagerMain --command <command>");
        en.put("cli.cmd.commands.title", "Supported Commands:");
        en.put("cli.cmd.commands.scan", "  scan              - Trigger network scan (requires running Manager)");
        en.put("cli.cmd.commands.list.agents", "  list agents       - Show all discovered agents");
        en.put("cli.cmd.commands.list.sessions", "  list sessions     - Show all recorded sessions");
        en.put("cli.cmd.commands.list.processes", "  list processes    - Show all processes");
        en.put("cli.cmd.commands.help", "  help              - Show available commands");
        en.put("cli.cmd.commands.status", "  status            - Show system status");
        en.put("cli.cmd.examples.title", "Examples:");
        en.put("cli.cmd.examples.scan", "  java ManagerMain -c scan");
        en.put("cli.cmd.examples.list.agents", "  java ManagerMain -c \"list agents\"");
        en.put("cli.cmd.examples.list.sessions", "  java ManagerMain --command \"show sessions\"");
        en.put("cli.cmd.note.title", "Note:");
        en.put("cli.cmd.note.scan.requires", "  - SCAN command requires a running Manager instance (interactive or GUI mode)");
        en.put("cli.cmd.note.other.commands", "  - Other commands only require database access");
        
        // Main - Startup Messages
        en.put("main.usage.title", "Manager - System Monitoring Manager");
        en.put("main.usage.header", "Usage:");
        en.put("main.usage.interactive", "  java ManagerMain                         - Start in interactive CLI mode (quiet)");
        en.put("main.usage.gui", "  java ManagerMain --gui                   - Start in GUI mode");
        en.put("main.usage.verbose", "  java ManagerMain --verbose               - Interactive mode with logs");
        en.put("main.usage.command.short", "  java ManagerMain -c <command>            - Execute single command and exit");
        en.put("main.usage.command.long", "  java ManagerMain --command <command>     - Execute single command and exit");
        en.put("main.usage.help", "  java ManagerMain --help                  - Show this help");
        en.put("main.examples.interactive.title", "Interactive/GUI Mode Examples:");
        en.put("main.examples.interactive.cli", "  java ManagerMain                         - Interactive CLI (clean)");
        en.put("main.examples.interactive.gui", "  java ManagerMain --gui                   - Launch graphical interface");
        en.put("main.examples.interactive.gui.short", "  java ManagerMain -g                      - Launch GUI (short form)");
        en.put("main.examples.interactive.verbose", "  java ManagerMain --verbose               - Interactive with background logs");
        en.put("main.examples.command.title", "Command Mode Examples:");
        en.put("main.examples.command.scan", "  java ManagerMain -c scan                 - Trigger scan (requires running Manager)");
        en.put("main.examples.command.agents", "  java ManagerMain -c \"list agents\"        - Show all agents");
        en.put("main.examples.command.sessions", "  java ManagerMain --command \"show sessions\" - Show all sessions");
        en.put("main.examples.command.help", "  java ManagerMain -c help                 - Show available commands");
        en.put("main.notes.title", "Command Mode Notes:");
        en.put("main.notes.scan", "  - SCAN command requires a running Manager instance (interactive or GUI mode)");
        en.put("main.notes.other", "  - Other commands only require database access (no running Manager needed)");
        en.put("main.error.unknown.option", "Error: Unknown option '%s'");
        en.put("main.auth.failed", "Authentication failed. Exiting...");
        en.put("main.gui.starting", "Starting Manager in GUI mode...");
        en.put("main.gui.launched", "GUI launched successfully!");
        en.put("main.shutdown.error", "Error during shutdown: %s");
        
        // CLI Executor - Usage Messages
        en.put("cli.exec.usage.use", "Usage: use {agent|session} <id|MAC>");
        en.put("cli.exec.usage.list", "Usage: list {agents|sessions|processes|current}");
        en.put("cli.exec.usage.head", "Usage: head <N> {agents|sessions|processes} [for <ID>]");
        en.put("cli.exec.usage.tail", "Usage: tail <N> {agents|sessions|processes} [for <ID>]");
        
        // CLI Executor - Error Messages
        en.put("cli.exec.error.agent.not.found", "Error: Agent '%s' not found. Use 'list agents' to see available agents.");
        en.put("cli.exec.error.agent.not.in.db", "Error: Agent '%s' not found in database.");
        en.put("cli.exec.error.session.not.found", "Error: Session '%s' not found.");
        en.put("cli.exec.error.session.id.number", "Error: Session ID must be a number.");
        en.put("cli.exec.error.unknown.type", "Error: Unknown type '%s'. Use 'agent' or 'session'.");
        en.put("cli.exec.error.n.positive", "Error: N must be a positive number.");
        en.put("cli.exec.error.n.number", "Error: N must be a number.");
        en.put("cli.exec.error.invalid.offset", "Error: Invalid offset value. Must be a number.");
        en.put("cli.exec.error.invalid.range", "Error: Invalid range. 'from' must be >= 0 and <= 'to'.");
        en.put("cli.exec.error.invalid.range.values", "Error: Invalid range values. Must be numbers.");
        en.put("cli.exec.error.no.session.or.agent", "Error: No Session or Agent selected. Use 'use session <ID>' or 'use agent <ID|MAC>'");
        en.put("cli.exec.error.no.agent.for.current", "Error: No Agent selected. Use 'use agent <ID|MAC>' or 'list current for <ID|MAC>'");
        
        // CLI Executor - Success Messages
        en.put("cli.exec.using.agent.with.id", "Now using Agent #%d: %s (%s)");
        en.put("cli.exec.using.agent", "Now using Agent: %s (%s)");
        en.put("cli.exec.using.session", "Now using Session: %d");
        
        // Common
        en.put("common.yes", "Yes");
        en.put("common.no", "No");
        en.put("common.ok", "OK");
        en.put("common.cancel", "Cancel");
        en.put("common.apply", "Apply");
        en.put("common.close", "Close");
        en.put("common.save", "Save");
        en.put("common.delete", "Delete");
        en.put("common.edit", "Edit");
        en.put("common.add", "Add");
        en.put("common.remove", "Remove");
        en.put("common.search", "Search");
        en.put("common.filter", "Filter");
        en.put("common.loading", "Loading...");
        en.put("common.error", "Error");
        en.put("common.warning", "Warning");
        en.put("common.info", "Information");
        en.put("common.confirm", "Confirm");
        
        // Settings
        en.put("settings.language", "Language");
        en.put("settings.language.changed", "Language Changed");
        en.put("settings.restart.required", "Please restart the application for language changes to take effect.");
        
        // Error Messages
        en.put("error.connection", "Connection error");
        en.put("error.database", "Database error");
        en.put("error.general", "An error occurred");
        en.put("error.not.found", "Not found");
        
        translations.put("en", en);
        
        // Vietnamese translations
        Map<String, String> vi = new HashMap<>();
        vi.put("app.title", "Quản Lý Giám Sát Hệ Thống");
        vi.put("app.name", "Quản Lý");
        
        // Authentication - GUI only (CLI uses English)
        vi.put("auth.login", "Đăng nhập");
        vi.put("auth.logout", "Đăng xuất");
        vi.put("auth.username", "Tên đăng nhập");
        vi.put("auth.password", "Mật khẩu");
        vi.put("auth.cancel", "Hủy");
        vi.put("auth.title", "Quản Lý - Đăng Nhập");
        vi.put("auth.failed", "Xác Thực Thất Bại");
        vi.put("auth.gui.header", "Xác Thực Quản Lý");
        vi.put("auth.gui.invalid", "Thông tin đăng nhập không hợp lệ.");
        vi.put("auth.gui.empty.username", "Tên đăng nhập không được để trống");
        vi.put("auth.gui.empty.password", "Mật khẩu không được để trống");
        // Note: auth.cli.* keys not translated - CLI always uses English
        
        // Main Window
        vi.put("window.title", "Quản Lý - Bảng Điều Khiển Giám Sát Hệ Thống");
        vi.put("window.actions", "Thao tác");
        vi.put("window.scan.refresh", "Quét & Làm mới");
        vi.put("window.scan.tooltip", "Quét mạng để tìm Agent mới và làm mới danh sách");
        vi.put("window.scanning", "Đang quét...");
        vi.put("window.clear.log", "Xóa Nhật ký");
        vi.put("window.agent.details", "Chi Tiết Agent");
        vi.put("window.select.agent", "Chọn một Agent để xem chi tiết");
        vi.put("window.discovered.agents", "Các Agent Đã Phát Hiện");
        vi.put("window.activity.log", "Nhật Ký Hoạt Động");
        vi.put("window.process.list", "Danh Sách Tiến Trình");
        vi.put("window.no.processes", "Chưa tải tiến trình nào");
        vi.put("window.cpu.usage", "Sử Dụng CPU");
        vi.put("window.ram.usage", "Sử Dụng RAM");
        
        // Menu
        vi.put("menu.file", "Tệp");
        vi.put("menu.file.settings", "Cài đặt...");
        vi.put("menu.file.exit", "Thoát");
        
        vi.put("menu.view", "Xem");
        vi.put("menu.view.refresh", "Làm Mới Tất Cả");
        vi.put("menu.view.clear.log", "Xóa Nhật Ký");
        vi.put("menu.view.agents", "Hiện Agent");
        vi.put("menu.view.sessions", "Hiện Phiên");
        
        vi.put("menu.tools", "Công cụ");
        vi.put("menu.tools.scan", "Quét Mạng");
        vi.put("menu.tools.database", "Quản Lý Database");
        vi.put("menu.tools.export", "Xuất Dữ Liệu...");
        vi.put("menu.tools.import", "Nhập Dữ Liệu...");
        
        vi.put("menu.help", "Trợ giúp");
        vi.put("menu.help.documentation", "Tài Liệu");
        vi.put("menu.help.about", "Giới Thiệu Manager");
        vi.put("menu.help.version", "Thông Tin Phiên Bản");
        
        // Legacy menu items (for compatibility)
        vi.put("menu.exit", "Thoát");
        vi.put("menu.refresh", "Làm mới");
        vi.put("menu.settings", "Cài đặt");
        vi.put("menu.about", "Giới thiệu");
        
        // Agent Management
        vi.put("agent.list", "Danh Sách Agent");
        vi.put("agent.id", "Mã Agent");
        vi.put("agent.hostname", "Tên máy");
        vi.put("agent.ip", "Địa chỉ IP");
        vi.put("agent.os", "Hệ điều hành");
        vi.put("agent.status", "Trạng thái");
        vi.put("agent.last.seen", "Lần cuối truy cập");
        vi.put("agent.online", "Trực tuyến");
        vi.put("agent.offline", "Ngoại tuyến");
        vi.put("agent.connect", "Kết nối");
        vi.put("agent.disconnect", "Ngắt kết nối");
        vi.put("agent.scan", "Quét");
        vi.put("agent.details", "Chi Tiết Agent");
        vi.put("agent.no.selection", "Chưa chọn agent");
        
        // Session Management
        vi.put("session.list", "Danh Sách Phiên");
        vi.put("session.id", "Mã Phiên");
        vi.put("session.agent", "Agent");
        vi.put("session.start", "Thời gian bắt đầu");
        vi.put("session.end", "Thời gian kết thúc");
        vi.put("session.duration", "Thời lượng");
        vi.put("session.active", "Đang hoạt động");
        vi.put("session.ended", "Đã kết thúc");
        vi.put("session.view", "Xem Phiên");
        vi.put("session.delete", "Xóa Phiên");
        
        // Common
        vi.put("common.yes", "Có");
        vi.put("common.no", "Không");
        vi.put("common.ok", "Đồng ý");
        vi.put("common.cancel", "Hủy");
        vi.put("common.apply", "Áp dụng");
        vi.put("common.close", "Đóng");
        vi.put("common.save", "Lưu");
        vi.put("common.delete", "Xóa");
        vi.put("common.edit", "Sửa");
        vi.put("common.add", "Thêm");
        vi.put("common.remove", "Gỡ bỏ");
        vi.put("common.search", "Tìm kiếm");
        vi.put("common.filter", "Lọc");
        vi.put("common.loading", "Đang tải...");
        vi.put("common.error", "Lỗi");
        vi.put("common.warning", "Cảnh báo");
        vi.put("common.info", "Thông tin");
        vi.put("common.confirm", "Xác nhận");
        
        // Settings
        vi.put("settings.language", "Ngôn ngữ");
        vi.put("settings.language.changed", "Đã Thay Đổi Ngôn Ngữ");
        vi.put("settings.restart.required", "Vui lòng khởi động lại ứng dụng để áp dụng thay đổi ngôn ngữ.");
        
        // Error Messages
        vi.put("error.connection", "Lỗi kết nối");
        vi.put("error.database", "Lỗi cơ sở dữ liệu");
        vi.put("error.general", "Đã xảy ra lỗi");
        vi.put("error.not.found", "Không tìm thấy");
        
        translations.put("vi", vi);
    }
    
    /**
     * Set the current language.
     * 
     * @param language Language code ("en" or "vi")
     */
    public static void setLanguage(String language) {
        if (translations.containsKey(language)) {
            currentLanguage = language;
        }
    }
    
    /**
     * Get the current language.
     * 
     * @return Current language code
     */
    public static String getLanguage() {
        return currentLanguage;
    }
    
    /**
     * Get a translated message.
     * 
     * @param key Message key
     * @return Translated message, or key if not found
     */
    public static String get(String key) {
        Map<String, String> langMap = translations.get(currentLanguage);
        if (langMap != null && langMap.containsKey(key)) {
            return langMap.get(key);
        }
        // Fallback to English
        Map<String, String> enMap = translations.get("en");
        if (enMap != null && enMap.containsKey(key)) {
            return enMap.get(key);
        }
        // Return key if not found
        return key;
    }
    
    /**
     * Get a formatted message with parameters.
     * 
     * @param key Message key
     * @param params Parameters to insert into message
     * @return Formatted message
     */
    public static String get(String key, Object... params) {
        String message = get(key);
        return String.format(message, params);
    }
}
