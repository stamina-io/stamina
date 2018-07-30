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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.staminaframework.runtime.log.bridge.OsgiAppender;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * Log related commands.
 *
 * @author Stamina Framework developers
 */
class LogCommands {
    private static final Map<String, LogLevel> LOG_LEVELS = new HashMap<>(8);

    static {
        LOG_LEVELS.put("audit", LogLevel.AUDIT);
        LOG_LEVELS.put("debug", LogLevel.DEBUG);
        LOG_LEVELS.put("error", LogLevel.ERROR);
        LOG_LEVELS.put("info", LogLevel.INFO);
        LOG_LEVELS.put("warn", LogLevel.WARN);
        LOG_LEVELS.put("trace", LogLevel.TRACE);
    }

    private final BundleContext bundleContext;

    public LogCommands(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static LogLevel toLogLevel(String s) {
        final LogLevel level;
        if (s == null) {
            level = null;
        } else {
            level = LOG_LEVELS.get(s.toLowerCase());
        }
        if (level == null) {
            throw new IllegalArgumentException("Invalid log level: " + s);
        }
        return level;
    }

    private <S, R> R useServiceIfAvailable(Class<S> serviceClass, Function<S, R> op) {
        final ServiceReference<S> ref = bundleContext.getServiceReference(serviceClass);
        if (ref != null) {
            try {
                final S svc = bundleContext.getService(ref);
                return op.apply(svc);
            } finally {
                bundleContext.ungetService(ref);
            }
        }
        return null;
    }

    @Descriptor("Set log level for a logger")
    public void set(@Descriptor("logger name") String logger,
                    @Descriptor("logger level") String level) {
        final LogLevel logLevel = toLogLevel(level);
        useServiceIfAvailable(LoggerAdmin.class, svc -> {
            final LoggerContext ctx = svc.getLoggerContext(null);
            final Map<String, LogLevel> levels = ctx.getLogLevels();
            levels.put(logger, logLevel);
            ctx.setLogLevels(levels);
            return true;
        });
    }

    @Descriptor("Get log level")
    public void get(CommandSession session,
                    @Descriptor("logger names") String... loggers) {
        useServiceIfAvailable(LoggerAdmin.class, svc -> {
            final LoggerContext ctx = svc.getLoggerContext(null);
            final LogLevel rootLevel = ctx.getEffectiveLogLevel(Logger.ROOT_LOGGER_NAME);
            if (rootLevel != null) {
                session.getConsole().println("Default log level is " + rootLevel);
            }

            final Map<String, LogLevel> logLevels = new HashMap<>(4);
            if (loggers != null && loggers.length != 0) {
                for (final String logger : loggers) {
                    final LogLevel level = ctx.getEffectiveLogLevel(logger);
                    if (level != null) {
                        logLevels.put(logger, level);
                    }
                }
            } else {
                logLevels.putAll(ctx.getLogLevels());
            }

            logLevels.keySet().stream().sorted().forEach(logger -> {
                session.getConsole().println(logger + "=" + logLevels.get(logger));
            });
            return true;
        });
    }

    @Descriptor("Display last log entries")
    public void tail(CommandSession session) {
        final ch.qos.logback.classic.LoggerContext loggerContext =
                (ch.qos.logback.classic.LoggerContext) LoggerFactory.getILoggerFactory();
        for (final Logger logger : loggerContext.getLoggerList()) {
            for (final Iterator<Appender<ILoggingEvent>> i = logger.iteratorForAppenders(); i.hasNext(); ) {
                final Appender<ILoggingEvent> appender = i.next();
                if (appender instanceof OsgiAppender) {
                    final OsgiAppender<?> osgiAppender = (OsgiAppender) appender;
                    for (final String entry : osgiAppender.getLogEntries()) {
                        session.getConsole().println(entry);
                    }
                    return;
                }
            }
        }
    }
}
