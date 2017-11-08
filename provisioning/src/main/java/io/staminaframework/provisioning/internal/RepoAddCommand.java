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

package io.staminaframework.provisioning.internal;

import io.staminaframework.command.Command;
import io.staminaframework.command.CommandConstants;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Command for adding an OBR URL.
 *
 * @author Stamina Framework developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND + "=repo:add")
public class RepoAddCommand implements Command {
    @Reference
    private RepositoryAdmin repositoryAdmin;
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
        out.println("Register an OBR URL.");
        out.println("Usage: repo:add <obr url>");
    }

    @Override
    public boolean execute(Context context) throws Exception {
        if (context.arguments().length != 1) {
            help(context.out());
            return false;
        }

        final String newUrl = context.arguments()[0];
        repositoryAdmin.addRepository(newUrl);

        final Path confDir = FileSystems.getDefault().getPath(
                bundleContext.getProperty("stamina.conf"));
        final Path confFile = confDir.resolve("custom.properties");
        if (!Files.exists(confFile)) {
            Files.createFile(confFile);
        }

        final String urlsStr = bundleContext.getProperty("obr.repository.url");
        final Set<String> urls;
        if (urlsStr != null) {
            urls = new HashSet<>(Arrays.asList(urlsStr.split(" ")));
            if (!urls.contains(newUrl)) {
                urls.add(newUrl);
            }
        } else {
            urls = Collections.singleton(newUrl);
        }

        final Properties conf = new Properties(confFile.toFile(), bundleContext);
        conf.put("obr.repository.url", "# Space separated list of OBR URLs",
                String.join(" ", urls));
        conf.save();

        return false;
    }
}
