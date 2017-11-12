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

package io.staminaframework.runtime.it;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import static io.staminaframework.runtime.starter.it.OsgiHelper.createSubsystemFeature;
import static io.staminaframework.runtime.starter.it.OsgiHelper.lookupSubsystem;
import static io.staminaframework.runtime.starter.it.StaminaOptions.staminaDistribution;
import static org.ops4j.pax.exam.CoreOptions.options;

/**
 * Integration tests for bundle <code>io.staminaframework.runtime.subsystem</code>.
 *
 * @author Stamina Framework developers
 */
@RunWith(PaxExam.class)
public class SubsystemIT {
    @Inject
    private BundleContext bundleContext;
    @Inject
    private ConfigurationAdmin configurationAdmin;
    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    @Configuration
    public Option[] config() throws IOException {
        return options(
                staminaDistribution()
        );
    }

    private void setupAddons() throws IOException {
        final File addonDir = tempDir.newFolder("addons");
        createSubsystemFeature(new File(addonDir, "myaddon.esa"),
                "it.myaddon", "mvn:org.apache.commons/commons-lang3/3.6");

        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("felix.fileinstall.dir", addonDir.getPath());
        props.put("felix.fileinstall.poll", "1000");
        props.put("felix.fileinstall.noInitialDelay", "true");
        props.put("felix.fileinstall.log.level", "4");

        final org.osgi.service.cm.Configuration conf =
                configurationAdmin.createFactoryConfiguration("org.apache.felix.fileinstall", "?");
        conf.update(props);
    }

    @Test
    public void testDeploySubsystem() throws IOException, InterruptedException {
        setupAddons();
        lookupSubsystem(bundleContext, "it.myaddon", 2000);
    }
}
