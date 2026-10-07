package com.cyberheist.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lightweight, zero-dependency logging utility for Cyber Heist.
 * Provides leveled console output with timestamps and class context.
 */
public final class Logger {

    public enum Level {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3);

        private final int priority;

        Level(int priority) {
            this.priority = priority;
        }

        public boolean isLoggable(Level currentLevel) {
            return this.priority >= currentLevel.priority;
        }
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static Level minLevel = Level.DEBUG;

    private final String context;

    private Logger(String context) {
        this.context = context;
    }

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz.getSimpleName());
    }

    public static Logger getLogger(String context) {
        return new Logger(context);
    }

    public static void setMinLevel(Level level) {
        minLevel = level;
    }

    public static Level getMinLevel() {
        return minLevel;
    }

    public void debug(String message) {
        log(Level.DEBUG, message, null);
    }

    public void info(String message) {
        log(Level.INFO, message, null);
    }

    public void warn(String message) {
        log(Level.WARN, message, null);
    }

    public void error(String message) {
        log(Level.ERROR, message, null);
    }

    public void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    private void log(Level level, String message, Throwable throwable) {
        if (!level.isLoggable(minLevel)) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String threadName = Thread.currentThread().getName();
        String formatted = String.format("[%s] [%-5s] [%s] [%s] %s",
                timestamp, level.name(), threadName, context, message);

        if (level == Level.ERROR) {
            System.err.println(formatted);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        } else {
            System.out.println(formatted);
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }
}
