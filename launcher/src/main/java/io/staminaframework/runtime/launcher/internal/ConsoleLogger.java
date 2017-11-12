/*
 * Copyright (c) 2017 Stamina Framework developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.staminaframework.runtime.launcher.internal;

import io.staminaframework.runtime.launcher.Logger;

import java.util.function.Supplier;

/**
 * {@link Logger} implementation, using <code>System.err</code> to log entries.
 *
 * @author Stamina Framework developers
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
