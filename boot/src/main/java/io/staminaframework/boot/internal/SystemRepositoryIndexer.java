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

import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.service.subsystem.SubsystemConstants;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This component is responsible for generating an OBR index file,
 * based on system repository content.
 *
 * @author Stamina Framework developers
 */
@Component(immediate = true, configurationPid = "io.staminaframework.boot.obr")
public class SystemRepositoryIndexer {
    @Reference
    private LogService logService;
    private Path indexFile;
    private Path sysRepo;

    /**
     * Component configuration.
     *
     * @author Stamina Framework developers
     */
    public @interface Config {
        /**
         * If <code>true</code>, the repository index is always generated
         * on startup.
         */
        boolean reindex() default false;
    }

    @Activate
    void activate(BundleContext bundleContext, Config config) throws IOException {
        final String sysRepoPath = bundleContext.getProperty("stamina.repo");
        if (sysRepoPath == null) {
            throw new IllegalStateException("Missing framework property: stamina.repo");
        }
        sysRepo = FileSystems.getDefault().getPath(sysRepoPath);

        final String dataPath = bundleContext.getProperty("stamina.data");
        if (dataPath == null) {
            throw new IllegalStateException("Missing framework property: stamina.data");
        }
        final Path dataDir = FileSystems.getDefault().getPath(dataPath);

        indexFile = dataDir.resolve("obr.xml");
        logService.log(LogService.LOG_DEBUG,
                "Using generated repository index: " + indexFile);
        if (config.reindex() && Files.exists(indexFile)) {
            try {
                logService.log(LogService.LOG_DEBUG,
                        "System repository index will be regenerated");
                Files.delete(indexFile);
            } catch (IOException ignore) {
            }
        }

        if (!Files.exists(indexFile) || Files.size(indexFile) == 0) {
            // Lazily index system repository.
            logService.log(LogService.LOG_INFO, "Indexing system repository");
            indexSystemRepository(sysRepo, indexFile);
        }
    }

    /**
     * Index system repository.
     *
     * @param repoDir   repository directory
     * @param indexFile index file
     * @throws IOException if repository indexing failed
     */
    void indexSystemRepository(Path repoDir, Path indexFile) throws IOException {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Failed to compute checksums using SHA-256", e);
        }

        final Set<Resource> resources = new HashSet<>(16);
        final Set<Path> resourceFiles = Files.walk(repoDir)
                .filter(p -> p.toString().endsWith(".esa") || p.toString().endsWith(".jar"))
                .map(p -> repoDir.resolve(p))
                .collect(Collectors.toSet());
        final byte[] buf = new byte[4096];
        for (final Path resourceFile : resourceFiles) {
            final Resource rsc = new Resource();
            try (final InputStream in = Files.newInputStream(resourceFile)) {
                for (int bytesRead; (bytesRead = in.read(buf)) != -1; ) {
                    md.update(buf, 0, bytesRead);
                }
            }
            final byte[] hash = md.digest();
            rsc.checksum = bytesToHex(hash);
            rsc.fileSize = Files.size(resourceFile);

            try (final ZipFile zip = new ZipFile(resourceFile.toFile())) {
                ZipEntry e = zip.getEntry("META-INF/MANIFEST.MF");
                if (e != null) {
                    final Manifest man = new Manifest(zip.getInputStream(e));
                    final String bsn = man.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                    if (bsn != null) {
                        rsc.symbolicName = Parser.parseHeader(bsn)[0].getName();
                        rsc.type = man.getMainAttributes().getValue(Constants.FRAGMENT_HOST) == null
                                ? Resource.Type.BUNDLE : Resource.Type.FRAGMENT_BUNDLE;
                        rsc.version = man.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
                        rsc.url = "system://" + rsc.symbolicName;
                        resources.add(rsc);
                        continue;
                    }
                }
                e = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
                if (e != null) {
                    final Manifest man = new Manifest(zip.getInputStream(e));
                    final String ssn = man.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
                    if (ssn != null) {
                        final String stype = man.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_TYPE);
                        if (SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(stype)) {
                            rsc.symbolicName = Parser.parseHeader(ssn)[0].getName();
                            rsc.type = Resource.Type.FEATURE_SUBSYSTEM;
                            rsc.version = man.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_VERSION);
                            rsc.url = "system://" + rsc.symbolicName;
                            resources.add(rsc);
                            continue;
                        }
                    }
                }
            }
        }

        final XMLStreamWriter xml;
        try {
            xml = XMLOutputFactory.newFactory().createXMLStreamWriter(
                    Files.newOutputStream(indexFile), "UTF-8");
        } catch (XMLStreamException e) {
            throw new IOException("Failed to initialize XML writer", e);
        }

        try {
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("repository");
            xml.writeDefaultNamespace("http://www.osgi.org/xmlns/repository/v1.0.0");
            xml.writeAttribute("increment", String.valueOf(System.currentTimeMillis()));
            xml.writeAttribute("name", "Stamina Runtime System Repository");

            for (final Resource rsc : resources) {
                xml.writeStartElement("resource");
                xml.writeStartElement("capability");
                xml.writeAttribute("namespace", "osgi.identity");

                xml.writeEmptyElement("attribute");
                xml.writeAttribute("name", "osgi.identity");
                xml.writeAttribute("value", rsc.symbolicName);

                xml.writeEmptyElement("attribute");
                xml.writeAttribute("name", "type");
                xml.writeAttribute("value", rsc.type.osgiType);

                xml.writeEmptyElement("attribute");
                xml.writeAttribute("name", "version");
                xml.writeAttribute("type", "Version");
                xml.writeAttribute("value", rsc.version);

                xml.writeEndElement();
                xml.writeStartElement("capability");
                xml.writeAttribute("namespace", "osgi.content");

                xml.writeEmptyElement("attribute");
                xml.writeAttribute("name", "osgi.content");
                xml.writeAttribute("value", rsc.checksum);

                xml.writeEmptyElement("attribute");
                xml.writeAttribute("name", "url");
                xml.writeAttribute("value", rsc.url);

                xml.writeEmptyElement("attribute");
                xml.writeAttribute("name", "size");
                xml.writeAttribute("type", "Long");
                xml.writeAttribute("value", String.valueOf(rsc.fileSize));

                xml.writeEndElement();
                xml.writeEndElement();
            }

            xml.writeEndElement();
            xml.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to write repository index file", e);
        } finally {
            try {
                xml.close();
            } catch (XMLStreamException ignore) {
            }
        }
    }

    private static String bytesToHex(byte[] hash) {
        final StringBuffer hexString = new StringBuffer(64);
        for (int i = 0; i < hash.length; i++) {
            final String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    static class Resource {
        String url;
        long fileSize;
        String checksum;
        Resource.Type type;
        String symbolicName;
        String version;

        enum Type {
            BUNDLE("osgi.bundle", "application/vnd.osgi.bundle"),
            FRAGMENT_BUNDLE("osgi.fragment", "application/vnd.osgi.bundle"),
            FEATURE_SUBSYSTEM(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "application/vnd.osgi.subsystem");

            final String osgiType;
            final String mimeType;

            Type(final String osgiType, final String mimeType) {
                this.osgiType = osgiType;
                this.mimeType = mimeType;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Resource resource = (Resource) o;

            return url.equals(resource.url);
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }
    }
}
