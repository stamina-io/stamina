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

package io.staminaframework.command.internal;

import io.staminaframework.command.Command;
import io.staminaframework.command.CommandConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.io.PrintStream;

/**
 * Command displaying platform version.
 *
 * @author Stamina Framework developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND + "=version")
public class VersionCommand implements Command {
    private String version;

    @Activate
    public void activate(BundleContext bundleContext) {
        version = bundleContext.getBundle().getVersion().toString();
    }

    @Override
    public boolean execute(Context ctx) throws Exception {
        ctx.out().println(version);
        return false;
    }

    @Override
    public void help(PrintStream out) {
        out.println("Show platform version.");
        out.println("Usage: version");
    }
}
