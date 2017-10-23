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


package io.staminaframework.boot.internal;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Log related commands.
 *
 * @author Stamina Framework developers
 */
@Component(service = LogCommands.class,
        property = {
                CommandProcessor.COMMAND_SCOPE + "=log",
                CommandProcessor.COMMAND_FUNCTION + "=tail",
                CommandProcessor.COMMAND_FUNCTION + "=get",
                CommandProcessor.COMMAND_FUNCTION + "=set",
        })
public class LogCommands {
    private BundleContext bundleContext;
    @Reference
    private LogReaderService logReaderService;
    @Reference
    private ConfigurationAdmin cm;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        this.bundleContext = null;
    }

    @Descriptor("Set log level for a logger")
    public void set(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: log:set <logger> <level>");
        }

        final String logger = args[0];
        final String level = args[1];

        final Configuration conf = cm.getConfiguration("org.ops4j.pax.logging");
        final Dictionary<String, Object> existingProps = conf.getProperties();
        final Hashtable<String, Object> newProps = new Hashtable<>(8);
        if (existingProps != null) {
            for (final Enumeration<String> keys = existingProps.keys(); keys.hasMoreElements(); ) {
                final String key = keys.nextElement();
                final Object value = existingProps.get(key);
                newProps.put(key, value);
            }
        }

        String loggerKey = null;
        if ("root".equals(logger)) {
            newProps.put("log4j2.rootLogger.level", level);
        } else {
            // Lookup existing logger configuration (if any).
            for (final Map.Entry<String, Object> e : newProps.entrySet()) {
                final String key = e.getKey();
                final Object value = e.getValue();
                if (key.endsWith(".name") && logger.equals(value)) {
                    final int i = key.lastIndexOf(".name");
                    loggerKey = key.substring(0, i);
                    break;
                }
            }
            if (loggerKey == null) {
                loggerKey = "log4j2.logger." + logger.replace(".", "");
            }
            newProps.put(loggerKey + ".name", logger);
            newProps.put(loggerKey + ".level", level);
        }

        conf.update(newProps);
    }

    @Descriptor("Get log level")
    public void get(CommandSession session, String... loggers) throws IOException {
        if (loggers == null || loggers.length == 0) {
            loggers = new String[]{"root"};
        }
        for (final String logger : loggers) {
            final Configuration conf = cm.getConfiguration("org.ops4j.pax.logging");
            final Dictionary<String, Object> props = conf.getProperties();
            String level = null;
            if (props != null) {
                String loggerKey = null;
                for (final Enumeration<String> keys = props.keys(); keys.hasMoreElements(); ) {
                    final String key = keys.nextElement();
                    final Object value = props.get(key);
                    if (key.endsWith(".name") && logger.equals(value)) {
                        final int i = key.lastIndexOf(".name");
                        loggerKey = key.substring(0, i);
                        break;
                    }
                }

                Object possiblyNullLevel = props.get(loggerKey + ".level");
                if (possiblyNullLevel != null) {
                    level = possiblyNullLevel.toString();
                } else {
                    // Get level from root logger.
                    possiblyNullLevel = props.get("log4j2.rootLogger.level");
                    if (possiblyNullLevel != null) {
                        level = possiblyNullLevel.toString();
                    }
                }
            }
            if (level == null) {
                // No logging configuration: just assume we have default level.
                level = "INFO";
            }
            session.getConsole().println(logger + "=" + level);
        }
    }

    @Descriptor("Display last log entries")
    public void tail(CommandSession session, @Parameter(names = {"-e", "--entries"}, absentValue = "50") int max) {
        final List<LogEntry> entries = Collections.list(logReaderService.getLog());
        Collections.sort(entries, LogEntryComparator.INSTANCE);

        final int entriesCount = entries.size();
        final int firstIndex = Math.max(entriesCount - max, 0);
        final DateFormat df = new SimpleDateFormat("HH:mm:ss,SSS");
        final PrintStream out = session.getConsole();
        for (int i = firstIndex; i < entriesCount; ++i) {
            final LogEntry log = entries.get(i);
            final String date = df.format(log.getTime());
            final String level = formatLevel(log.getLevel());
            final String bundle = log.getBundle().getSymbolicName();
            final long bundleId = log.getBundle().getBundleId();
            final String logStr = String.format("%s [%-7s] %40s (%3s) - %s", date, level, bundle, bundleId, log.getMessage());
            out.println(logStr);
        }
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
}
