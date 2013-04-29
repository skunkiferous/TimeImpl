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
import com.blockwithme.time.Timeline;

/**
 * A DerivedTimeline derives it's ticks from another Timeline.
 *
 * @author monster
 */
public class DerivedTimeline extends AbstractTimeline implements TimeListener {

    /** The Timeline. */
    private final Timeline timeline;

    /** The task representing this Timeline. */
    private final Task<TimeListener> task;

    /**
     * Creates a CoreTimeline from a Scheduler.
     */
    public DerivedTimeline(final Timeline theTimeline, final String theName,
            final boolean pausedAtStart, final long ticks,
            final long theRealTimeOffset, final int clockDivider) {
        super(theTimeline.clockService().currentTimeNanos(), theName,
                theRealTimeOffset);
        timeline = theTimeline;
        if (pausedAtStart) {
            pause();
        }
        setTicks(ticks);
        setClockDivider(clockDivider);
        task = timeline.registerListener(this);
    }

    @Override
    public void close() throws Exception {
        task.close();
    }

    /** Returns the ClockService that created this Timeline. */
    @Override
    public ClockService clockService() {
        return timeline.clockService();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline#tickPeriode()
     */
    @Override
    public long tickPeriode() {
        return clockDivider * timeline.tickPeriode();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeListener#onTimeChange(com.blockwithme.time.Time)
     */
    @Override
    public void onTimeChange(final Time time) {
        tick(time.tickStep, time.tickTime);
    }
}
