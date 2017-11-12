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
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogReaderService;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.FileNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Bundle activator.
 *
 * @author Stamina Framework developers
 */
public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        final String confPath = context.getProperty("stamina.conf");
        if (confPath == null) {
            throw new IllegalStateException("Missing framework property: stamina.conf");
        }
        final Path confDir = FileSystems.getDefault().getPath(confPath);
        final Path logFile = confDir.resolve("logback.xml");
        if (!Files.exists(logFile)) {
            throw new FileNotFoundException("Missing logging configuration file: " + logFile);
        }

        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            final JoranConfigurator joranConfigurator = new JoranConfigurator();
            joranConfigurator.setContext(loggerContext);
            loggerContext.reset();
            joranConfigurator.doConfigure(logFile.toFile());
        } catch (JoranException e) {
            throw new RuntimeException("Unable to configure logging system", e);
        }

        final OsgiBridge bridge = new OsgiBridge();
        bridge.setContext(loggerContext);
        bridge.setName("OSGI");
        final Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(bridge);
        bridge.start();

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        context.registerService(LogReaderService.class, bridge, null);

        final Dictionary<String, Object> logCmdProps = new Hashtable<>(2);
        logCmdProps.put("osgi.command.scope", "log");
        logCmdProps.put("osgi.command.function", new String[]{"tail", "get", "set", "clear"});
        context.registerService(LogCommands.class, new LogCommands(bridge), logCmdProps);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        SLF4JBridgeHandler.uninstall();

        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
    }
}
