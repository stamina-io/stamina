/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.boot;

import javax.annotation.Generated;

/**
 * Application version informations.
 *
 * @author Stamina.io developers
 */
@Generated("stamina.io")
public final class Version {
    /**
     * Application build.
     */
    public static final String BUILD = "${stamina.build}";
    /**
     * Application version.
     */
    public static final String VERSION = "${stamina.version}";

    private Version() {
    }
}
