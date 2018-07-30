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

package io.staminaframework.runtime.boot.internal;

import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This component is a fix for Eclipse Region: the digraph is not written
 * when the bundle stops, causing an error on restart.
 *
 * @author Stamina Framework developers
 */
@Component
public class RegionDigraphWriter {
    private static final String DIGRAPH_FILE = "digraph";

    @Reference
    private LoggerFactory loggerFactory;
    private Logger logger;
    @Reference
    private RegionDigraph regionDigraph;

    @Activate
    public void activate(BundleContext bundleContext) {
        logger = loggerFactory.getLogger(getClass());
        logger.debug("Region digraph writer is enabled");

        // Write to disk when the region digraph is first published
        // in service registry.
        handleRegionDigraph(bundleContext);
    }

    @Deactivate
    public void deactivate(BundleContext bundleContext) {
        // We also write to disk when the region digraph
        // is about to be unpublished.
        handleRegionDigraph(bundleContext);
    }

    private void handleRegionDigraph(BundleContext bundleContext) {
        for (final Bundle b : bundleContext.getBundles()) {
            if ("org.eclipse.equinox.region".equals(b.getSymbolicName())) {
                try {
                    saveDigraph(b);
                } catch (IOException e) {
                    logger.error("Failed to write region digraph", e);
                }
                break;
            }
        }
    }

    private void saveDigraph(Bundle regionBundle) throws IOException {
        logger.debug("Saving region digraph to disk");

        final File digraphFile = regionBundle.getBundleContext().getDataFile(DIGRAPH_FILE);
        try (final OutputStream out = new FileOutputStream(digraphFile)) {
            regionDigraph.getRegionDigraphPersistence().save(regionDigraph, out);
        }
    }
}
