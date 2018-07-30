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

package io.staminaframework.runtime.log.bridge;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.helpers.CyclicBuffer;
import ch.qos.logback.core.status.ErrorStatus;

import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Logback {@link ch.qos.logback.core.Appender} implementation used to get access
 * to the last log entries from the OSGi platform.
 *
 * @author Stamina Framework developers
 */
public class OsgiAppender<E> extends UnsynchronizedAppenderBase<E> {
    private static final int BUFFER_CAPACITY = 50;
    private static final CyclicBuffer BUFFER = new CyclicBuffer(BUFFER_CAPACITY);
    private static final ReadWriteLock RWL = new ReentrantReadWriteLock();
    private Encoder<E> encoder;

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    @Override
    public void start() {
        int errors = 0;
        if (encoder == null) {
            addStatus(new ErrorStatus("No encoder set for the appender named \"" + name + "\".", this));
            ++errors;
        }
        if (errors == 0) {
            super.start();
        }
    }

    @Override
    protected void append(E e) {
        if (!isStarted()) {
            return;
        }
        final byte[] encoded = encoder.encode(e);
        final String msg = new String(encoded).trim();
        RWL.writeLock().lock();
        try {
            BUFFER.add(msg);
        } finally {
            RWL.writeLock().unlock();
        }
    }

    public Iterable<String> getLogEntries() {
        RWL.readLock().lock();
        try {
            return new ArrayList<>(BUFFER.asList());
        } finally {
            RWL.readLock().unlock();
        }
    }
}
