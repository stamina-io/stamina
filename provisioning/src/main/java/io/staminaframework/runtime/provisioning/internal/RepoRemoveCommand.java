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

package io.staminaframework.runtime.provisioning.internal;

import io.staminaframework.runtime.command.Command;
import io.staminaframework.runtime.command.CommandConstants;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.utils.properties.Properties;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Command for removing an OBR URL.
 *
 * @author Stamina Framework developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND + "=repo:remove")
public class RepoRemoveCommand implements Command {
    @Reference
    private RepositoryAdmin repoAdmin;
    private BundleContext bundleContext;

    @Activate
    void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    void deactivate() {
        this.bundleContext = null;
    }

    @Override
    public void help(PrintStream out) {
        out.println("Unregister an OBR URL.");
        out.println("Usage: repo:remove <obr url>");
    }

    @Override
    public boolean execute(Context context) throws Exception {
        if (context.arguments().length != 1) {
            help(context.out());
            return false;
        }

        final String urlToRemove = context.arguments()[0];
        repoAdmin.removeRepository(urlToRemove);

        final String urlsStr = bundleContext.getProperty("obr.repository.url");
        if (urlsStr != null) {
            final Set<String> urls = new HashSet<>(Arrays.asList(urlsStr.split(" ")));
            urls.remove(urlToRemove);

            final Path confDir = FileSystems.getDefault().getPath(
                    bundleContext.getProperty("stamina.conf"));
            final Path confFile = confDir.resolve("custom.properties");
            if (!Files.exists(confFile)) {
                Files.createFile(confFile);
            }

            final Properties conf = new Properties(confFile.toFile(), bundleContext);
            conf.put("obr.repository.url", "# Space separated list of OBR URLs",
                    String.join(" ", urls));
            conf.save();
        }

        return false;
    }
}
