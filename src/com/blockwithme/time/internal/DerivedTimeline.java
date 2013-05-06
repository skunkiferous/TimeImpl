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

import java.util.Objects;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.Scheduler;
import com.blockwithme.time.TimelineBuilder;
import com.blockwithme.time.implapi._Scheduler;
import com.blockwithme.time.implapi._Timeline;

/**
 * All timelines, except for the core timeline, are DerivedTimeline2.
 *
 * @author monster
 */
public class DerivedTimeline extends TimelineImpl {

    /** The parent timeline. */
    private final _Timeline parent;

    /**
     * @param theName
     * @param theStartTime
     * @param theTimeOffset
     * @param theLoopWhenReachingEnd
     * @param theLocalTickScaling
     * @param theFixedDurationTicks
     * @param theLocalTickStep
     */
    public DerivedTimeline(final String theName, final long theStartTime,
            final double theTimeOffset, final boolean theLoopWhenReachingEnd,
            final double theLocalTickScaling, final long theFixedDurationTicks,
            final double theLocalTickStep, final _Timeline theParent,
            final _Scheduler scheduler) {
        super(theName, theStartTime, theTimeOffset, theLoopWhenReachingEnd,
                theLocalTickScaling, theFixedDurationTicks, theLocalTickStep);
        parent = Objects.requireNonNull(theParent, "theParent cannot be null");
        scheduler.queue(this);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#globalTickStep()
     */
    @Override
    public double globalTickStep() {
        return localTickStep() * parent.globalTickStep();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#globalTickScaling()
     */
    @Override
    public double globalTickScaling() {
        return localTickScaling() * parent.globalTickScaling();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockServiceSource#clockService()
     */
    @Override
    public ClockService clockService() {
        return parent.clockService();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#newSiblingTimeline(boolean)
     */
    @Override
    public TimelineBuilder newSiblingTimeline(final boolean cloneState,
            final Scheduler scheduler) {
        return new TimelineBuilderImpl2(this, parent, cloneState,
                (_Scheduler) scheduler);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#pausedGlobally()
     */
    @Override
    public boolean pausedGlobally() {
        return pausedLocally() || parent.pausedGlobally();
    }
}
