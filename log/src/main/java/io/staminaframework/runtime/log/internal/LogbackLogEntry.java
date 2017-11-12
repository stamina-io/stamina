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

package io.staminaframework.runtime.log.internal;

import ch.qos.logback.classic.Level;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

import java.util.HashMap;
import java.util.Map;

class LogbackLogEntry implements LogEntry {
    private static final Map<Level, Integer> LEVELS = new HashMap<>(6);

    static {
        LEVELS.put(Level.DEBUG, LogService.LOG_DEBUG);
        LEVELS.put(Level.INFO, LogService.LOG_INFO);
        LEVELS.put(Level.WARN, LogService.LOG_WARNING);
        LEVELS.put(Level.ERROR, LogService.LOG_ERROR);
        LEVELS.put(Level.TRACE, LogService.LOG_DEBUG);
        LEVELS.put(Level.ALL, LogService.LOG_DEBUG);
    }

    private final int level;
    private final long time;
    private final String message;

    public LogbackLogEntry(final String message, final long timestamp, final Level level) {
        this.time = timestamp;
        this.level = LEVELS.getOrDefault(level, LogService.LOG_DEBUG);
        this.message = message;
    }

    @Override
    public Bundle getBundle() {
        return null;
    }

    @Override
    public ServiceReference getServiceReference() {
        return null;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Throwable getException() {
        return null;
    }

    @Override
    public long getTime() {
        return time;
    }
}
