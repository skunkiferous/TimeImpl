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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Task;
import com.blockwithme.time.Time;
import com.blockwithme.time.TimelineBuilder;
import com.blockwithme.time.implapi.CoreScheduler;
import com.blockwithme.time.implapi.Ticker;

/**
 * Implements the Core Timeline.
 *
 * @author monster
 */
public class CoreTimeline extends TimelineImpl {

    /** Logger */
    private static final Logger LOG = LoggerFactory
            .getLogger(CoreTimeline.class);

    /** The ClockService */
    private final ClockService clockService;

    /** The task representing this Timeline. */
    private final Task<Ticker> task;

    /** The global tick period. */
    private final long tickPeriod;

    public CoreTimeline(final ClockService theClockService,
            final CoreScheduler scheduler) {
        super("core timeline", theClockService.currentTimeMicros(), 0, false,
                1, 0, 1);
        clockService = theClockService;
        tickPeriod = Math.round(((double) Time.SECOND_MUS)
                / scheduler.ticksPerSecond());
        task = scheduler.scheduleTicker(this);
        pause();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#globalTickStep()
     */
    @Override
    public double globalTickStep() {
        return 1;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#globalTickScaling()
     */
    @Override
    public double globalTickScaling() {
        return 1;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockServiceSource#clockService()
     */
    @Override
    public ClockService clockService() {
        return clockService;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#tickPeriod()
     */
    @Override
    public long tickPeriod() {
        return tickPeriod;
    }

    /* (non-Javadoc)
     * @see java.lang.AutoCloseable#afterClose()
     */
    @Override
    protected void afterClose() {
        try {
            task.close();
        } catch (final Exception e) {
            LOG.error("Error while closing core timeline", e);
        }
        LOG.info("Core Timeline closed: " + this);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#newSiblingTimeline(boolean)
     */
    @Override
    public TimelineBuilder newSiblingTimeline(final boolean cloneState,
            final Scheduler scheduler) {
        throw new UnsupportedOperationException(this + " cannot have siblings!");
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#pausedGlobally()
     */
    @Override
    public boolean pausedGlobally() {
        return pausedLocally();
    }
}
