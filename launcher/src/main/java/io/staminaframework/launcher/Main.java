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

package io.staminaframework.launcher;

import io.staminaframework.launcher.internal.ConsoleLogger;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Application entry point.
 *
 * @author Stamina Framework developers
 */
public class Main {
    private static Framework fmk;

    public static void main(String[] args) throws Exception {
        final String homePath = System.getProperty("stamina.home", System.getProperty("user.dir"));
        final File homeDir = new File(homePath).getCanonicalFile();
        if (!homeDir.exists() || !homeDir.isDirectory()) {
            System.err.println("Invalid home directory: please set System property stamina.home");
            System.exit(1);
        }
        // Make sure home dir stored in System properties is a canonical path.
        System.setProperty("stamina.home", homeDir.getPath());

        // Parse command-line arguments.
        final Map<String, String> fmkArgs = new HashMap<>(4);
        String cmd = null;
        final List<String> cmdArgs = new ArrayList<>(4);
        for (final String arg : args) {
            if (cmd != null) {
                cmdArgs.add(arg);
            } else if (arg.startsWith("--")) {
                final int i = arg.indexOf('=');
                if (i != -1 && i != arg.length() - 1 && arg.length() > 2) {
                    final String key = arg.substring(2, i);
                    final String value = arg.substring(i + 1);
                    fmkArgs.put(key, value);
                }
            } else if (cmd == null) {
                cmd = arg;
            }
        }

        final String confPath = System.getProperty("stamina.conf", new File(homeDir, "etc").getPath());
        final File confDir = new File(confPath);
        final Map<String, String> fmkConf =
                ConfigurationUtils.loadConfiguration(new File(confDir, "framework.properties"), null);
        fmkConf.putAll(fmkArgs);

        final Logger logger = setupLogging(fmkConf, cmd != null);
        logger.info(() -> "Stamina " + Version.VERSION + " build " + Version.BUILD);
        logger.debug(() -> "Home directory: " + homeDir);
        logger.debug(() -> "Framework configuration: " + sortMap(fmkConf));

        final File systemFile = new File(confDir, "system.properties");
        final Map<String, String> systemProps = ConfigurationUtils.loadConfiguration(systemFile, null);
        if (!systemProps.isEmpty()) {
            logger.debug(() -> "Setting System properties: " + sortMap(systemProps));
            systemProps.forEach((k, v) -> {
                System.setProperty(k, v);
            });
        }

        // Copy some important key entries to System properties.
        fmkConf.keySet().stream().filter(k -> k.startsWith("stamina.")).forEach(k -> {
            System.setProperty(k, fmkConf.get(k));
        });

        // Clean-up data directory if needed.
        final File dataDir = new File(fmkConf.get("stamina.data"));
        if (dataDir.exists() && "true".equals(fmkConf.get("stamina.data.clean"))) {
            logger.info(() -> "Cleaning data directory: " + dataDir);
            deleteDir(dataDir);
        }

        // Make sure temp dir exists.
        final File javaTmpDir = new File(System.getProperty("java.io.tmpdir"));
        if (!javaTmpDir.exists()) {
            javaTmpDir.mkdirs();
        }

        // Felix logger is disabled by default.
        if (!fmkConf.containsKey(FelixConstants.LOG_LEVEL_PROP)) {
            fmkConf.put(FelixConstants.LOG_LEVEL_PROP, "0");
        }

        logger.debug(() -> "Selecting OSGi framework");
        try {
            fmk = selectFramework(fmkConf);
        } catch (IOException e) {
            logger.fatal(() -> "Failed to locate OSGi framework", e);
        }
        assert fmk != null;
        logger.debug(() -> "OSGi framework found: " + fmk.getClass().getName());

        final FrameworkStartLevel fsl = fmk.adapt(FrameworkStartLevel.class);
        final FrameworkListener fmkListener = (event) -> {
            switch (event.getType()) {
                case FrameworkEvent.ERROR:
                    logger.fatal(() -> "Fatal error", event.getThrowable());
                    System.exit(1);
                    break;
                case FrameworkEvent.INFO:
                    logger.info(() -> event.toString());
                    break;
                case FrameworkEvent.WARNING:
                    logger.warn(() -> event.toString());
                    break;
                case FrameworkEvent.PACKAGES_REFRESHED:
                    logger.debug(() -> "Packages refresh completed");
                    break;
                case FrameworkEvent.STARTED:
                    logger.debug(() -> "OSGi framework started");
                    break;
                case FrameworkEvent.STARTLEVEL_CHANGED:
                    logger.debug(() -> "Framework start level changed: "
                            + fsl.getStartLevel());
                    break;
                case FrameworkEvent.STOPPED:
                case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
                    logger.debug(() -> "OSGi framework stopped");
                    break;
                case FrameworkEvent.WAIT_TIMEDOUT:
                    logger.debug(() -> "OSGi wait timeout");
                    break;
            }
        };

        logger.info(() -> "Initializing OSGi framework");
        try {
            fmk.init(fmkListener);
        } catch (BundleException e) {
            logger.fatal(() -> "Failed to initialize OSGi framework", e);
        }
        fmk.getBundleContext().addFrameworkListener(fmkListener);

        // Register custom URL stream handler in order to load bundles from the system repository.
        final BundleContext sysCtx = fmk.getBundleContext();
        final Dictionary<String, Object> systemUrlProps = new Hashtable<>();
        systemUrlProps.put(URLConstants.URL_HANDLER_PROTOCOL, "system");
        systemUrlProps.put(URLConstants.URL_CONTENT_MIMETYPE, "application/java-archive");
        sysCtx.registerService(URLStreamHandlerService.class, new SystemURLStreamHandlerService(new File(homeDir, "sys")), systemUrlProps);

        logger.debug(() -> "Loading bundle start levels");
        final File initFile = new File(confDir, "init.properties");
        final Map<String, String> initProps = ConfigurationUtils.loadConfiguration(initFile, fmk.getBundleContext());
        final Map<Integer, SortedSet<String>> initStartLevels = new HashMap<>(10);
        initProps.forEach((k, v) -> {
            int level = 1;
            try {
                level = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                logger.fatal(() -> "Invalid start level: " + v, e);
            }
            SortedSet<String> bundles = initStartLevels.get(level);
            if (bundles == null) {
                bundles = new TreeSet<>();
                initStartLevels.put(level, bundles);
            }
            bundles.add(k);
        });
        logger.debug(() -> "Bundle start levels: " + initStartLevels);

        logger.debug(() -> "Provisioning OSGi platform");
        final Map<String, Bundle> installedBundles = new HashMap<>(32);
        for (final Bundle b : sysCtx.getBundles()) {
            installedBundles.put(b.getSymbolicName(), b);
        }

        initStartLevels.entrySet().forEach(e -> {
            final Integer startLevel = e.getKey();
            final SortedSet<String> bsns = e.getValue();
            bsns.forEach(bsn -> {
                Bundle b = installedBundles.get(bsn);
                if (b == null) {
                    try {
                        logger.info(() -> "Installing system bundle: " + bsn);
                        b = sysCtx.installBundle("system://" + bsn);
                        b.adapt(BundleStartLevel.class).setStartLevel(startLevel);
                        b.start();
                    } catch (BundleException ex) {
                        logger.fatal(() -> "Failed to install system bundle: " + bsn, ex);
                    }
                }
            });
        });

        // If a command is set, write it with its arguments to a file,
        // which will be read by bundle boot.helper to publish a CommandLine service.
        boolean bootBundleFound = false;
        for (final Bundle b : sysCtx.getBundles()) {
            if ("io.staminaframework.boot".equals(b.getSymbolicName())) {
                bootBundleFound = true;
                final File cmdFile = b.getDataFile("cmd.dat");
                cmdFile.getParentFile().mkdirs();
                if (cmd != null) {
                    try (final DataOutputStream out = new DataOutputStream(new FileOutputStream(cmdFile))) {
                        out.writeUTF(cmd);
                        out.writeInt(cmdArgs.size());
                        for (String cmdArg : cmdArgs) {
                            out.writeUTF(cmdArg);
                        }
                    } catch (IOException e) {
                        logger.fatal(() -> "Failed to write command-line data", e);
                    }
                } else {
                    cmdFile.delete();
                }
                break;
            }
        }
        if (!bootBundleFound) {
            logger.fatal(() -> "Missing system bundle: io.staminaframework.boot", null);
        }

        Runtime.getRuntime().addShutdownHook(new Thread("Stamina Shutdown Hook") {
            @Override
            public void run() {
                try {
                    fmk.stop();
                    fmk.waitForStop(10000);
                } catch (Exception e) {
                    logger.fatal(() -> "Failed to properly stop OSGi framework", e);
                }
            }
        });

        logger.info(() -> "Starting OSGi framework");
        try {
            fmk.start();
        } catch (BundleException e) {
            logger.fatal(() -> "Failed to start OSGi framework", e);
        }

        // OSGi framework is now ready.
        int exitCode = 0;
        try {
            final FrameworkEvent evt = fmk.waitForStop(0);
            if (evt.getType() == FrameworkEvent.STOPPED_UPDATE
                    || evt.getType() == FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED) {
                // Framework wants to be restarted: use a special return code.
                exitCode = 100;
            }
        } catch (InterruptedException e) {
            logger.info(() -> "Stopping OSGi framework");
        }

        System.exit(exitCode);
    }

