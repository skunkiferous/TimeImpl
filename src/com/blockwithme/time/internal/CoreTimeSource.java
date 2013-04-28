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

import com.blockwithme.time.ClockService;
import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Task;

/**
 * A CoreTimeSource derives it's ticks directly from a scheduler.
 *
 * @author monster
 */
public class CoreTimeSource extends AbstractTimeSource implements Runnable {

    /** The duration of a clock tick in nanoseconds. */
    private final long tickDurationNanos;

    /** The scheduler. */
    private final Scheduler scheduler;

    /** The task representing this TimeSource. */
    private final Task<Runnable> task;

    /**
     * Creates a CoreTimeSource from a Scheduler.
     * @param theName
     */
    public CoreTimeSource(final Scheduler theScheduler, final String theName) {
        super(theScheduler.clockService().currentTimeNanos(), theName);
        scheduler = theScheduler;
        task = scheduler.scheduleTicker(this);
        tickDurationNanos = scheduler.clockService().tickDurationNanos();
    }

    @Override
    public void close() throws Exception {
        task.close();
    }

    /** Returns the ClockService that created this TimeSource. */
    @Override
    public ClockService clockService() {
        return scheduler.clockService();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#tickPeriode()
     */
    @Override
    public long tickPeriode() {
        return parentRatio * tickDurationNanos;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        // core time sources always "assume" a parent step of +1 tick.
        tick(1, clockService().currentTimeNanos());
    }
}
