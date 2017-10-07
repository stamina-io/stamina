/*
 * Copyright (c) 2017 Stamina developers.
 * All rights reserved.
 */

package io.stamina.boot;

import java.util.function.Supplier;

/**
 * Application logger.
 *
 * @author Stamina developers
 */
public interface Logger {
    /**
     * Log an entry at DEBUG level.
     */
    void debug(Supplier<String> msg);

    /**
     * Log an entry at INFO level.
     */
    void info(Supplier<String> msg);

    /**
     * Log an entry at WARN level.
     */
    void warn(Supplier<String> msg);

    /**
     * Log an entry at FATAL level, and then halt current process.
     */
    void fatal(Supplier<String> msg, Throwable cause);
}
