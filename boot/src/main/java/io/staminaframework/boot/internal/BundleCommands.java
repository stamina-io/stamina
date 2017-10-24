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

import io.staminaframework.asciitable.AsciiTable;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.*;

import static java.util.Arrays.asList;

/**
 * Provides bundle related commands.
 *
 * @author Stamina Framework developers
 */
@Component(service = BundleCommands.class,
        property = {
                CommandProcessor.COMMAND_SCOPE + "=bundle",
                CommandProcessor.COMMAND_FUNCTION + "=list",
                CommandProcessor.COMMAND_FUNCTION + "=start",
                CommandProcessor.COMMAND_FUNCTION + "=stop",
                CommandProcessor.COMMAND_FUNCTION + "=install",
                CommandProcessor.COMMAND_FUNCTION + "=uninstall",
                CommandProcessor.COMMAND_FUNCTION + "=update",
                CommandProcessor.COMMAND_FUNCTION + "=headers",
        })
public class BundleCommands {
    private static final Map<Integer, String> BUNDLE_STATES = new HashMap<>(6);

    static {
        BUNDLE_STATES.put(Bundle.ACTIVE, "Active");
        BUNDLE_STATES.put(Bundle.INSTALLED, "Installed");
        BUNDLE_STATES.put(Bundle.RESOLVED, "Resolved");
        BUNDLE_STATES.put(Bundle.STARTING, "Starting");
        BUNDLE_STATES.put(Bundle.STOPPING, "Stopping");
        BUNDLE_STATES.put(Bundle.UNINSTALLED, "Uninstalled");
    }

    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        this.bundleContext = bundleContext;
    }

    @Descriptor("List installed bundles")
    public void list(CommandSession session,
                     @Descriptor("show symbolic name") @Parameter(names = {"-s", "--symbolic-name"}, absentValue = "false", presentValue = "true") boolean showSymbolicName) {
        final Bundle[] bundles = bundleContext.getBundles();
        Arrays.sort(bundles, BundleComparator.INSTANCE);

        final AsciiTable table = AsciiTable.of(asList("ID", "STATE", "LEVEL", "NAME"));
        for (final Bundle bundle : bundles) {
            final List<String> row = new ArrayList<>(4);
            row.add(String.valueOf(bundle.getBundleId()));
            row.add(BUNDLE_STATES.getOrDefault(bundle.getState(), "<unknown>"));

            final BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
            final int startLevel = bsl.getStartLevel();
            row.add(String.valueOf(startLevel));

            String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
            final StringBuilder buf = new StringBuilder(64);
            if (showSymbolicName) {
                buf.append(bundle.getSymbolicName());
            } else {
                if (name == null) {
                    buf.append(bundle.getSymbolicName());
                } else {
                    buf.append(name);
                }
            }

            final Version version = bundle.getVersion();
            if (!Version.emptyVersion.equals(version)) {
                buf.append(" (").append(version).append(")");
            }
            row.add(buf.toString());
            table.add(row);
        }
        table.render(session.getConsole());
    }

    @Descriptor("Start a bundle")
    public void start(@Descriptor("bundle to start") Bundle bundle) throws BundleException {
        bundle.start();
    }

    @Descriptor("Stop a bundle")
    public void stop(@Descriptor("bundle to stop") Bundle bundle) throws BundleException {
        bundle.stop();
    }

    @Descriptor("Update a bundle")
    public void update(@Descriptor("bundle to update") Bundle bundle) throws BundleException {
        bundle.update();
    }

    @Descriptor("Install a bundle")
    public void install(@Descriptor("bundle location") String url) throws BundleException {
        bundleContext.installBundle(url);
    }

    @Descriptor("Uninstall a bundle")
    public void uninstall(@Descriptor("bundle to uninstall") Bundle bundle) throws BundleException {
        bundle.uninstall();
    }

    @Descriptor("Display bundle headers")
    public void headers(CommandSession session,
                        @Descriptor("bundle to inspect") Bundle bundle) {
        final SortedMap<String, String> sortedHeaders = new TreeMap<>();
        final Dictionary<String, String> rawHeaders = bundle.getHeaders();
        for (final Enumeration<String> keys = rawHeaders.keys(); keys.hasMoreElements(); ) {
            final String key = keys.nextElement();
            final String rawHeader = rawHeaders.get(key);
            if (rawHeader.length() != 0) {
                // We use Felix Utils here to "reformat" bundle headers.
                final Clause[] clauses = Parser.parseHeader(rawHeader);
                if (clauses.length != 0) {
                    final String[] clausesStr = new String[clauses.length];
                    for (int i = 0; i < clauses.length; ++i) {
                        clausesStr[i] = clauses[i].toString();
                    }
                    sortedHeaders.put(key, String.join(", ", clausesStr));
                }
            }
        }
        for (final Map.Entry<String, String> e : sortedHeaders.entrySet()) {
            session.getConsole().println(e.getKey() + ": " + e.getValue());
        }
    }

    private static class BundleComparator implements Comparator<Bundle> {
        public static final Comparator<Bundle> INSTANCE = new BundleComparator();

        @Override
        public int compare(Bundle o1, Bundle o2) {
            final long id1 = o1.getBundleId();
            final long id2 = o2.getBundleId();
            if (id1 < id2) {
                return -1;
            }
            if (id1 > id2) {
                return 1;
            }
            return 0;
        }
    }
}
