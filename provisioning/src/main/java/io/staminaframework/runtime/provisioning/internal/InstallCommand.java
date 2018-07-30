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

package io.staminaframework.runtime.provisioning.internal;

import io.staminaframework.runtime.command.Command;
import io.staminaframework.runtime.command.CommandConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * This command is responsible for copying bundles / subsystems to the "addons"
 * directory.
 *
 * @author Stamina Framework developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND + "=provision:install")
public class InstallCommand implements Command {
    @Reference(target = "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=0)")
    private Subsystem root;
    private BundleContext bundleContext;

    @Activate
    void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    void deactivate() {
        this.bundleContext = null;
    }

    @Override
    public void help(PrintStream out) {
        out.println("Install artifacts (such as bundle / subsystem) to the 'addons' directory.");
        out.println("Provision files contains artifact URLs to install, one by line.");
        out.println("All platform supported URL protocols can be used, such as:");
        out.println("  - addon:io.staminaframework.addons.shell");
        out.println("  - mvn:groupId/artifactId/version/type");
        out.println("  - http://repo.site/artifact.esa");
        out.println("  - file://extensions/bundle.jar");
        out.println("Following artifact types are supported:");
        out.println("  - Bundle (*.jar)");
        out.println("  - Subsystem/addon (*.esa)");
        out.println("  - Configuration (*.cfg)");
        out.println("Lines starting with '#' are skipped.");
        out.println("Use flag '--force' to force artifact install.");
        out.println("Use flag '--start' to keep platform running when provisioning is done.");
        out.println("Provision file arguments may refer to a downloadable resource.");
        out.println("Usage: provision:install [--force] [--start] <provision files>");
    }

    @Override
    public boolean execute(Context context) throws Exception {
        if (context.arguments().length == 0) {
            help(context.out());
            return true;
        }

        final String dataProp = System.getProperty("stamina.data");
        if (dataProp == null) {
            throw new RuntimeException("Missing system property: stamina.data");
        }
        final Path dataDir = FileSystems.getDefault().getPath(dataProp);
        if (!Files.exists(dataDir) || !Files.isDirectory(dataDir)) {
            throw new IOException("Missing data directory: " + dataDir);
        }

        final String confProp = System.getProperty("stamina.conf");
        if (confProp == null) {
            throw new RuntimeException("Missing system property: stamina.conf");
        }
        final Path confDir = FileSystems.getDefault().getPath(confProp);
        if (!Files.exists(confDir) || !Files.isDirectory(confDir)) {
            throw new IOException("Missing configuration directory: " + confDir);
        }

        final Path provisionDir = dataDir.resolve("provision");
        Files.createDirectories(provisionDir);

        log(context.out(), "Starting platform provisioning");

        // Parse command flags.
        boolean forceInstall = false;
        boolean start = false;
        for (final String arg : context.arguments()) {
            if ("--force".equals(arg)) {
                forceInstall = true;
            }
            if ("--start".equals(arg)) {
                start = true;
            }
        }

        final String httpUserAgent = "StaminaFramework/"
                + bundleContext.getBundle().getVersion().toString();

        for (final String arg : context.arguments()) {
            if (arg.startsWith("--")) {
                continue;
            }
            Path provisionFile = null;
            try {
                provisionFile = FileSystems.getDefault().getPath(arg);
                if (!Files.exists(provisionFile)) {
                    throw new IllegalArgumentException("Provision file does not exist: " + provisionFile);
                }
            } catch (InvalidPathException ignore) {
            }
            if (provisionFile == null) {
                log(context.out(), "Downloading provision file: " + arg);
                final URL provisionUrl = new URL(arg);
                provisionFile = Files.createTempFile("stamina-provision-", ".spf");
                provisionFile.toFile().deleteOnExit();

                final URLConnection conn = provisionUrl.openConnection();
                conn.setRequestProperty("User-Agent", httpUserAgent);
                try (final InputStream in = conn.getInputStream()) {
                    Files.copy(in, provisionFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            log(context.out(),
                    "Reading provision file: " + arg);

            final List<String> provisionLines = Files.readAllLines(provisionFile);
            for (final String provisionLine : provisionLines) {
                final String urlSpec = provisionLine.trim();
                if (urlSpec.startsWith("#") || urlSpec.length() == 0) {
                    // Skip comments & empty lines.
                    continue;
                }

                final URL artifactUrl = new URL(urlSpec);
                final String targetFileName = toLocalPath(artifactUrl.toExternalForm());
                final Path target;
                if (targetFileName.endsWith(".cfg")) {
                    target = confDir.resolve(targetFileName);
                } else {
                    target = provisionDir.resolve(targetFileName);
                }
                if (Files.exists(target) && !forceInstall) {
                    log(context.out(),
                            "Artifact already exists: " + artifactUrl);
                    continue;
                }

                final Path tmp = Files.createTempFile("stamina-install-", ".tmp");
                tmp.toFile().deleteOnExit();
                log(context.out(),
                        "Downloading artifact: " + artifactUrl);
                try (final InputStream in = new BufferedInputStream(artifactUrl.openStream(), 4096)) {
                    // Download remote artifact and copy it to a temporary file.
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }

                // Move the fully downloaded file to the provision directory.
                log(context.out(),
                        "Installing artifact: " + artifactUrl);
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);

                // Install resource.
                if (targetFileName.endsWith(".jar")) {
                    try (final InputStream in = Files.newInputStream(target)) {
                        final Bundle bundle = bundleContext.installBundle(artifactUrl.toExternalForm(), in);
                        bundle.start();
                    }
                } else if (targetFileName.endsWith(".esa")) {
                    try (final InputStream in = Files.newInputStream(target)) {
                        final Subsystem subsystem = root.install(artifactUrl.toExternalForm(), in);
                        subsystem.start();
                    }
                }
            }
        }

        log(context.out(), "Platform provisioning done");

        return start;
    }

    private String toLocalPath(String url) throws IOException {
        // Do some clever work to transform an URL to a file name...
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte[] hash = md5.digest(url.getBytes("UTF-8"));
            final StringBuilder hex = new StringBuilder(32);
            for (int i = 0; i < hash.length; i++) {
                if ((0xff & hash[i]) < 0x10) {
                    hex.append("0"
                            + Integer.toHexString((0xFF & hash[i])));
                } else {
                    hex.append(Integer.toHexString(0xFF & hash[i]));
                }
            }

            // ArtifactInstaller from FileInstall requires a valid extension:
            // we do our best to guess file type.
            final String ext;
            if (url.startsWith("addon:") || url.contains("/esa") || url.toLowerCase().endsWith(".esa")) {
                ext = ".esa";
            } else if (url.contains("/cfg") || url.toLowerCase().endsWith(".cfg")) {
                ext = ".cfg";
            } else {
                ext = ".jar";
            }

            return hex.toString() + ext;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Cannot convert URL to local file", e);
        }
    }

    private void log(PrintStream out, String message) {
        out.print("[INFO ] ");
        out.println(message);
    }
}
