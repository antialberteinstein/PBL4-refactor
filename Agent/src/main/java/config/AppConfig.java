package config;

public class AppConfig {
    public final String DATABASE_URL = "jdbc:sqlite:agent_data.db";

    public final int AGENT_UDP_PORT = 5000;
    // Interval for chart/UI refresh in milliseconds
    public final int CHART_REFRESH_MILLIS = 1000; // 1 second by default
    
}
