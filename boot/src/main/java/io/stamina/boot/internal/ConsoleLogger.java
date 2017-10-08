/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.boot.internal;

import io.stamina.boot.Logger;

import java.util.function.Supplier;

/**
 * {@link Logger} implementation, using <code>System.err</code> to log entries.
 *
 * @author Stamina developers
 */
public class ConsoleLogger implements Logger {
    public static final int DEBUG_LEVEL = 0;
    public static final int INFO_LEVEL = 1;
    public static final int WARN_LEVEL = 2;
    public static final int FATAL_LEVEL = 3;

    private final int level;

    public ConsoleLogger() {
        this(WARN_LEVEL);
    }

    public ConsoleLogger(final int level) {
        this.level = Math.max(level, DEBUG_LEVEL);
    }

    private void log(int level, String msg, Throwable cause) {
        if (msg != null) {
            final StringBuilder buf = new StringBuilder(64);
            switch (level) {
                case DEBUG_LEVEL:
                    buf.append("[DEBUG] ");
                    break;
                case INFO_LEVEL:
                    buf.append("[INFO ] ");
                    break;
                case WARN_LEVEL:
                    buf.append("[WARN ] ");
                    break;
                case FATAL_LEVEL:
                    buf.append("[FATAL] ");
                    break;
            }
            buf.append(msg);
            System.err.println(buf);
        }
        if (cause != null) {
            cause.printStackTrace(System.err);
        }
        if (level == FATAL_LEVEL) {
            System.exit(1);
        }
    }

    @Override
    public void debug(Supplier<String> msg) {
        if (level <= DEBUG_LEVEL) {
            log(DEBUG_LEVEL, msg.get(), null);
        }
    }

    @Override
    public void info(Supplier<String> msg) {
        if (level <= INFO_LEVEL) {
            log(INFO_LEVEL, msg.get(), null);
        }
    }

    @Override
    public void warn(Supplier<String> msg) {
        if (level <= WARN_LEVEL) {
            log(WARN_LEVEL, msg.get(), null);
        }
    }

    @Override
    public void fatal(Supplier<String> msg, Throwable cause) {
        if (level <= FATAL_LEVEL) {
            log(FATAL_LEVEL, msg.get(), cause);
        }
    }
}
