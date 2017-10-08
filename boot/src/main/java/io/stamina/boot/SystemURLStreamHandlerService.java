/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.boot;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.Constants;
import org.osgi.service.url.AbstractURLStreamHandlerService;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * URL handler for loading bundles through the system repository.
 *
 * @author Stamina developers
 */
class SystemURLStreamHandlerService extends AbstractURLStreamHandlerService {
    private final Map<String, URL> bundlesBySymbolicName;

    public SystemURLStreamHandlerService(final File systemRepoDir) throws IOException {
        bundlesBySymbolicName = Files.walk(systemRepoDir.toPath())
                .filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                .collect(Collectors.toMap(this::getSymbolicName, this::toURL));
    }

    @Override
    public URLConnection openConnection(URL u) throws IOException {
        final String symbolicName = u.getHost();
        final URL bundle = bundlesBySymbolicName.get(symbolicName);
        if (bundle == null) {
            throw new IOException("Bundle not found in system repository: " + symbolicName);
        }
        return bundle.openConnection();
    }

    private URL toURL(Path p) {
        try {
            return p.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unexpected error", e);
        }
    }

    private String getSymbolicName(Path f) {
        String sn = null;
        try (final JarFile jar = new JarFile(f.toFile())) {
            final Manifest man = jar.getManifest();
            if (man != null) {
                final String rawSn = man.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                if (rawSn != null) {
                    final Clause[] snClauses = Parser.parseHeader(rawSn);
                    if (snClauses.length != 0) {
                        sn = snClauses[0].getName();
                    }
                }
            }
        } catch (IOException ignore) {
        }
        return sn;
    }
}
