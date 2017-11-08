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
import org.osgi.framework.Version;
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
    private final Map<String, Resource> resourcesBySymbolicName = new HashMap<>(16);

    public SystemURLStreamHandlerService(final Path systemRepoDir, final Logger logger) throws IOException {
        final Set<Path> files = Files.walk(systemRepoDir)
                .filter(this::isBundleOrSubsystem)
                .collect(Collectors.toSet());
        for (final Path p : files) {
            Resource rsc = toResource(p);
            final Resource old = resourcesBySymbolicName.get(rsc.symbolicName);
            if (old != null) {
                // We only keep the highest version of a given system resource.
                rsc = rsc.version.compareTo(old.version) == 1 ? rsc : old;
            }
            resourcesBySymbolicName.put(rsc.symbolicName, rsc);
        }
    }

    private boolean isBundleOrSubsystem(Path p) {
        final String fileName = p.getFileName().toString();
        return fileName.endsWith(".jar") || fileName.endsWith(".esa");
    }

    @Override
    public URLConnection openConnection(URL u) throws IOException {
        final String symbolicName = u.getHost();
        final Resource rsc = resourcesBySymbolicName.get(symbolicName);
        if (rsc == null) {
            throw new IOException("Resource not found in system repository: " + symbolicName);
        }
        return rsc.url.openConnection();
    }

    private Resource toResource(Path f) throws MalformedURLException {
        String sn = null;
        Version v = null;
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
                    final String rawVersion = man.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
                    if (rawVersion == null) {
                        v = Version.emptyVersion;
                    } else {
                        v = Version.parseVersion(rawVersion);
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
                    final String rawVersion = man.getMainAttributes().getValue("Subsystem-Version");
                    if (rawVersion == null) {
                        v = Version.emptyVersion;
                    } else {
                        v = Version.parseVersion(rawVersion);
                    }
                }
            } catch (IOException ignore) {
            }
        }
        return new Resource(sn, f.toUri().toURL(), v);
    }

    private static class Resource {
        public final Version version;
        public final String symbolicName;
        public final URL url;

        public Resource(final String symbolicName, final URL url, final Version version) {
            this.symbolicName = symbolicName;
            this.url = url;
            this.version = version;
        }
    }
}
