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

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.io.PrintStream;
import java.util.*;

/**
 * Service related commands.
 *
 * @author Stamina Framework developers
 */
@Component(service = ServiceCommands.class,
        property = {
                CommandProcessor.COMMAND_SCOPE + "=service",
                CommandProcessor.COMMAND_FUNCTION + "=list",
                CommandProcessor.COMMAND_FUNCTION + "=get",
        })
public class ServiceCommands {
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        ;
    }

    @Deactivate
    public void deactivate() {
        this.bundleContext = bundleContext;
    }

    @Descriptor("Display all services properties")
    public void get(CommandSession session) throws InvalidSyntaxException {
        get(session, false, null);
    }

    @Descriptor("Display service properties")
    public void get(CommandSession session,
                    @Descriptor("use argument as a filter") @Parameter(names = {"-f", "--filter"}, absentValue = "false", presentValue = "true") boolean filter,
                    @Descriptor("service target (interface or filter)") String service) throws InvalidSyntaxException {
        final String serviceItf;
        final String serviceFilter;
        if (filter) {
            serviceItf = null;
            serviceFilter = service;
        } else {
            serviceItf = service;
            serviceFilter = null;
        }
        final ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(serviceItf, serviceFilter);
        if (refs == null || refs.length == 0) {
            return;
        }
        Arrays.sort(refs, ServiceReferenceComparator.INSTANCE);

        final PrintStream out = session.getConsole();
        for (int i = 0; i < refs.length; ++i) {
            if (i != 0) {
                out.println("--------------------------------------------------");
            }
            final ServiceReference<?> ref = refs[i];
            final String[] serviceInterfaces = (String[]) ref.getProperty(Constants.OBJECTCLASS);
            out.println(Arrays.toString(serviceInterfaces));

            out.println("Properties:");
            final List<String> propertyKeys = new ArrayList<>(Arrays.asList(ref.getPropertyKeys()));
            Collections.sort(propertyKeys);
            for (final String key : propertyKeys) {
                if (Constants.OBJECTCLASS.equals(key)) {
                    continue;
                }
                final Object value = ref.getProperty(key);
                final String valueStr;
                if (value instanceof int[]) {
                    valueStr = Arrays.toString((int[]) value);
                } else if (value instanceof boolean[]) {
                    valueStr = Arrays.toString((boolean[]) value);
                } else if (value instanceof String[]) {
                    valueStr = Arrays.toString((String[]) value);
                } else if (value instanceof Object[]) {
                    valueStr = Arrays.toString((Object[]) value);
                } else {
                    valueStr = value.toString();
                }
                out.println("  " + key + " = " + valueStr);
            }

            final Bundle bundle = ref.getBundle();
            if (bundle != null) {
                out.println("Provided by:");
                out.println("  " + toString(ref.getBundle()));
            }

            final Bundle[] usingBundles = ref.getUsingBundles();
            if (usingBundles != null && usingBundles.length != 0) {
                out.println("Used by:");
                Arrays.sort(usingBundles, BundleComparator.INSTANCE);
                for (final Bundle b : usingBundles) {
                    out.println("  " + toString(b));
                }
            }
        }
    }

    private static CharSequence toString(Bundle bundle) {
        final StringBuilder buf = new StringBuilder(32);
        final String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
        if (name == null) {
            buf.append(bundle.getSymbolicName());
        } else {
            buf.append(name);
        }
        buf.append(" (").append(bundle.getBundleId()).append(")");
        return buf;
    }

    @Descriptor("Display all services")
    public void list(CommandSession session) throws InvalidSyntaxException {
        list(session, false, null);
    }

    @Descriptor("Display services matching a filter or an interface")
    public void list(CommandSession session,
                     @Descriptor("use argument as a filter") @Parameter(names = {"-f", "--filter"}, absentValue = "false", presentValue = "true") boolean filter,
                     @Descriptor("service target (interface or filter)") String service) throws InvalidSyntaxException {
        final String serviceItf;
        final String serviceFilter;
        if (filter) {
            serviceItf = null;
            serviceFilter = service;
        } else {
            serviceItf = service;
            serviceFilter = null;
        }
        final ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(serviceItf, serviceFilter);
        if (refs == null || refs.length == 0) {
            return;
        }
        Arrays.sort(refs, ServiceReferenceComparator.INSTANCE);

        final SortedMap<String, Integer> serviceInstances = new TreeMap<>();
        for (int i = 0; i < refs.length; ++i) {
            final ServiceReference<?> ref = refs[i];
            final String[] serviceInterfaces = (String[]) ref.getProperty(Constants.OBJECTCLASS);
            for (final String serviceInterface : serviceInterfaces) {
                final int count = serviceInstances.getOrDefault(serviceInterface, 0);
                serviceInstances.put(serviceInterface, count + 1);
            }
        }

        final PrintStream out = session.getConsole();
        for (final Map.Entry<String, Integer> e : serviceInstances.entrySet()) {
            out.println(e.getKey() + " (" + e.getValue() + ")");
        }
    }

    private static class ServiceReferenceComparator implements Comparator<ServiceReference<?>> {
        public static final Comparator<ServiceReference<?>> INSTANCE = new ServiceReferenceComparator();

        @Override
        public int compare(ServiceReference<?> o1, ServiceReference<?> o2) {
            final long id1 = (Long) o1.getProperty(Constants.SERVICE_ID);
            final long id2 = (Long) o2.getProperty(Constants.SERVICE_ID);
            if (id1 < id2) {
                return -1;
            }
            if (id1 > id2) {
                return 1;
            }
            return 0;
        }
    }

    private static class BundleComparator implements Comparator<Bundle> {
        public static final Comparator<Bundle> INSTANCE = new BundleComparator();

        @Override
        public int compare(Bundle o1, Bundle o2) {
            final long b1 = o1.getBundleId();
            final long b2 = o2.getBundleId();
            if (b1 < b2) {
                return -1;
            }
            if (b1 > b2) {
                return 1;
            }
            return 0;
        }
    }
}
