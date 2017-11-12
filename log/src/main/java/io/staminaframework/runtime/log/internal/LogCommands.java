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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Log related commands.
 *
 * @author Stamina Framework developers
 */
class LogCommands {
    private final OsgiBridge bridge;

    public LogCommands(final OsgiBridge bridge) {
        this.bridge = bridge;
    }

    @Descriptor("Set log level for a logger")
    public void set(@Descriptor("logger name") String logger,
                    @Descriptor("logger level") String level) throws IOException {
        ((Logger) LoggerFactory.getLogger(logger)).setLevel(Level.toLevel(level));
    }

    @Descriptor("Get log level")
    public void get(CommandSession session,
                    @Descriptor("logger names") String... loggers) throws IOException {
        if (loggers == null || loggers.length == 0) {
            final List<String> loggerNames = new ArrayList<>(8);
            final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            for (final Logger logger : loggerContext.getLoggerList()) {
                if (logger.getLevel() != null) {
                    loggerNames.add(logger.getName());
                }
            }
            Collections.sort(loggerNames, LoggerNameComparator.INSTANCE);
            loggers = loggerNames.toArray(new String[loggerNames.size()]);
        }
        for (final String logger : loggers) {
            final String level = ((Logger) LoggerFactory.getLogger(logger)).getEffectiveLevel().toString();
            session.getConsole().println(logger + "=" + level);
        }
    }

    @Descriptor("Display last log entries")
    public void tail(CommandSession session) {
        final List<LogEntry> entries = Collections.list(bridge.getLog());
        Collections.sort(entries, LogEntryComparator.INSTANCE);

        final PrintStream out = session.getConsole();
        for (final LogEntry log : entries) {
            out.println(log.getMessage());

            final Throwable error = log.getException();
            if (error != null) {
                error.printStackTrace(out);
            }
        }
    }

    @Descriptor("Clear log entries stored in memory")
    public void clear() {
        bridge.clear();
    }

    private static String formatLevel(int level) {
        switch (level) {
            case LogService.LOG_DEBUG:
                return "DEBUG";
            case LogService.LOG_ERROR:
                return "ERROR";
            case LogService.LOG_WARNING:
                return "WARNING";
            case LogService.LOG_INFO:
            default:
                return "INFO";
        }
    }

    private static class LogEntryComparator implements Comparator<LogEntry> {
        public static final Comparator<LogEntry> INSTANCE = new LogEntryComparator();

        @Override
        public int compare(LogEntry o1, LogEntry o2) {
            final long t1 = o1.getTime();
            final long t2 = o2.getTime();
            if (t1 < t2) {
                return -1;
            }
            if (t1 > t2) {
                return 1;
            }
            return 0;
        }
    }

    private static class LoggerNameComparator implements Comparator<String> {
        public static final Comparator<String> INSTANCE = new LoggerNameComparator();

        @Override
        public int compare(String o1, String o2) {
            if (Logger.ROOT_LOGGER_NAME.equals(o1)) {
                return -1;
            }
            if (Logger.ROOT_LOGGER_NAME.equals(o2)) {
                return 1;
            }
            return o1.compareTo(o2);
        }
    }
}
