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

/**
 * URL handler for loading bundles through the system repository.
 *
 * @author Stamina Framework developers
 */
class SystemURLStreamHandlerService extends AbstractURLStreamHandlerService {
    private final Map<String, URL> bundlesBySymbolicName = new HashMap<>(16);

    public SystemURLStreamHandlerService(final Path systemRepoDir, final Logger logger) throws IOException {
        final Set<Path> files = Files.walk(systemRepoDir)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jar"))
                .collect(Collectors.toSet());
        for (final Path p : files) {
            final String bsn = getSymbolicName(p);
            if (bundlesBySymbolicName.put(bsn, p.toUri().toURL()) != null) {
                logger.warn(() -> "Found duplicate bundle in system repository: " + p);
            }
        }
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
