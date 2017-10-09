/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.it;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * Test helper.
 *
 * @author Stamina.io developers
 */
public final class TestHelper {
    private TestHelper() {
    }

    public static Option[] options(Option... options) {
        return CoreOptions.options(
                mavenBundle("org.apache.felix", "org.apache.felix.log", "1.0.1"),
                mavenBundle("org.everit.osgi", "org.everit.osgi.loglistener.slf4j", "1.0.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.12"),
                mavenBundle("org.eclipse.equinox", "org.eclipse.equinox.region").versionAsInProject(),
                composite(options),
                junitBundles()
        );
    }

    public static Bundle lookupBundle(BundleContext ctx, String symbolicName) {
        for (final Bundle b : ctx.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Bundle not found: " + symbolicName);
    }

    public static <T> T lookupService(BundleContext ctx, Class<T> serviceClass) {
        T svc = null;
        final ServiceReference<T> ref = ctx.getServiceReference(serviceClass);
        if (ref != null) {
            svc = ctx.getService(ref);
        }
        if (svc == null) {
            throw new IllegalArgumentException("Service not found: " + serviceClass);
        }
        return svc;
    }
}
