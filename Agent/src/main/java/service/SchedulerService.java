package service;

import util.Logger;
import java.util.concurrent.*;

/**
 * Scheduler service for managing recurring tasks
 * Follows Single Responsibility Principle
 */
public class SchedulerService {

    private static final String COMPONENT = "SchedulerService";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /**
     * Chạy task lặp lại theo khoảng thời gian (ví dụ: mỗi 2 phút, mỗi 1 tiếng, ...)
     */
    public void scheduleRepeatingTask(Runnable task, long interval, TimeUnit unit) {
        long delay = 0; // bắt đầu ngay lập tức
        scheduler.scheduleAtFixedRate(task, delay, interval, unit);
        Logger.info(COMPONENT, "Task scheduled to repeat every " + interval + " " + unit.toString().toLowerCase());
    }

    public void shutdown() {
        Logger.info(COMPONENT, "Shutting down scheduler service");
        scheduler.shutdown();
    }
}
