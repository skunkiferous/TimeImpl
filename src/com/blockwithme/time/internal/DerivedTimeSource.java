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
import com.blockwithme.time.Task;
import com.blockwithme.time.Time;
import com.blockwithme.time.TimeListener;
import com.blockwithme.time.TimeSource;

/**
 * A DerivedTimeSource derives it's ticks from another TimeSource.
 *
 * @author monster
 */
public class DerivedTimeSource extends AbstractTimeSource implements
        TimeListener {

    /** The TimeSource. */
    private final TimeSource timeSource;

    /** The task representing this TimeSource. */
    private final Task<TimeListener> task;

    /**
     * Creates a CoreTimeSource from a Scheduler.
     */
    public DerivedTimeSource(final TimeSource theTimeSource,
            final String theName, final boolean pausedAtStart,
            final boolean inheritTickCount) {
        super(theTimeSource.clockService().currentTimeNanos(), theName);
        timeSource = theTimeSource;
        if (pausedAtStart) {
            pause();
        }
        if (inheritTickCount) {
            setTicks(timeSource.ticks());
        }
        task = timeSource.registerListener(this);
    }

    @Override
    public void close() throws Exception {
        task.close();
    }

    /** Returns the ClockService that created this TimeSource. */
    @Override
    public ClockService clockService() {
        return timeSource.clockService();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#tickPeriode()
     */
    @Override
    public long tickPeriode() {
        return clockDivider * timeSource.tickPeriode();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeListener#onTimeChange(com.blockwithme.time.Time)
     */
    @Override
    public void onTimeChange(final Time time) {
        tick(time.tickStep, time.tickTime);
    }
}
