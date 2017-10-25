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

package io.staminaframework.addon.internal;

import io.staminaframework.command.Command;
import io.staminaframework.command.CommandConstants;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

/**
 * This command allows you to install Stamina addons.
 *
 * @author Stamina Framework developers
 */
@Component(service = Command.class,
        property = {
                CommandConstants.COMMAND + "=addon:install",
                CommandProcessor.COMMAND_SCOPE + "=addon",
                CommandProcessor.COMMAND_FUNCTION + "=install",
        })
public class AddonCommand implements Command {
    @Reference
    private LogService logService;
    @Reference(target = "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=0)")
    private Subsystem root;

    @Override
    public void help(PrintStream out) {
        out.println("Install addons.");
        out.println("Addon argument must respect one of these patterns:");
        out.println(" - symbolic.name");
        out.println(" - symbolic.name/version");
        out.println("Usage: addon:install <addon spec>");
    }

    @Override
    public boolean execute(Context context) throws Exception {
        if (context.arguments().length == 0) {
            help(context.out());
            return false;
        }
        for (final String arg : context.arguments()) {
            context.out().println("Installing addon: " + arg);
            doInstallAddon(arg);
        }
        return false;
    }

    @Descriptor("Install addon")
    public void install(@Descriptor("addon spec") String addonSpec) throws IOException {
        doInstallAddon(addonSpec);
    }

    private void doInstallAddon(String addonSpec) throws IOException {
        final URL addonUrl = new URL("addon:" + addonSpec);
        logService.log(LogService.LOG_INFO, "Installing addon: " + addonUrl);
        final Subsystem subsystem = root.install(addonUrl.toExternalForm());

        logService.log(LogService.LOG_INFO, "Starting addon: " + addonUrl);
        subsystem.start();
    }
}
