/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.boot.helper.internal;

import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This component is a fix for Eclipse Region: the digraph is not written
 * when the bundle stops, causing an error on restart.
 *
 * @author Stamina developers
 */
@Component
public class RegionDigraphWriter {
    private static final String DIGRAPH_FILE = "digraph";

    @Reference
    private LogService logService;
    @Reference
    private RegionDigraph regionDigraph;

    @Activate
    public void activate(BundleContext bundleContext) {
        logService.log(LogService.LOG_DEBUG, "Region digraph writer is enabled");

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
                    logService.log(LogService.LOG_ERROR, "Failed to write region digraph", e);
                }
                break;
            }
        }
    }

    private void saveDigraph(Bundle regionBundle) throws IOException {
        logService.log(LogService.LOG_DEBUG, "Saving region digraph to disk");

        final File digraphFile = regionBundle.getBundleContext().getDataFile(DIGRAPH_FILE);
        try (final OutputStream out = new FileOutputStream(digraphFile)) {
            regionDigraph.getRegionDigraphPersistence().save(regionDigraph, out);
        }
    }
}
