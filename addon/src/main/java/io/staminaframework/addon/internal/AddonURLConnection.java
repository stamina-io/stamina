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

import org.osgi.service.repository.RepositoryContent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

class AddonURLConnection extends URLConnection {
    private final RepositoryContent repositoryContent;

    public AddonURLConnection(final URL url, final RepositoryContent repositoryContent) {
        super(url);
        this.repositoryContent = repositoryContent;
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public String getContentType() {
        return "application/vnd.osgi.subsystem";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return repositoryContent.getContent();
    }

    @Override
    public String toString() {
        return "AddonURLConnection[url=" + url + "]";
    }
}
