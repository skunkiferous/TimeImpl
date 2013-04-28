/*
 * Copyright (C) 2013 Sebastien Diot.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blockwithme.time.internal;

import junit.framework.TestCase;

import com.blockwithme.time.ClockService;

/**
 * @author monster
 *
 */
public abstract class TestBase extends TestCase {

    protected static void sleep(final long sleep) {
        try {
            Thread.sleep(sleep);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected static void close(final AutoCloseable ac) {
        try {
            ac.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    protected static ClockService newClockService() {
        return new ClockServiceImpl(false, new TimerCoreScheduler(
                1000000000L / ClockServiceImpl.TICKS_PER_SECONDS),
                ClockServiceImpl.TICKS_PER_SECONDS, new NTPClockSynchronizer(),
                new HTTPClockSynchronizer());
    }
}
