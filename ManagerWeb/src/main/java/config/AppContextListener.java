package config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import model.repository.DatabaseRepository;
import model.repository.AuthRepository;
import model.repository.ComputerRepository;
import model.repository.SessionRepository;
import model.repository.ProcessRepository;
import service.WebCommandMode;
import service.ConfigReloadService;
import util.Messages;

/**
 * Application Context Listener
 * Initializes shared instances of repositories and services once at application startup
 * and stores them in ServletContext for all servlets to use.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        
        System.out.println("=== Initializing ManagerWeb Application ===");
        
        // 1. Load AppConfig from shared config.json
        AppConfig appConfig = ConfigManager.loadConfig();
        context.setAttribute("appConfig", appConfig);
        System.out.println("✓ AppConfig loaded from: " + ConfigManager.getConfigFilePath());
        
        // 1.5. Initialize language
        Messages.setLanguage(appConfig.getLanguage());
        System.out.println("✓ Language set to: " + appConfig.getLanguage());
        
        // 2. Initialize Main DatabaseRepository
        DatabaseRepository databaseRepository = new DatabaseRepository(appConfig.getDatabaseUrl());
        databaseRepository.initializeDatabase(); // Create tables
        context.setAttribute("databaseRepository", databaseRepository);
        System.out.println("✓ DatabaseRepository initialized with: " + appConfig.getDatabaseUrl());
        
        // 3. Initialize Auth DatabaseRepository (separate database)
        DatabaseRepository authDatabaseRepository = new DatabaseRepository(appConfig.getAuthDatabaseUrl());
        authDatabaseRepository.initializeDatabase(); // Create users table
        AuthRepository authRepository = new AuthRepository(authDatabaseRepository);
        authRepository.initializeDefaultAdmin(); // Create admin/admin if not exists
        context.setAttribute("authRepository", authRepository);
        System.out.println("✓ AuthRepository initialized with: " + appConfig.getAuthDatabaseUrl());
        
        // 4. Initialize Repositories (shared)
        ComputerRepository computerRepository = new ComputerRepository(databaseRepository);
        ProcessRepository processRepository = new ProcessRepository(databaseRepository);
        SessionRepository sessionRepository = new SessionRepository(databaseRepository, processRepository);
        
        context.setAttribute("computerRepository", computerRepository);
        context.setAttribute("sessionRepository", sessionRepository);
        context.setAttribute("processRepository", processRepository);
        System.out.println("✓ Repositories initialized (Computer, Session, Process)");
        
        // 4. Initialize WebCommandMode service (shared)
        WebCommandMode webCommandMode = new WebCommandMode(
            appConfig,
            computerRepository,
            sessionRepository,
            processRepository
        );
        context.setAttribute("webCommandMode", webCommandMode);
        System.out.println("✓ WebCommandMode service initialized");
        
        // 5. Initialize ConfigReloadService (monitors config.json for changes)
        ConfigReloadService configReloadService = new ConfigReloadService(appConfig);
        configReloadService.setConfigChangeListener(newConfig -> {
            // Update appConfig in context when config file changes
            context.setAttribute("appConfig", newConfig);
            System.out.println("✓ AppConfig updated in ServletContext (language: " + newConfig.getLanguage() + ")");
        });
        configReloadService.start();
        context.setAttribute("configReloadService", configReloadService);
        System.out.println("✓ ConfigReloadService started (monitors ~/PBL4DATA/config.json)");
        
        System.out.println("=== ManagerWeb Application Ready ===");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("=== Shutting down ManagerWeb Application ===");
        
        // Stop ConfigReloadService
        ServletContext context = sce.getServletContext();
        ConfigReloadService configReloadService = (ConfigReloadService) context.getAttribute("configReloadService");
        if (configReloadService != null) {
            configReloadService.stop();
        }
        
        System.out.println("✓ Cleanup completed");
    }
}
