package util;

import java.util.HashMap;
import java.util.Map;

/**
 * Internationalization (i18n) support for ManagerWeb application.
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
        
        // Authentication
        en.put("auth.login", "Login");
        en.put("auth.logout", "Logout");
        en.put("auth.username", "Username");
        en.put("auth.password", "Password");
        en.put("auth.title", "Manager - Login");
        en.put("auth.header", "Login to Manager");
        en.put("auth.invalid", "Invalid username or password");
        en.put("auth.welcome", "Welcome back!");
        en.put("auth.required", "Please login to continue");
        
        // Navigation
        en.put("nav.home", "Home");
        en.put("nav.dashboard", "Dashboard");
        en.put("nav.agents", "Agents");
        en.put("nav.sessions", "Sessions");
        en.put("nav.settings", "Settings");
        en.put("nav.user", "User");
        en.put("nav.guest", "Guest");
        
        // Dashboard
        en.put("dashboard.title", "Manager - Monitoring Dashboard");
        en.put("dashboard.network.scan", "Network Scan Control");
        en.put("dashboard.trigger.scan", "Trigger Scan");
        en.put("dashboard.agents.title", "Agents");
        en.put("dashboard.loading", "Loading agents...");
        en.put("dashboard.loading.text", "Loading...");
        
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
        en.put("agent.details", "Agent Details");
        en.put("agent.no.agents", "No agents found");
        en.put("agent.view", "View Details");
        
        // Agent Details Page
        en.put("agent.details.title", "Agent Details - Manager");
        en.put("agent.back.dashboard", "Back to Dashboard");
        en.put("agent.date", "Date");
        en.put("agent.slot.size", "Slot Size");
        en.put("agent.time.slot", "Time Slot");
        en.put("agent.realtime", "Real-time");
        en.put("agent.previous", "Previous");
        en.put("agent.next", "Next");
        en.put("agent.cpu.usage", "CPU Usage");
        en.put("agent.ram.usage", "RAM Usage");
        en.put("agent.running.processes", "Running Processes");
        en.put("agent.line.chart", "Line Chart");
        en.put("agent.histogram", "Histogram");
        en.put("agent.top.chart", "Top Chart");
        en.put("agent.table.view", "Table View");
        en.put("agent.search", "Search by name or PID...");
        en.put("agent.sort", "Sort by");
        en.put("agent.cpu.high.low", "CPU (High to Low)");
        en.put("agent.cpu.low.high", "CPU (Low to High)");
        en.put("agent.ram.high.low", "RAM (High to Low)");
        en.put("agent.ram.low.high", "RAM (Low to High)");
        en.put("agent.name.asc", "Name (A-Z)");
        en.put("agent.name.desc", "Name (Z-A)");
        
        // Session Management
        en.put("session.list", "Session List");
        en.put("session.id", "Session ID");
        en.put("session.agent", "Agent");
        en.put("session.start", "Start Time");
        en.put("session.end", "End Time");
        en.put("session.duration", "Duration");
        en.put("session.active", "Active");
        en.put("session.ended", "Ended");
        en.put("session.view", "View Details");
        en.put("session.no.sessions", "No sessions found");
        
        // Performance Charts
        en.put("chart.cpu", "CPU Usage");
        en.put("chart.ram", "RAM Usage");
        en.put("chart.disk", "Disk Usage");
        en.put("chart.network", "Network Usage");
        en.put("chart.time", "Time");
        en.put("chart.percentage", "Percentage (%)");
        en.put("chart.loading", "Loading chart data...");
        
        // Common
        en.put("common.search", "Search");
        en.put("common.filter", "Filter");
        en.put("common.refresh", "Refresh");
        en.put("common.loading", "Loading...");
        en.put("common.error", "Error");
        en.put("common.success", "Success");
        en.put("common.info", "Information");
        en.put("common.back", "Back");
        en.put("common.next", "Next");
        en.put("common.previous", "Previous");
        en.put("common.save", "Save");
        en.put("common.cancel", "Cancel");
        
        // Settings
        en.put("settings.title", "Settings");
        en.put("settings.language", "Language");
        en.put("settings.language.changed", "Language Changed");
        en.put("settings.restart.required", "Please refresh the page for language changes to take effect.");
        en.put("settings.select.language", "Select your preferred language");
        
        // Error Messages
        en.put("error.connection", "Connection error");
        en.put("error.database", "Database error");
        en.put("error.general", "An error occurred");
        en.put("error.not.found", "Page not found");
        en.put("error.unauthorized", "Unauthorized access");
        
        translations.put("en", en);
        
        // Vietnamese translations
        Map<String, String> vi = new HashMap<>();
        vi.put("app.title", "Quản Lý Giám Sát Hệ Thống");
        vi.put("app.name", "Quản Lý");
        
        // Authentication
        vi.put("auth.login", "Đăng nhập");
        vi.put("auth.logout", "Đăng xuất");
        vi.put("auth.username", "Tên đăng nhập");
        vi.put("auth.password", "Mật khẩu");
        vi.put("auth.title", "Quản Lý - Đăng Nhập");
        vi.put("auth.header", "Đăng Nhập Hệ Thống");
        vi.put("auth.invalid", "Tên đăng nhập hoặc mật khẩu không đúng");
        vi.put("auth.welcome", "Chào mừng trở lại!");
        vi.put("auth.required", "Vui lòng đăng nhập để tiếp tục");
        
        // Navigation
        vi.put("nav.home", "Trang chủ");
        vi.put("nav.dashboard", "Bảng điều khiển");
        vi.put("nav.agents", "Agents");
        vi.put("nav.sessions", "Phiên làm việc");
        vi.put("nav.settings", "Cài đặt");
        vi.put("nav.user", "Người dùng");
        vi.put("nav.guest", "Khách");
        
        // Dashboard
        vi.put("dashboard.title", "Quản Lý - Bảng Điều Khiển Giám Sát");
        vi.put("dashboard.network.scan", "Điều Khiển Quét Mạng");
        vi.put("dashboard.trigger.scan", "Kích Hoạt Quét");
        vi.put("dashboard.agents.title", "Các Agent");
        vi.put("dashboard.loading", "Đang tải agents...");
        vi.put("dashboard.loading.text", "Đang tải...");
        
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
        vi.put("agent.details", "Chi Tiết Agent");
        vi.put("agent.no.agents", "Không tìm thấy agent nào");
        vi.put("agent.view", "Xem chi tiết");
        
        // Agent Details Page
        vi.put("agent.details.title", "Chi Tiết Agent - Quản Lý");
        vi.put("agent.back.dashboard", "Quay Lại Trang Chủ");
        vi.put("agent.date", "Ngày");
        vi.put("agent.slot.size", "Kích Thước Khe");
        vi.put("agent.time.slot", "Khe Thời Gian");
        vi.put("agent.realtime", "Thời gian thực");
        vi.put("agent.previous", "Trước");
        vi.put("agent.next", "Tiếp");
        vi.put("agent.cpu.usage", "Sử Dụng CPU");
        vi.put("agent.ram.usage", "Sử Dụng RAM");
        vi.put("agent.running.processes", "Các Tiến Trình Đang Chạy");
        vi.put("agent.line.chart", "Biểu Đồ Đường");
        vi.put("agent.histogram", "Biểu Đồ Cột");
        vi.put("agent.top.chart", "Biểu Đồ Top");
        vi.put("agent.table.view", "Chế Độ Bảng");
        vi.put("agent.search", "Tìm theo tên hoặc PID...");
        vi.put("agent.sort", "Sắp xếp");
        vi.put("agent.cpu.high.low", "CPU (Cao xuống Thấp)");
        vi.put("agent.cpu.low.high", "CPU (Thấp lên Cao)");
        vi.put("agent.ram.high.low", "RAM (Cao xuống Thấp)");
        vi.put("agent.ram.low.high", "RAM (Thấp lên Cao)");
        vi.put("agent.name.asc", "Tên (A-Z)");
        vi.put("agent.name.desc", "Tên (Z-A)");
        
        // Session Management
        vi.put("session.list", "Danh Sách Phiên");
        vi.put("session.id", "Mã Phiên");
        vi.put("session.agent", "Agent");
        vi.put("session.start", "Thời gian bắt đầu");
        vi.put("session.end", "Thời gian kết thúc");
        vi.put("session.duration", "Thời lượng");
        vi.put("session.active", "Đang hoạt động");
        vi.put("session.ended", "Đã kết thúc");
        vi.put("session.view", "Xem chi tiết");
        vi.put("session.no.sessions", "Không tìm thấy phiên nào");
        
        // Performance Charts
        vi.put("chart.cpu", "Sử Dụng CPU");
        vi.put("chart.ram", "Sử Dụng RAM");
        vi.put("chart.disk", "Sử Dụng Đĩa");
        vi.put("chart.network", "Sử Dụng Mạng");
        vi.put("chart.time", "Thời gian");
        vi.put("chart.percentage", "Phần trăm (%)");
        vi.put("chart.loading", "Đang tải dữ liệu biểu đồ...");
        
        // Common
        vi.put("common.search", "Tìm kiếm");
        vi.put("common.filter", "Lọc");
        vi.put("common.refresh", "Làm mới");
        vi.put("common.loading", "Đang tải...");
        vi.put("common.error", "Lỗi");
        vi.put("common.success", "Thành công");
        vi.put("common.info", "Thông tin");
        vi.put("common.back", "Quay lại");
        vi.put("common.next", "Tiếp theo");
        vi.put("common.previous", "Trước đó");
        vi.put("common.save", "Lưu");
        vi.put("common.cancel", "Hủy");
        
        // Settings
        vi.put("settings.title", "Cài Đặt");
        vi.put("settings.language", "Ngôn ngữ");
        vi.put("settings.language.changed", "Đã Thay Đổi Ngôn Ngữ");
        vi.put("settings.restart.required", "Vui lòng tải lại trang để áp dụng thay đổi ngôn ngữ.");
        vi.put("settings.select.language", "Chọn ngôn ngữ ưa thích của bạn");
        
        // Error Messages
        vi.put("error.connection", "Lỗi kết nối");
        vi.put("error.database", "Lỗi cơ sở dữ liệu");
        vi.put("error.general", "Đã xảy ra lỗi");
        vi.put("error.not.found", "Không tìm thấy trang");
        vi.put("error.unauthorized", "Truy cập không được phép");
        
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
