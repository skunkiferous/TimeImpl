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

import com.blockwithme.time.Interval;
import com.blockwithme.time.Time;
import com.blockwithme.time.Timeline;

/**
 * Represents a time Interval.
 *
 * @author monster
 */
public class IntervalImpl implements Interval {

    /** The source timeline. Can be null. */
    private final Timeline timeline;

    /** The start. */
    private final long start;

    /** The end. */
    private final long end;

    /** toString */
    private String toString;

    /**
     * Creates a IntervalImpl.
     */
    public IntervalImpl(final Timeline theTimeline, final long theStart,
            final long theEnd) {
        if (theEnd < theStart) {
            throw new IllegalArgumentException("end(" + theEnd + ") < start("
                    + theStart + ")");
        }
        timeline = theTimeline;
        start = theStart;
        end = theEnd;
    }

    @Override
    public int hashCode() {
        int result = (timeline == null) ? 13 : timeline.hashCode();
        result ^= start ^ (start >>> 32);
        result ^= end ^ (end >>> 32);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof IntervalImpl) {
            final IntervalImpl other = (IntervalImpl) obj;
            return (timeline == other.timeline) && (start == other.start)
                    && (end == other.end);
        }
        return false;
    }

    @Override
    public String toString() {
        if (toString == null) {
            final String tl = (timeline == null) ? null : timeline.name();
            toString = "Interval(timeline=" + tl + ",start=" + start + ",end="
                    + end + ")";
        }
        return toString;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#relativeTo()
     */
    @Override
    public Timeline relativeTo() {
        return timeline;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#start()
     */
    @Override
    public long start() {
        return start;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#end()
     */
    @Override
    public long end() {
        return end;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#isOpenStart()
     */
    @Override
    public boolean isOpenStart() {
        return (start == Long.MIN_VALUE);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#isOpenEnd()
     */
    @Override
    public boolean isOpenEnd() {
        return (end == Long.MAX_VALUE);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#isOpen()
     */
    @Override
    public boolean isOpen() {
        return isOpenStart() || isOpenEnd();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#duration()
     */
    @Override
    public Long duration() {
        return isOpen() ? null : (end - start);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#beforeInterval(long)
     */
    @Override
    public boolean beforeInterval(final long time) {
        return (time < start);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#afterInterval(long)
     */
    @Override
    public boolean afterInterval(final long time) {
        return (time > end);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#inInterval(long)
     */
    @Override
    public boolean inInterval(final long time) {
        return (start <= time) && (time <= end);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#beforeInterval(com.blockwithme.time.Time)
     */
    @Override
    public boolean beforeInterval(final Time time) {
        return beforeInterval(toTime(time));
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#afterInterval(com.blockwithme.time.Time)
     */
    @Override
    public boolean afterInterval(final Time time) {
        return afterInterval(toTime(time));
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Interval#inInterval(com.blockwithme.time.Time)
     */
    @Override
    public boolean inInterval(final Time time) {
        return inInterval(toTime(time));
    }

    /** Converts the Time instance to an appropriate time value. */
    private long toTime(final Time time) {
        if (timeline == null) {
            return time.creationTime;
        }
        if (timeline == time.source) {
            return time.runningElapsedTicks;
        }
        // TODO Convert
        throw new IllegalArgumentException("Cannot convert Time from "
                + time.source.name() + " to " + timeline.name());
    }
}
