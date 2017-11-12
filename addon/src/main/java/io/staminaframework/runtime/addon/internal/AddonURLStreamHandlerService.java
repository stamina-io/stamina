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

package io.staminaframework.runtime.addon.internal;

import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * This component is responsible for locating addons from URL strings.
 *
 * @author Stamina Framework developers
 */
@Component(service = URLStreamHandlerService.class,
        property = {
                URLConstants.URL_HANDLER_PROTOCOL + "=addon",
                URLConstants.URL_CONTENT_MIMETYPE + "=application/vnd.osgi.subsystem"
        })
public class AddonURLStreamHandlerService extends AbstractURLStreamHandlerService {
    private static final int START_INDEX = "addon:".length();
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<Repository> repositories = Collections.emptyList();

    @Override
    public URLConnection openConnection(URL u) throws IOException {
        // Addon URL can follow these patterns:
        // - addon:symbolicname
        // - addon:symbolicname/version

        String symbolicName = null;
        String version = null;

        final String uStr = u.toExternalForm();
        final int i = uStr.indexOf('/');
        if (i == -1) {
            symbolicName = uStr.substring(START_INDEX);
        } else {
            symbolicName = uStr.substring(START_INDEX, i);
            if (i != uStr.length() - 1) {
                version = uStr.substring(i + 1);
            }
        }

        if (symbolicName == null && version == null) {
            throw new IOException("Malformed addon URL: " + u);
        }

        final Requirement req = new AddonRequirement(symbolicName, version);
        final List<Capability> results = new ArrayList<>();

        for (final Repository repo : repositories) {
            final Map<Requirement, Collection<Capability>> providers =
                    repo.findProviders(Collections.singleton(req));
            results.addAll(providers.get(req));
        }

        if (results.isEmpty()) {
            throw new IOException("Addon not found: " + u);
        }

        Resource res = null;
        Version resVersion = null;

        for (final Capability cap : results) {
            final Version candidateVersion = (Version) cap.getAttributes().get("version");
            if (candidateVersion == null) {
                continue;
            }
            if (resVersion == null || candidateVersion.compareTo(resVersion) == 1) {
                resVersion = candidateVersion;
                res = cap.getResource();
            }
        }
        if (res == null) {
            res = results.get(0).getResource();
        }

        final RepositoryContent resContent = (RepositoryContent) res;
        return new AddonURLConnection(u, resContent);
    }
}
