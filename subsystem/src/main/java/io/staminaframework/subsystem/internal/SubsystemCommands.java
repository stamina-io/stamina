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

import io.staminaframework.asciitable.AsciiTable;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Provides subsystem related commands.
 *
 * @author Stamina Framework developers
 */
@Component(service = SubsystemCommands.class,
        property = {
                CommandProcessor.COMMAND_SCOPE + "=subsystem",
                CommandProcessor.COMMAND_FUNCTION + "=list",
                CommandProcessor.COMMAND_FUNCTION + "=start",
                CommandProcessor.COMMAND_FUNCTION + "=stop",
                CommandProcessor.COMMAND_FUNCTION + "=install",
                CommandProcessor.COMMAND_FUNCTION + "=uninstall",
                CommandProcessor.COMMAND_FUNCTION + "=headers",
        })
public class SubsystemCommands {
    private static final Map<Subsystem.State, String> SUBSYSTEM_STATES = new HashMap<>(10);

    static {
        SUBSYSTEM_STATES.put(Subsystem.State.ACTIVE, "Active");
        SUBSYSTEM_STATES.put(Subsystem.State.INSTALL_FAILED, "Install failed");
        SUBSYSTEM_STATES.put(Subsystem.State.INSTALLED, "Installed");
        SUBSYSTEM_STATES.put(Subsystem.State.INSTALLING, "Installing");
        SUBSYSTEM_STATES.put(Subsystem.State.RESOLVED, "Resolved");
        SUBSYSTEM_STATES.put(Subsystem.State.RESOLVING, "Resolving");
        SUBSYSTEM_STATES.put(Subsystem.State.STARTING, "Starting");
        SUBSYSTEM_STATES.put(Subsystem.State.STOPPING, "Stopping");
        SUBSYSTEM_STATES.put(Subsystem.State.UNINSTALLED, "Uninstalled");
        SUBSYSTEM_STATES.put(Subsystem.State.UNINSTALLING, "Uninstalling");
    }

    private BundleContext bundleContext;
    @Reference(target = "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=0)")
    private Subsystem root;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        this.bundleContext = bundleContext;
    }

    @Descriptor("List installed subsystems")
    public void list(CommandSession session,
                     @Descriptor("show symbolic name") @Parameter(names = {"-s", "--symbolic-name"}, absentValue = "false", presentValue = "true") boolean showSymbolicName) {
        final AsciiTable table = AsciiTable.of(
                asList("ID", "PARENTS", "STATE", "TYPE", "NAME"));
        final SortedSet<Subsystem> subsystems = new TreeSet<>(SubsystemComparator.INSTANCE);
        listChildren(root, subsystems);

        for (final Subsystem subsystem : subsystems) {
            final List<String> parents;
            if (subsystem.getState() != Subsystem.State.INSTALL_FAILED && subsystem.getState() != Subsystem.State.UNINSTALLED) {
                parents = subsystem.getParents().stream()
                        .map(s -> s.getSubsystemId())
                        .distinct()
                        .map(id -> String.valueOf(id))
                        .sorted()
                        .collect(Collectors.toList());
            } else {
                parents = Collections.emptyList();
            }

            final StringBuilder buf = new StringBuilder(64);
            if (showSymbolicName) {
                buf.append(subsystem.getSymbolicName());
            } else {
                buf.append(subsystem.getSubsystemHeaders(null)
                        .getOrDefault(SubsystemConstants.SUBSYSTEM_NAME, subsystem.getSymbolicName()));
            }
            final Version version = subsystem.getVersion();
            if (!Version.emptyVersion.equals(version)) {
                buf.append(" (").append(version).append(")");
            }

            table.add(
                    asList(
                            String.valueOf(subsystem.getSubsystemId()),
                            String.join(", ", parents),
                            SUBSYSTEM_STATES.getOrDefault(subsystem.getState(), "<unknown>"),
                            subsystem.getType(),
                            buf.toString()
                    ));
        }

        table.render(session.getConsole());
    }

    private void listChildren(Subsystem parent, Set<Subsystem> subsystems) {
        subsystems.add(parent);
        if (parent.getState() != Subsystem.State.INSTALL_FAILED && parent.getState() != Subsystem.State.UNINSTALLED) {
            for (final Subsystem child : parent.getChildren()) {
                listChildren(child, subsystems);
            }
        }
    }

    @Descriptor("Start a subsystem")
    public void start(@Descriptor("subsystem to start") Subsystem sys) {
        sys.start();
    }

    @Descriptor("Stop a subsystem")
    public void stop(@Descriptor("subsystem to stop") Subsystem sys) {
        sys.stop();
    }

    @Descriptor("Install a subsystem")
    public void install(@Descriptor("subsystem location") String location) {
        root.install(location);
    }

    @Descriptor("Uninstall a subsystem")
    public void uninstall(@Descriptor("subsystem to uninstall") Subsystem sys) {
        sys.uninstall();
    }

    @Descriptor("Display subsystem headers")
    public void headers(CommandSession session,
                        @Descriptor("subsystem to inspect") Subsystem subsystem) {
        final Map<String, String> headers = subsystem.getSubsystemHeaders(null);
        final SortedMap<String, String> sortedHeaders = new TreeMap<>();
        for (final Map.Entry<String, String> e : headers.entrySet()) {
            final String key = e.getKey();
            final String rawValue = e.getValue();
            try {
                final Clause[] clauses = Parser.parseHeader(rawValue);
                final String value = String.join(", ",
                        Arrays.asList(clauses).stream()
                                .map(c -> c.toString())
                                .collect(Collectors.toList()));
                sortedHeaders.put(key, value);
            } catch (IllegalArgumentException ignore) {
            }
        }
        final PrintStream out = session.getConsole();
        for (final Map.Entry<String, String> e : sortedHeaders.entrySet()) {
            out.println(e.getKey() + ": " + e.getValue());
        }
    }

    private static class SubsystemComparator implements Comparator<Subsystem> {
        public static final Comparator<Subsystem> INSTANCE = new SubsystemComparator();

        @Override
        public int compare(Subsystem o1, Subsystem o2) {
            final long id1 = o1.getSubsystemId();
            final long id2 = o2.getSubsystemId();
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
