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

package io.staminaframework.it;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import static io.staminaframework.starter.it.StaminaOptions.staminaDistribution;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.options;

/**
 * Integration tests for bundle <code>io.staminaframework.addon</code>.
 *
 * @author Stamina Framework developers
 */
@RunWith(PaxExam.class)
public class AddonIT {
    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        final String obrFile = getClass().getResource("obr.xml").toExternalForm();
        return options(
                frameworkProperty("obr.repository.url").value(obrFile),
                staminaDistribution()
        );
    }

    @Test
    public void testBasic() throws IOException {
        final Path tmp = Files.createTempFile("stamina-addon-", ".esa");
        final URL addonUrl = new URL("addon:stamina-hello");
        try (final InputStream in = addonUrl.openStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        assertNotEquals(0, Files.size(tmp));
        assertSameFileContent(getClass().getResource("stamina-hello-1.0.0.esa.mock"), tmp.toUri().toURL());
    }

    @Test
    public void testSelectHighestVersion() throws IOException {
        final Path tmp = Files.createTempFile("stamina-addon-", ".esa");
        final URL addonUrl = new URL("addon:stamina-realm");
        try (final InputStream in = addonUrl.openStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        assertNotEquals(0, Files.size(tmp));
        assertSameFileContent(getClass().getResource("stamina-realm-1.3.0.esa.mock"), tmp.toUri().toURL());
    }

    @Test
    public void testSelectVersion() throws IOException {
        final Path tmp = Files.createTempFile("stamina-addon-", ".esa");
        final URL addonUrl = new URL("addon:stamina-realm/1.1.0");
        try (final InputStream in = addonUrl.openStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        assertNotEquals(0, Files.size(tmp));
        assertSameFileContent(getClass().getResource("stamina-realm-1.1.0.esa.mock"), tmp.toUri().toURL());
    }

    private void assertSameFileContent(URL expected, URL actual) throws IOException {
        final byte[] expectedContent = readAll(expected);
        final byte[] actualContent = readAll(actual);
        assertTrue("Files don't have the same content",
                Arrays.equals(expectedContent, actualContent));
    }

    private byte[] readAll(URL url) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (final InputStream in = url.openStream()) {
            final byte[] buf = new byte[1024];
            for (int bytesRead; (bytesRead = in.read(buf)) != -1; ) {
                result.write(buf, 0, bytesRead);
            }
        }
        return result.toByteArray();
    }
}
