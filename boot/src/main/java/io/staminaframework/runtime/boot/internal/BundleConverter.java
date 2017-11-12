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

import org.apache.felix.service.command.Converter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * {@link Converter} implementation handling {@link Bundle} instances.
 *
 * @author Stamina Framework developers
 */
@Component(service = Converter.class)
public class BundleConverter implements Converter {
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        this.bundleContext = null;
    }

    @Override
    public Object convert(Class<?> desiredType, Object in) throws Exception {
        if (Bundle.class == desiredType) {
            final String s = in.toString();
            // Try to convert input to bundle identifier (long).
            try {
                final long bundleId = Long.parseLong(s);
                return bundleContext.getBundle(bundleId);
            } catch (NumberFormatException ignore) {
            }

            // Lookup bundle against symbolic name or location.
            for (final Bundle bundle : bundleContext.getBundles()) {
                if (s.equals(bundle.getSymbolicName())) {
                    return bundle;
                }
                if (s.equals(bundle.getLocation())) {
                    return bundle;
                }
            }
        }
        return null;
    }

    @Override
    public CharSequence format(Object target, int level, Converter escape) throws Exception {
        if (target instanceof Bundle && level == Converter.PART) {
            return ((Bundle) target).getSymbolicName();
        }
        if (target instanceof Bundle && level == Converter.INSPECT) {
            final Bundle bundle = (Bundle) target;
            final StringBuilder buf = new StringBuilder(64);
            if (bundle.getHeaders().get(Constants.BUNDLE_NAME) == null) {
                buf.append(bundle.getSymbolicName());
            } else {
                buf.append(bundle.getHeaders().get(Constants.BUNDLE_NAME));
            }
            final Version version = bundle.getVersion();
            if (!Version.emptyVersion.equals(version)) {
                buf.append(" (").append(version).append(")");
            }
            return buf;
        }
        return null;
    }
}
