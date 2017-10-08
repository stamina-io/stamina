/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.it;

import io.stamina.boot.helper.CommandLine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import javax.inject.Inject;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static io.stamina.it.TestHelper.*;
import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * Integration tests for bundle <code>io.stamina.boot.helper</code>.
 *
 * @author Stamina developers
 */
@RunWith(PaxExam.class)
public class BootHelperTest {
    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        return options(
                mavenBundle("io.stamina", "io.stamina.boot.helper").versionAsInProject().noStart()
                );
    }

    @Test
    public void testRegionDigraphWrite() throws BundleException {
        final Bundle regionBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        final File digraphFile = regionBundle.getDataFile("digraph");
        assertFalse(digraphFile.exists());
        lookupBundle(bundleContext, "io.stamina.boot.helper").start();
        assertTrue(digraphFile.exists());
    }

    @Test
    public void testCommandLine() throws IOException, BundleException {
        final Bundle bootHelperBundle = lookupBundle(bundleContext, "io.stamina.boot.helper");
        final File cmdFile = bootHelperBundle.getDataFile("cmd.dat");
        try (final DataOutputStream out = new DataOutputStream(new FileOutputStream(cmdFile))) {
            out.writeUTF("hello");
            out.writeInt(2);
            out.writeUTF("mr");
            out.writeUTF("bond");
        }
        lookupBundle(bundleContext, "io.stamina.boot.helper").start();

        final CommandLine cmd = lookupService(bundleContext, CommandLine.class);
        assertEquals("hello", cmd.command());
        assertArrayEquals(new String[]{"mr", "bond"}, cmd.arguments());
    }

    @Test
    public void testNoCommandLine() throws BundleException {
        lookupBundle(bundleContext, "io.stamina.boot.helper").start();
        assertNull(bundleContext.getServiceReference(CommandLine.class));
    }
}
