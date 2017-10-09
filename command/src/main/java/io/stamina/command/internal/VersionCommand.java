/*
 * Copyright (c) 2017 Stamina.io developers.
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

package io.stamina.command.internal;

import io.stamina.command.Command;
import io.stamina.command.CommandConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.io.PrintStream;

/**
 * Command displaying platform version.
 *
 * @author Stamina.io developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND_PROPERTY + "=version")
public class VersionCommand implements Command {
    private String version;

    @Activate
    public void activate(BundleContext bundleContext) {
        version = bundleContext.getBundle().getVersion().toString();
    }

    @Override
    public void execute(Context ctx) throws Exception {
        ctx.out().println(version);
    }

    @Override
    public void help(PrintStream out) {
        out.println("Show platform version.");
        out.println("Usage: version");
    }
}
