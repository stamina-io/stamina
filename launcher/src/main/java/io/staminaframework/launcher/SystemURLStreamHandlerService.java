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

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.Constants;
import org.osgi.service.url.AbstractURLStreamHandlerService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * URL handler for loading bundles through the system repository.
 *
 * @author Stamina Framework developers
 */
class SystemURLStreamHandlerService extends AbstractURLStreamHandlerService {
    private final Map<String, URL> resourcesBySymbolicName = new HashMap<>(16);

    public SystemURLStreamHandlerService(final Path systemRepoDir, final Logger logger) throws IOException {
        final Set<Path> files = Files.walk(systemRepoDir)
                .filter(this::isBundleOrSubsystem)
                .collect(Collectors.toSet());
        for (final Path p : files) {
            final String sn = getSymbolicName(p);
            if (resourcesBySymbolicName.put(sn, p.toUri().toURL()) != null) {
                logger.warn(() -> "Found duplicate resource in system repository: " + p);
            }
        }
    }

    private boolean isBundleOrSubsystem(Path p) {
        final String fileName = p.getFileName().toString().toLowerCase();
        return fileName.endsWith(".jar") || fileName.endsWith(".esa");
    }

    @Override
    public URLConnection openConnection(URL u) throws IOException {
        final String symbolicName = u.getHost();
        final URL resourceLocation = resourcesBySymbolicName.get(symbolicName);
        if (resourceLocation == null) {
            throw new IOException("Resource not found in system repository: " + symbolicName);
        }
        return resourceLocation.openConnection();
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
        final String fileName = f.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".jar")) {
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
        } else if (fileName.endsWith(".esa")) {
            try (final ZipFile zip = new ZipFile(f.toFile())) {
                final ZipEntry e = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
                if (e != null) {
                    final Manifest man = new Manifest(zip.getInputStream(e));
                    final String ssn = man.getMainAttributes().getValue("Subsystem-SymbolicName");
                    if (ssn != null) {
                        sn = Parser.parseHeader(ssn)[0].getName();
                    }
                }
            } catch (IOException ignore) {
            }
        }
        return sn;
    }
}
