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
import org.osgi.service.component.annotations.Component;

import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * System related commands.
 *
 * @author Stamina Framework developers
 */
@Component(service = SystemCommands.class,
        property = {
                CommandProcessor.COMMAND_SCOPE + "=system",
                CommandProcessor.COMMAND_FUNCTION + "=exit",
                CommandProcessor.COMMAND_FUNCTION + "=properties",
        })
public class SystemCommands {
    @Descriptor("Force exit process")
    public void exit() {
        System.exit(1);
    }

    @Descriptor("Display system properties")
    public void properties(CommandSession session) {
        final Properties sysProps = System.getProperties();
        final SortedMap<Object, Object> sortedSysProps = new TreeMap<>(sysProps);
        for (final Map.Entry<Object, Object> e : sortedSysProps.entrySet()) {
            final Object key = e.getKey();
            final Object value = e.getValue();
            session.getConsole().println(key + "=" + value);
        }
    }
}
