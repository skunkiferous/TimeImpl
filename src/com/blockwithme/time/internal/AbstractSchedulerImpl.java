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
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.blockwithme.time.Clock;
import com.blockwithme.time.Scheduler;

/**
 * Abstract Scheduler implementation.
 *
 * @author monster
 */
public abstract class AbstractSchedulerImpl<T> implements Scheduler<T> {

    /** NS im MN. */
    private static final long MS2NS = 1000000L;

    /** The executor */
    protected final Scheduler.Executor<T> executor;

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

    /** @see schedule(TimerTask,java.util.Date) */
    private void schedule2(final T task, final Date timeUTC) {
        final long delayMS = timeUTC.getTime() - Clock.currentTimeMillis();
        schedule2(task, delayMS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    private void schedule2(final T task, final Date firstTimeUTC,
            final long periodMS) {
        final long delayMS = firstTimeUTC.getTime() - Clock.currentTimeMillis();
        schedule2(task, delayMS, periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    private void scheduleAtFixedRate2(final T task, final Date firstTimeUTC,
            final long periodMS) {
        final long delayMS = firstTimeUTC.getTime() - Clock.currentTimeMillis();
        scheduleAtFixedRate2(task, delayMS, periodMS);
    }

    /**
     *
     */
    public AbstractSchedulerImpl(final Scheduler.Executor<T> executor) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        this.executor = executor;
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public void schedule(final T task, final Date timeUTC) {
        schedule2(task, toUTCDate(timeUTC));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public void schedule(final T task, final Instant timeUTC) {
        schedule2(task, toUTCDate(timeUTC));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public void schedule(final T task, final ZonedDateTime dateTime) {
        schedule2(task, toUTCDate(dateTime));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public void schedule(final T task, final LocalDateTime dateTime) {
        schedule2(task, toUTCDate(dateTime));
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    @Override
    public void schedule(final T task, final Date firstTimeUTC,
            final long periodMS) {
        schedule2(task, toUTCDate(firstTimeUTC), periodMS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    @Override
    public void schedule(final T task, final Instant firstTimeUTC,
            final long periodMS) {
        schedule2(task, toUTCDate(firstTimeUTC), periodMS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    @Override
    public void schedule(final T task, final ZonedDateTime firstTime,
            final long periodMS) {
        schedule2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    @Override
    public void schedule(final T task, final LocalDateTime firstTime,
            final long periodMS) {
        schedule2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleNS(final T task, final Date firstTimeUTC,
            final long periodNS) {
        schedule2(task, firstTimeUTC, periodNS / MS2NS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleNS(final T task, final Instant firstTimeUTC,
            final long periodNS) {
        schedule2(task, toUTCDate(firstTimeUTC), periodNS / MS2NS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleNS(final T task, final ZonedDateTime firstTime,
            final long periodNS) {
        schedule2(task, toUTCDate(firstTime), periodNS / MS2NS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleNS(final T task, final LocalDateTime firstTime,
            final long periodNS) {
        schedule2(task, toUTCDate(firstTime), periodNS / MS2NS);
    }

    /** @see schedule(TimerTask,long) */
    @Override
    public void schedule(final T task, final long delayMS) {
        schedule2(task, delayMS);
    }

    /** @see schedule(TimerTask,long) */
    @Override
    public void scheduleNS(final T task, final long delayNS) {
        schedule2(task, delayNS / MS2NS);
    }

    /** @see schedule(TimerTask,long,long) */
    @Override
    public void schedule(final T task, final long delayMS, final long periodMS) {
        schedule2(task, delayMS, periodMS);
    }

    /** @see schedule(TimerTask,long,long) */
    @Override
    public void scheduleNS(final T task, final long delayNS, final long periodNS) {
        schedule2(task, delayNS / MS2NS, periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleAtFixedRate(final T task, final Date firstTimeUTC,
            final long periodMS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleAtFixedRate(final T task, final Instant firstTimeUTC,
            final long periodMS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleAtFixedRate(final T task,
            final ZonedDateTime firstTime, final long periodMS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleAtFixedRate(final T task,
            final LocalDateTime firstTime, final long periodMS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleAtFixedRateNS(final T task, final Date firstTimeUTC,
            final long periodNS) {
        scheduleAtFixedRate2(task, firstTimeUTC, periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleAtFixedRateNS(final T task, final Instant firstTimeUTC,
            final long periodNS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleAtFixedRateNS(final T task,
            final ZonedDateTime firstTime, final long periodNS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTime), periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public void scheduleAtFixedRateNS(final T task,
            final LocalDateTime firstTime, final long periodNS) {
        scheduleAtFixedRate2(task, toUTCDate(firstTime), periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,long,long) */
    @Override
    public void scheduleAtFixedRate(final T task, final long delayMS,
            final long periodMS) {
        scheduleAtFixedRate2(task, delayMS, periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,long,long) */
    @Override
    public void scheduleAtFixedRateNS(final T task, final long delayNS,
            final long periodNS) {
        scheduleAtFixedRate2(task, delayNS / MS2NS, periodNS / MS2NS);
    }

    /** @see schedule(TimerTask,long) */
    protected abstract void schedule2(final T task, final long delayMS);

    /** @see schedule(TimerTask,long,long) */
    protected abstract void schedule2(final T task, final long delayMS,
            final long periodMS);

    /** @see scheduleAtFixedRate(TimerTask,long,long) */
    protected abstract void scheduleAtFixedRate2(final T task,
            final long delayMS, final long periodMS);
}
