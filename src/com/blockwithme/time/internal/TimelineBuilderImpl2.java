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

import com.blockwithme.time.Time;
import com.blockwithme.time.Timeline;
import com.blockwithme.time.TimelineBuilder;
import com.blockwithme.time._Scheduler;

/**
 * TimelineBuilderImpl2 allows the creation of a new derived timeline.
 *
 * @author monster
 */
public class TimelineBuilderImpl2 implements TimelineBuilder {

    private final Timeline source;

    private final Timeline parent;

    private final _Scheduler scheduler;

    private boolean paused;

    private Long startTimePoint;

    private double localTickStep;

    private long fixedDurationTicks;

    private boolean loopWhenReachingEnd;

    private double localTickScaling;

    private double timeOffset;

    /** Creates a TimelineBuilderImpl2. */
    public TimelineBuilderImpl2(final Timeline theSource,
            final Timeline theParent, final boolean cloneState,
            final _Scheduler theScheduler) {
        source = theSource;
        parent = theParent;
        scheduler = theScheduler;
        if (cloneState) {
            paused = source.pausedLocally();
            startTimePoint = source.startTimePoint();
            localTickStep = source.localTickStep();
            fixedDurationTicks = source.fixedDurationTicks();
            loopWhenReachingEnd = source.loopWhenReachingEnd();
            localTickScaling = source.localTickScaling();
            timeOffset = source.timeOffset();
        } else {
            localTickStep = 1;
            localTickScaling = 1;
        }
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder2#create(java.lang.String)
     */
    @Override
    public Timeline create(final String name) {
        final long start = (startTimePoint == null) ? source.clockService()
                .currentTimeNanos() : startTimePoint;
        final DerivedTimeline result = new DerivedTimeline(name, start,
                timeOffset, loopWhenReachingEnd, localTickScaling,
                fixedDurationTicks, localTickStep, parent, scheduler);
        if (paused) {
            result.pause();
        }
        parent.registerListener(result);
        return result;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder2#paused()
     */
    @Override
    public TimelineBuilder paused() {
        paused = true;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder2#running()
     */
    @Override
    public TimelineBuilder running() {
        paused = false;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder2#setStartTimePoint(Long)
     */
    @Override
    public TimelineBuilder setStartTimePoint(final Long theStartTimePoint) {
        startTimePoint = theStartTimePoint;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder2#setLocalTickStep(double)
     */
    @Override
    public TimelineBuilder setLocalTickStep(final double theLocalTickStep) {
        // TODO Validate input!
        localTickStep = theLocalTickStep;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder2#setFixedDurationTicks(long)
     */
    @Override
    public TimelineBuilder setFixedDurationTicks(
            final long theFixedDurationTicks) {
        if (theFixedDurationTicks > 0) {
            throw new IllegalArgumentException(
                    "theFixedDurationTicks cannot be negative: "
                            + theFixedDurationTicks);
        }
        fixedDurationTicks = theFixedDurationTicks;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder2#setLoopWhenReachingEnd(boolean)
     */
    @Override
    public TimelineBuilder setLoopWhenReachingEnd(
            final boolean theLoopWhenReachingEnd) {
        loopWhenReachingEnd = theLoopWhenReachingEnd;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder2#setLocalTickScaling(double)
     */
    @Override
    public TimelineBuilder setLocalTickScaling(final double theLocalTickScaling) {
        // TODO Validate input!
        localTickScaling = theLocalTickScaling;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder2#setTimeOffset(double)
     */
    @Override
    public TimelineBuilder setTimeOffset(final double theTimeOffset) {
        timeOffset = theTimeOffset;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder2#setTicksPerSecond(double)
     */
    @Override
    public TimelineBuilder setTicksPerSecond(final double ticksPerSecond) {
        if (ticksPerSecond <= 1.0 / Time.DAY_SECONDS) {
            throw new IllegalArgumentException(
                    "ticksPerSecond must be more then 0: " + ticksPerSecond);
        }
        return setLocalTickStep(parent.ticksPerSecond() / ticksPerSecond);
    }
}
