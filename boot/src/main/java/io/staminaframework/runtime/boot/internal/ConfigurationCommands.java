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

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * ConfigurationAdmin related commands.
 *
 * @author Stamina Framework developers
 */
@Component(service = ConfigurationCommands.class,
        property = {
                CommandProcessor.COMMAND_SCOPE + "=config",
                CommandProcessor.COMMAND_FUNCTION + "=get",
                CommandProcessor.COMMAND_FUNCTION + "=list",
        })
public class ConfigurationCommands {
    @Reference
    private ConfigurationAdmin cm;

    @Descriptor("Display configuration PID list")
    public void list(CommandSession session) throws IOException, InvalidSyntaxException {
        final Configuration[] confs = cm.listConfigurations(null);
        if (confs == null || confs.length == 0) {
            return;
        }
        Arrays.sort(confs, ConfigurationComparator.INSTANCE);

        final PrintStream out = session.getConsole();
        for (final Configuration conf : confs) {
            out.println(conf.getPid());
        }
    }

    @Descriptor("Display all configurations")
    public void get(CommandSession session)
            throws IOException, InvalidSyntaxException {
        get(session, null);
    }

    @Descriptor("Display configuration from a PID")
    public void get(CommandSession session,
                    @Descriptor("configuration PID") String configurationPid)
            throws IOException, InvalidSyntaxException {
        final String filter;
        if (configurationPid == null) {
            filter = null;
        } else {
            filter = "(" + Constants.SERVICE_PID + "=" + configurationPid + ")";
        }
        final Configuration[] confs = cm.listConfigurations(filter);
        if (confs == null || confs.length == 0) {
            return;
        }
        Arrays.sort(confs, ConfigurationComparator.INSTANCE);

        final PrintStream out = session.getConsole();
        for (int i = 0; i < confs.length; ++i) {
            if (i != 0) {
                out.println("--------------------------------------------------");
            }
            final Configuration conf = confs[i];
            out.println("PID: " + conf.getPid());
            if (conf.getFactoryPid() != null) {
                out.println("Factory PID: " + conf.getFactoryPid());
            }
            if (conf.getBundleLocation() != null) {
                out.println("Bundle Location: " + conf.getBundleLocation());
            }
            final Dictionary<String, Object> props = conf.getProperties();
            if (props != null) {
                out.println("Properties:");
                final SortedSet<String> sortedKeys = new TreeSet<>();
                sortedKeys.addAll(Collections.list(props.keys()));

                for (final String key : sortedKeys) {
                    final Object value = props.get(key);
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
            }
        }
    }

    private static class ConfigurationComparator implements Comparator<Configuration> {
        public static final Comparator<Configuration> INSTANCE = new ConfigurationComparator();

        @Override
        public int compare(Configuration o1, Configuration o2) {
            final String p1 = o1.getPid();
            final String p2 = o2.getPid();
            return p1.compareTo(p2);
        }
    }
}
