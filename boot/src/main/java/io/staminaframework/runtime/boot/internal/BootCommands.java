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

import io.staminaframework.runtime.asciitable.AsciiTable;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.lang.management.ManagementFactory;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;

import static java.util.Arrays.asList;

/**
 * Boot related commands.
 *
 * @author Stamina Framework developers
 */
@Component(service = BootCommands.class,
        property = {
                CommandProcessor.COMMAND_SCOPE + "=boot",
                CommandProcessor.COMMAND_FUNCTION + "=info",
                CommandProcessor.COMMAND_FUNCTION + "=version",
        }
)
public class BootCommands {
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        this.bundleContext = bundleContext;
    }

    @Descriptor("Display platform information")
    public void info(CommandSession session) throws InvalidSyntaxException {
        final AsciiTable table = AsciiTable.of(asList("NAME", "VALUE"));
        table.add(asList("Java version", System.getProperty("java.version")));
        table.add(asList("Java home", System.getProperty("java.home")));
        table.add(asList("Stamina Framework version", bundleContext.getBundle().getVersion().toString()));
        table.add(asList("Stamina Framework home", System.getProperty("stamina.home")));
        table.add(asList("Installed bundles", String.valueOf(bundleContext.getBundles().length)));

        final ServiceReference<?>[] subsystemRefs = bundleContext.getServiceReferences("org.osgi.service.subsystem.Subsystem", null);
        final int subsystemCount = subsystemRefs != null ? subsystemRefs.length : 0;
        table.add(asList("Installed subsystems", String.valueOf(subsystemCount)));

        final Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
        final long hours = uptime.toHours();
        final int minutes = (int) ((uptime.getSeconds() % (60 * 60)) / 60);
        final int seconds = (int) (uptime.getSeconds() % 60);
        final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.ENGLISH);
        nf.setMinimumIntegerDigits(2);
        nf.setGroupingUsed(false);
        final String uptimeStr;
        if (hours > 10) {
            uptimeStr = hours + ":" + nf.format(minutes) + ":" + nf.format(seconds);
        } else if (hours > 0) {
            uptimeStr = nf.format(hours) + ":" + nf.format(minutes) + ":" + nf.format(seconds);
        } else if (minutes > 10) {
            uptimeStr = nf.format(minutes) + ":" + nf.format(seconds);
        } else if (minutes > 0) {
            uptimeStr = minutes + ":" + nf.format(seconds);
        } else {
            uptimeStr = seconds + " seconds";
        }
        table.add(asList("Uptime", uptimeStr));

        table.render(session.getConsole());
    }

    @Descriptor("Display platform version")
    public void version(CommandSession session) {
        final String version = bundleContext.getBundle().getVersion().toString();
        session.getConsole().println(version);
    }
}
