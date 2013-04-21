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

import java.util.Date;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.blockwithme.time.Clock;
import com.blockwithme.time.Scheduler;

/**
 * Abstract Scheduler implementation.
 *
 * @author monster
 */
public abstract class AbstractSchedulerImpl implements Scheduler {

    /** NS im MN. */
    protected static final long MS2NS = 1000000L;

    /**
     * Converts an Date, assumed to be UTC, to a Date(!)
     */
    private static Date toUTCDate(final Date dateUTC) {
        return dateUTC;
    }

    /**
     * Converts an Instant, assumed to be UTC, to a Date.
     */
    private static Date toUTCDate(final Instant instantUTC) {
        return new Date(instantUTC.toEpochMilli());
    }

    /**
     * Converts an ZonedDateTime, to a Date.
     */
    private static Date toUTCDate(final ZonedDateTime dateTime) {
        return toUTCDate(dateTime.with(ZoneOffset.UTC).toInstant());
    }

    /**
     * Converts an LocalDateTime, to a Date.
     */
    private static Date toUTCDate(final LocalDateTime dateTime) {
        return toUTCDate(dateTime.atZone(ZoneOffset.UTC).toInstant());
    }

    /**
     * Converts an LocalTime, to a Date.
     */
    private static Date toUTCDate(final LocalTime time) {
        final org.threeten.bp.Clock clock = Clock.localClock();
        final LocalDate today = LocalDate.now(clock);
        final LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime dateTime = LocalDateTime.of(today, time);
        if (dateTime.compareTo(now) < 0) {
            final LocalDate tomorrow = today.plusDays(1);
            dateTime = LocalDateTime.of(tomorrow, time);
        }
        return toUTCDate(dateTime);
    }

    /** The error handler. */
    protected final Handler errorHandler;

    /**
     * Rounds a number of nano-seconds to a number of milli-seconds.
     * @param nanos nano-seconds
     * @return milli-seconds.
     */
    protected static long roundToMS(final long nanos) {
        if (nanos <= 0) {
            return 0;
        }
        final long rest = nanos % MS2NS;
        // Positive non-zero nanos is never rounded to 0.
        if ((rest >= MS2NS / 2) || (nanos < MS2NS / 2)) {
            return (nanos / MS2NS) + 1;
        }
        return (nanos / MS2NS);
    }

    /** @see schedule(TimerTask,java.util.Date) */
    private void scheduleImpl(final Runnable task, final Date timeUTC) {
        final long delayMS = timeUTC.getTime() - Clock.currentTimeMillis();
        scheduleNS(task, delayMS * MS2NS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    private void scheduleAtFixedPeriodImplNS(final Runnable task,
            final Date firstTimeUTC, final long periodNS) {
        final long delayMS = firstTimeUTC.getTime() - Clock.currentTimeMillis();
        scheduleAtFixedPeriodNS(task, delayMS * MS2NS, periodNS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    private void scheduleAtFixedRate2(final Runnable task,
            final Date firstTimeUTC, final long periodMS) {
        final long delayMS = firstTimeUTC.getTime() - Clock.currentTimeMillis();
        scheduleAtFixedRateNS(task, delayMS * MS2NS, periodMS * MS2NS);
    }

    /**
     * Creates a AbstractSchedulerImpl with an error handler.
     */
    protected AbstractSchedulerImpl(final Handler theErrorHandler) {
        if (theErrorHandler == null) {
            throw new IllegalArgumentException("theErrorHandler is null");
        }
        errorHandler = theErrorHandler;
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final void schedule(final Runnable task, final Date timeUTC) {
        scheduleImpl(task, toUTCDate(timeUTC));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final void schedule(final Runnable task, final Instant timeUTC) {
        scheduleImpl(task, toUTCDate(timeUTC));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final void schedule(final Runnable task, final ZonedDateTime dateTime) {
        scheduleImpl(task, toUTCDate(dateTime));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final void schedule(final Runnable task, final LocalDateTime dateTime) {
        scheduleImpl(task, toUTCDate(dateTime));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final void schedule(final Runnable task, final LocalTime time) {
        scheduleImpl(task, toUTCDate(time));
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedPeriod(final Runnable task,
            final Date firstTimeUTC, final long periodMS) {
        scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTimeUTC), periodMS
                * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedPeriod(final Runnable task,
            final Instant firstTimeUTC, final long periodMS) {
        scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTimeUTC), periodMS
                * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedPeriod(final Runnable task,
            final ZonedDateTime firstTime, final long periodMS) {
        scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodMS
                * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedPeriod(final Runnable task,
            final LocalDateTime firstTime, final long periodMS) {
        scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodMS
                * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedPeriod(final Runnable task,
            final LocalTime firstTime, final long periodMS) {
        scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodMS
                * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedPeriodNS(final Runnable task,
            final Date firstTimeUTC, final long periodNS) {
        scheduleAtFixedPeriodImplNS(task, firstTimeUTC, periodNS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedPeriodNS(final Runnable task,
            final Instant firstTimeUTC, final long periodNS) {
        scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTimeUTC), periodNS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedPeriodNS(final Runnable task,
            final ZonedDateTime firstTime, final long periodNS) {
        scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodNS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedPeriodNS(final Runnable task,
            final LocalDateTime firstTime, final long periodNS) {
        scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodNS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedPeriodNS(final Runnable task,
            final LocalTime firstTime, final long periodNS) {
        scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodNS);
    }

    /** @see schedule(TimerTask,long) */
    @Override
    public final void schedule(final Runnable task, final long delayMS) {
        scheduleNS(task, delayMS * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,long,long) */
    @Override
    public final void scheduleAtFixedPeriod(final Runnable task,
            final long delayMS, final long periodMS) {
        scheduleAtFixedPeriodNS(task, delayMS * MS2NS, periodMS * MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedRate(final Runnable task,
            final Date firstTimeUTC, final long periodMS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedRate(final Runnable task,
            final Instant firstTimeUTC, final long periodMS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedRate(final Runnable task,
            final ZonedDateTime firstTime, final long periodMS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedRate(final Runnable task,
            final LocalDateTime firstTime, final long periodMS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedRate(final Runnable task,
            final LocalTime firstTime, final long periodMS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedRateNS(final Runnable task,
            final Date firstTimeUTC, final long periodNS) {
        scheduleAtFixedRate2(task, firstTimeUTC, periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedRateNS(final Runnable task,
            final Instant firstTimeUTC, final long periodNS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedRateNS(final Runnable task,
            final ZonedDateTime firstTime, final long periodNS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTime), periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedRateNS(final Runnable task,
            final LocalDateTime firstTime, final long periodNS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTime), periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final void scheduleAtFixedRateNS(final Runnable task,
            final LocalTime firstTime, final long periodNS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTime), periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,long,long) */
    @Override
    public final void scheduleAtFixedRate(final Runnable task,
            final long delayMS, final long periodMS) {
        scheduleAtFixedRateNS(task, delayMS * MS2NS, periodMS * MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,long,long) */
    @Override
    public abstract void scheduleAtFixedRateNS(final Runnable task,
            final long delayNS, final long periodNS);

    /** @see scheduleAtFixedPeriod(TimerTask,long,long) */
    @Override
    public abstract void scheduleAtFixedPeriodNS(final Runnable task,
            final long delayNS, final long periodNS);

    /** @see schedule(TimerTask,long) */
    @Override
    public abstract void scheduleNS(final Runnable task, final long delayNS);
}
