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

package io.staminaframework.subsystem.internal;

import org.apache.felix.service.command.Converter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

import java.util.Collection;

/**
 * {@link Converter} implementation handling {@link Subsystem} instances.
 *
 * @author Stamina Framework developers
 */
@Component(service = Converter.class)
public class SubsystemConverter implements Converter {
    private BundleContext bundleContext;
    @Reference(target = "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=0)")
    private Subsystem root;

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
        if (Subsystem.class == desiredType) {
            final String s = in.toString();
            // Try to convert input to subsystem identifier (long).
            try {
                final long sysId = Long.parseLong(s);
                final Collection<ServiceReference<Subsystem>> refs =
                        bundleContext.getServiceReferences(Subsystem.class,
                                "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=" + sysId + ")");
                for (final ServiceReference<Subsystem> ref : refs) {
                    return bundleContext.getService(ref);
                }
            } catch (NumberFormatException ignore) {
            }

            // Lookup subsystem against symbolic name or location.
            final Collection<ServiceReference<Subsystem>> refs =
                    bundleContext.getServiceReferences(Subsystem.class, null);
            for (final ServiceReference<Subsystem> ref : refs) {
                final Subsystem sys = bundleContext.getService(ref);
                if (sys.getSymbolicName().equals(s)) {
                    return sys;
                }
                if (sys.getLocation().equals(s)) {
                    return sys;
                }

                bundleContext.ungetService(ref);
            }
        }
        return null;
    }

    @Override
    public CharSequence format(Object target, int level, Converter escape) throws Exception {
        if (target instanceof Subsystem && level == Converter.PART) {
            return ((Subsystem) target).getSymbolicName();
        }
        if (target instanceof Subsystem && level == Converter.INSPECT) {
            final Subsystem sys = (Subsystem) target;
            final StringBuilder buf = new StringBuilder(64);
            buf.append(
                    sys.getSubsystemHeaders(null).getOrDefault(SubsystemConstants.SUBSYSTEM_NAME, sys.getSymbolicName()));
            final Version version = sys.getVersion();
            if (!Version.emptyVersion.equals(version)) {
                buf.append(" (").append(version).append(")");
            }
            return buf;
        }
        return null;
    }
}
