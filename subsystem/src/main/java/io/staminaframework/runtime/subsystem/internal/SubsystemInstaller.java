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

package io.staminaframework.runtime.subsystem.internal;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * OSGi Subsystem installer.
 *
 * @author Stamina Framework developers
 */
@Component(service = ArtifactInstaller.class, immediate = true)
public class SubsystemInstaller implements ArtifactInstaller {
    @Reference
    private LoggerFactory loggerFactory;
    private Logger logger;
    @Reference(target = "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=0)")
    private Subsystem root;
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) throws Exception {
        logger = loggerFactory.getLogger(getClass());
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        bundleContext = null;
    }

    @Override
    public void install(File artifact) throws Exception {
        final Manifest man = getManifest(artifact);
        final String sid = getSubsystemId(man);

        final String spath = artifact.getCanonicalFile().toURI().toURL().toExternalForm();
        final boolean alreadyInstalled = lookupSubsystem(root, spath) != null;
        if (alreadyInstalled) {
            logger.debug("Subsystem {} is already installed", sid);
            return;
        }

        logger.info("Installing subsystem: {}", sid);
        final Subsystem sub = root.install(spath);
        logger.info("Starting subsystem: {}", sid);
        sub.start();
    }

    private Subsystem lookupSubsystem(Subsystem parent, String url) {
        if (parent.getLocation().equals(url)) {
            return parent;
        }
        for (final Subsystem child : parent.getChildren()) {
            if (lookupSubsystem(child, url) != null) {
                return child;
            }
        }
        return null;
    }

    @Override
    public void update(File artifact) throws Exception {
        final Manifest man = getManifest(artifact);
        final String sid = getSubsystemId(man);
        logger.info("Updating subsystem: {}", sid);
        uninstall(artifact);
        install(artifact);
        logger.info("Subsystem updated: {}", sid);
    }

    @Override
    public void uninstall(File artifact) throws Exception {
        final String spath = artifact.getCanonicalFile().toURI().toURL().toExternalForm();
        // Subsystem is already installed: we need to uninstall it first.
        final Subsystem subsystem = lookupSubsystem(root, spath);
        if (subsystem != null) {
            final String sid = getSubsystemId(subsystem);
            logger.info("Uninstalling subsystem: {}", sid);
            subsystem.uninstall();
            logger.info("Subsystem uninstalled: {}", sid);
        }
    }

    @Override
    public boolean canHandle(File artifact) {
        if (!artifact.getName().toLowerCase().endsWith(".esa")) {
            return false;
        }
        try {
            final Manifest man = getManifest(artifact);
            final String ssn = man.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
            if (ssn == null) {
                throw new IOException("Missing symbolic name in subsystem manifest");
            }
        } catch (IOException e) {
            logger.warn("Failed to open file as a subsystem: {}", artifact, e);
            return false;
        }
        return true;
    }

    private Manifest getManifest(File artifact) throws IOException {
        final Manifest man = new Manifest();
        try (final ZipFile zip = new ZipFile(artifact)) {
            final ZipEntry manEntry = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
            if (manEntry == null) {
                throw new IOException("Missing subsystem manifest");
            }
            try (final InputStream manIn = zip.getInputStream(manEntry)) {
                man.read(manIn);
            }
            final Attributes atts = man.getMainAttributes();
            final String ssn = atts.getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
            if (ssn == null) {
                throw new IOException("Missing symbolic name in subsystem manifest");
            }
        }
        return man;
    }

    private String getSubsystemId(Manifest man) {
        final String ssn = man.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
        String svn = man.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_VERSION);
        if (svn == null) {
            svn = "0.0.0";
        }
        return ssn + "/" + svn;
    }

    private String getSubsystemId(Subsystem sub) {
        final String ssn = sub.getSymbolicName();
        final String svn = sub.getVersion().toString();
        return ssn + "/" + svn;
    }
}
