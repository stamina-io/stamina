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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Framework related commands.
 *
 * @author Stamina Framework developers
 */
@Component(service = FrameworkCommands.class,
        property = {
                CommandProcessor.COMMAND_SCOPE + "=framework",
                CommandProcessor.COMMAND_FUNCTION + "=shutdown",
                CommandProcessor.COMMAND_FUNCTION + "=startlevel",
        })
public class FrameworkCommands {
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        this.bundleContext = null;
    }

    @Descriptor("Gracefully shutdown platform")
    public void shutdown(CommandSession session) throws BundleException {
        session.getConsole().println("Platform is about to shut down");
        bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).stop();
    }

    @Descriptor("Get framework start level")
    public void startlevel(CommandSession session) {
        final Bundle sysBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        final FrameworkStartLevel fsl = sysBundle.adapt(FrameworkStartLevel.class);
        final int startLevel = fsl.getStartLevel();
        session.getConsole().println(startLevel);
    }

    @Descriptor("Set framework start level")
    public void startlevel(CommandSession session, int startLevel) {
        final Bundle sysBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        final FrameworkStartLevel fsl = sysBundle.adapt(FrameworkStartLevel.class);
        session.getConsole().println("Set framework start level to " + startLevel);
        fsl.setStartLevel(startLevel);
    }
}
