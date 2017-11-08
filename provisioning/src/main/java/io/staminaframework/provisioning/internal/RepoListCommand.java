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

import io.staminaframework.asciitable.AsciiTable;
import io.staminaframework.command.Command;
import io.staminaframework.command.CommandConstants;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Command for listing OBR URLs.
 *
 * @author Stamina Framework developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND + "=repo:list")
public class RepoListCommand implements Command {
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
        out.println("List OBR URLs.");
        out.println("Usage: repo:list");
    }

    @Override
    public boolean execute(Context context) throws Exception {
        if (context.arguments().length != 0) {
            help(context.out());
            return false;
        }

        final String urlsStr = bundleContext.getProperty("obr.repository.url");
        if (urlsStr == null) {
            return false;
        }

        final String[] urls = urlsStr.split(" ");

        final Repository[] repos = repoAdmin.listRepositories();
        if (repos != null) {
            Arrays.sort(repos, RepositoryComparator.INSTANCE);

            final AsciiTable table = AsciiTable.of(Arrays.asList("NAME", "URL"));
            for (final Repository repo : repos) {
                String repoUri = repo.getURI();
                for (final String url : urls) {
                    if (RepoUtils.sameObrUrl(url, repo.getURI())) {
                        repoUri = url;
                        break;
                    }
                }
                table.add(Arrays.asList(repo.getName(), repoUri));
            }
            table.render(context.out());
        }

        return false;
    }

    private static class RepositoryComparator implements Comparator<Repository> {
        public static final Comparator<Repository> INSTANCE = new RepositoryComparator();

        @Override
        public int compare(Repository o1, Repository o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
