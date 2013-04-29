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

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import org.threeten.bp.Instant;

import com.blockwithme.time.Time;
import com.blockwithme.time.Timeline;
import com.blockwithme.time.TimelineBuilder;

/**
 * An implementation of TimelineBuilder.
 *
 * @author monster
 */
public class TimelineBuilderImpl implements TimelineBuilder {

    /** The name of the new Timeline. */
    private final String name;

    /** Tzhe parent timeline. */
    private final Timeline source;

    /** Paused state at creation. */
    private Boolean paused;

    /** The ticks count to set. */
    private Long ticks;

    /** The ticks count to offset. */
    private Long ticksOffset;

    /** The real time in nanoseconds to set. */
    private Long realTimeNanos;

    /** The real-time offset, in nanoseconds. */
    private Long realTimeNanosOffset;

    /** The clockDivider. */
    private Integer clockDivider;

    /** The clockDivider factor. */
    private Float clockDividerFactor;

    /** Creates a new TimelineBuilderImpl, specifying the name of the new Timeline.  */
    public TimelineBuilderImpl(final String theName, final Timeline theSource) {
        name = Objects.requireNonNull(theName, "theName");
        source = Objects.requireNonNull(theSource, "theSource");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("theName cannot be empty");
        }
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#create()
     */
    @Override
    public Timeline create() {
        final boolean paused = (this.paused == null) ? source.paused()
                : this.paused;
        final long ticks;
        if (this.ticks != null) {
            ticks = this.ticks;
        } else if (ticksOffset != null) {
            ticks = source.ticks() + ticksOffset;
        } else {
            ticks = source.ticks();
        }
        final long realTimeNanosOffset;
        if (this.realTimeNanosOffset != null) {
            realTimeNanosOffset = this.realTimeNanosOffset
                    + source.realTimeOffset();
        } else if (this.realTimeNanos != null) {
            // TODO Test this!
            realTimeNanosOffset = (realTimeNanos - source.clockService()
                    .currentTimeNanos()) + source.realTimeOffset();
        } else {
            realTimeNanosOffset = source.realTimeOffset();
        }
        final int clockDivider;
        if (this.clockDivider != null) {
            clockDivider = this.clockDivider;
        } else if (clockDividerFactor != null) {
            int tmp = Math.round(source.clockDivider() * clockDividerFactor);
            if (tmp == 0) {
                final int cdfSign = (clockDividerFactor < 0.0) ? -1 : 1;
                final int scdSign = (source.clockDivider() < 0) ? -1 : 1;
                tmp = cdfSign * scdSign;
            }
            clockDivider = tmp;
        } else {
            clockDivider = source.clockDivider();
        }
        return new DerivedTimeline(source, name, paused, ticks,
                realTimeNanosOffset, clockDivider);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#pause()
     */
    @Override
    public TimelineBuilder pause() {
        paused = true;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#unpause()
     */
    @Override
    public TimelineBuilder unpause() {
        paused = false;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#setTickerCount(long)
     */
    @Override
    public TimelineBuilder setTickerCount(final long ticks) {
        this.ticks = ticks;
        this.ticksOffset = null;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#offsetTickerCount(long)
     */
    @Override
    public TimelineBuilder offsetTickerCount(final long ticksOffset) {
        this.ticks = null;
        this.ticksOffset = ticksOffset;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#setRealTime(long)
     */
    @Override
    public TimelineBuilder setRealTime(final long realTimeNanos) {
        this.realTimeNanos = realTimeNanos;
        realTimeNanosOffset = null;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#setRealTime(java.util.Date)
     */
    @Override
    public TimelineBuilder setRealTime(final Date realTime) {
        return setRealTime(realTime.getTime());
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#setRealTime(java.util.Calendar)
     */
    @Override
    public TimelineBuilder setRealTime(final Calendar realTime) {
        return setRealTime(realTime.getTime());
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#setRealTime(org.threeten.bp.Instant)
     */
    @Override
    public TimelineBuilder setRealTime(final Instant realTime) {
        final long millis = realTime.toEpochMilli();
        final int nanos = realTime.getNano();
        return setRealTime((millis / Time.MILLI_NS) + nanos);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#offsetRealTime(long)
     */
    @Override
    public TimelineBuilder offsetRealTime(final long realTimeNanosOffset) {
        realTimeNanos = null;
        this.realTimeNanosOffset = realTimeNanosOffset;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#setClockDivider(int)
     */
    @Override
    public TimelineBuilder setClockDivider(final int clockDivider) {
        if (clockDivider == 0) {
            throw new IllegalArgumentException("clockDivider cannot be 0");
        }
        this.clockDivider = clockDivider;
        clockDividerFactor = null;
        return this;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimelineBuilder#multiplyClockDivider(float)
     */
    @Override
    public TimelineBuilder multiplyClockDivider(final float clockDividerFactor) {
        if ((-0.0001 <= clockDividerFactor) && (clockDividerFactor <= 0.0001)) {
            throw new IllegalArgumentException("clockDividerFactor cannot be 0");
        }
        this.clockDividerFactor = clockDividerFactor;
        clockDivider = null;
        return this;
    }
}
