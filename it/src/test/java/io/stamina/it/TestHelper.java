/*
 * Copyright (c) 2017 Stamina.io developers.
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
