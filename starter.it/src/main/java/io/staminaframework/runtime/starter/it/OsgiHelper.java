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

package io.staminaframework.runtime.starter.it;

import org.osgi.framework.*;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * OSGi utilities.
 *
 * @author Stamina Framework developers
 */
public final class OsgiHelper {
    private OsgiHelper() {
    }

    /**
     * Lookup a bundle.
     *
     * @param ctx          bundle context injected by Pax-Exam
     * @param symbolicName bundle to lookup
     * @return a bundle instance
     */
    public static Bundle lookupBundle(BundleContext ctx, String symbolicName) {
        for (final Bundle b : ctx.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Bundle not found: " + symbolicName);
    }

    /**
     * Lookup a service in the registry.
     *
     * @param ctx          bundle context injected by Pax-Exam
     * @param serviceClass service to lookup
     * @param <T>          service type
     * @return a service instance
     */
    public static <T> T lookupService(BundleContext ctx, Class<T> serviceClass) {
        T svc = null;
        final ServiceReference<T> ref = ctx.getServiceReference(serviceClass);
        if (ref != null) {
            svc = ctx.getService(ref);
        }
        if (svc == null) {
            throw new IllegalArgumentException("Service not found: " + serviceClass.getName());
        }
        return svc;
    }

    /**
     * Lookup a service in the registry.
     *
     * @param ctx          bundle context injected by Pax-Exam
     * @param serviceClass service to lookup
     * @param filter       service filter, may be <code>null</code>
     * @param timeout      time in milliseconds to wait for the service
     * @param <T>          service type
     * @return a service instance
     */
    public static <T> T lookupService(BundleContext ctx, Class<T> serviceClass, String filter, long timeout) throws InterruptedException {
        final String fullFilter;
        final String objFilter = "(" + Constants.OBJECTCLASS + "=" + serviceClass.getName() + ")";
        if (filter == null) {
            fullFilter = objFilter;
        } else {
            fullFilter = "(&" + filter + objFilter + ")";
        }

        final Filter osgiFilter;
        try {
            osgiFilter = ctx.createFilter(fullFilter);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid OSGi service filter: " + fullFilter, e);
        }
        final ServiceTracker<T, T> tracker = new ServiceTracker<>(ctx, osgiFilter, null);
        tracker.open();
        final T svc = tracker.waitForService(timeout);
        if (svc == null) {
            throw new IllegalArgumentException("Service not found: " + serviceClass.getName());
        }
        return svc;
    }

    /**
     * Lookup an active subsystem.
     *
     * @param ctx          bundle context injected by Pax-Exam
     * @param symbolicName subsystem symbolic name to lookup
     * @param timeout      time in milliseconds to wait for the subsystem to become <code>ACTIVE</code>
     * @return a subsystem instance
     */
    public static Subsystem lookupSubsystem(BundleContext ctx, String symbolicName, long timeout) throws InterruptedException {
        final String filter = "(&(" + SubsystemConstants.SUBSYSTEM_SYMBOLICNAME_PROPERTY + "=" + symbolicName + ")("
                + SubsystemConstants.SUBSYSTEM_STATE_PROPERTY + "=" + Subsystem.State.ACTIVE + "))";
        try {
            return lookupService(ctx, Subsystem.class, filter, timeout);
        } catch (Exception e) {
            throw new IllegalArgumentException("Subsystem not found: " + symbolicName, e);
        }
    }

    /**
     * Create a subsystem file of type feature.
     *
     * @param f            file where to create the subsystem
     * @param symbolicName subsystem symbolic name
     * @param contentUrls  subsystem content URLs
     * @throws IOException if subsystem creation failed
     */
    public static void createSubsystemFeature(File f, String symbolicName, String... contentUrls) throws IOException {
        final Manifest man = new Manifest();
        man.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1");
        man.getMainAttributes().putValue(SubsystemConstants.SUBSYSTEM_MANIFESTVERSION, "1");
        man.getMainAttributes().putValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, symbolicName);
        man.getMainAttributes().putValue(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);

        try (final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
            ZipEntry e = new ZipEntry("OSGI-INF/SUBSYSTEM.MF");
            out.putNextEntry(e);
            man.write(out);
            out.closeEntry();

            int i = 0;
            final byte[] buf = new byte[1024];
            for (final String contentUrl : contentUrls) {
                e = new ZipEntry("bundle" + i + ".jar");
                out.putNextEntry(e);
                try (final InputStream in = new URL(contentUrl).openStream()) {
                    for (int bytesRead; (bytesRead = in.read(buf)) != -1; ) {
                        out.write(buf, 0, bytesRead);
                    }
                }
                out.closeEntry();
            }
        }
    }

    /**
     * Install a subsystem.
     *
     * @param ctx bundle context injected by Pax-Exam
     * @param url subsystem URL to install
     */
    public void installSubsystem(BundleContext ctx, String url) throws InterruptedException {
        final Subsystem root = lookupSubsystem(ctx, SubsystemConstants.ROOT_SUBSYSTEM_SYMBOLICNAME, 1000);
        root.install(url);
    }
}
