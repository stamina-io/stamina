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

package io.staminaframework.log.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.helpers.CyclicBuffer;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class OsgiBridge extends UnsynchronizedAppenderBase<ILoggingEvent> implements LogReaderService {
    private static final int BUFFER_CAPACITY = 50;
    private final CyclicBuffer buffer = new CyclicBuffer(BUFFER_CAPACITY);
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final PatternLayout patternLayout = new PatternLayout();
    private final Set<LogListener> logListeners = new HashSet<>(1);

    public OsgiBridge() {
        patternLayout.setPattern("%date{ISO8601} | %-5level - %msg");
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        patternLayout.setContext(context);
    }

    @Override
    public void start() {
        super.start();
        patternLayout.start();
    }

    @Override
    protected void append(ILoggingEvent e) {
        final String msg = patternLayout.doLayout(e);
        final BufferLogEntry bufferEntry = new BufferLogEntry(msg, e.getTimeStamp(), e.getLevel());

        rwl.writeLock().lock();
        try {
            buffer.add(bufferEntry);
        } finally {
            rwl.writeLock().unlock();
        }

        if (logListeners.isEmpty()) {
            return;
        }
        final LogEntry logEntry = new LogbackLogEntry(msg, e.getTimeStamp(), e.getLevel());
        for (final LogListener logListener : logListeners) {
            try {
                logListener.logged(logEntry);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void addLogListener(LogListener listener) {
        if (listener != null) {
            logListeners.add(listener);
        }
    }

    @Override
    public void removeLogListener(LogListener listener) {
        logListeners.remove(listener);
    }

    @Override
    public Enumeration getLog() {
        final List<LogEntry> events = new ArrayList<>(BUFFER_CAPACITY);
        rwl.readLock().lock();
        try {
            final int bufferSize = buffer.length();
            for (int i = 0; i < bufferSize; ++i) {
                final BufferLogEntry e = (BufferLogEntry) buffer.get(i);
                events.add(new LogbackLogEntry(e.message, e.timestamp, e.level));
            }
        } finally {
            rwl.readLock().unlock();
        }
        return Collections.enumeration(events);
    }

    public void clear() {
        rwl.writeLock().lock();
        try {
            buffer.clear();
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private static class BufferLogEntry {
        public final String message;
        public final long timestamp;
        public final Level level;

        public BufferLogEntry(final String message, final long timestamp, final Level level) {
            this.message = message;
            this.timestamp = timestamp;
            this.level = level;
        }
    }
}