    private static SortedMap<String, String> sortMap(Map<String, String> map) {
        return new TreeMap<>(map);
    }

    private static Framework selectFramework(Map<String, String> fmkConf) throws IOException {
        final ServiceLoader<FrameworkFactory> fmkLoader = ServiceLoader.load(FrameworkFactory.class);
        final Iterator<FrameworkFactory> i = fmkLoader.iterator();
        if (!i.hasNext()) {
            throw new RuntimeException("No OSGi framework found");
        }

        final FrameworkFactory fact = i.next();
        return fact.newFramework(fmkConf);
    }

    private static Logger setupLogging(Map<String, String> fmkConf, boolean commandWasSet) {
        final ServiceLoader<Logger> loggerLoader = ServiceLoader.load(Logger.class);
        final Iterator<Logger> i = loggerLoader.iterator();
        if (!i.hasNext()) {
            final String logLevelStr = fmkConf.get("stamina.log.level");
            int logLevel = ConsoleLogger.INFO_LEVEL;
            if (commandWasSet) {
                // Console output is handled by the command being run:
                // no log outputs unless FATAL level is used.
                logLevel = ConsoleLogger.FATAL_LEVEL;
            } else {
                if (logLevelStr != null) {
                    try {
                        logLevel = Integer.parseInt(logLevelStr);
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
            return new ConsoleLogger(logLevel);
        }
        return i.next();
    }

    private static void deleteDir(File dir) throws IOException {
        Path rootPath = dir.toPath();
        Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
