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
import java.util.Objects;
import java.util.WeakHashMap;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.Task;
import com.blockwithme.time.Time;
import com.blockwithme.time.implapi.CoreScheduler;
import com.blockwithme.time.implapi._Scheduler;

/**
 * Lightweight Scheduler implementation.
 *
 * @author monster
 */
public class LightweightSchedulerImpl extends
        WeakHashMap<AutoCloseable, Object> implements _Scheduler {

    /** One milli-second, in microseconds. */
    private static final long MS2MUS = Time.MILLI_MUS;

    /** The error handler. */
    private final Handler errorHandler;

    /** The CoreScheduler */
    private final CoreScheduler coreScheduler;

    /** The ClockService */
    private final ClockService clockService;

    /** The name. */
    private final String name;

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
    private Date toUTCDate(final LocalTime time) {
        final org.threeten.bp.Clock clock = clockService.localClock();
        final LocalDate today = LocalDate.now(clock);
        final LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime dateTime = LocalDateTime.of(today, time);
        if (dateTime.compareTo(now) < 0) {
            final LocalDate tomorrow = today.plusDays(1);
            dateTime = LocalDateTime.of(tomorrow, time);
        }
        return toUTCDate(dateTime);
    }

    /**
     * Rounds a number of micro-seconds to a number of milli-seconds.
     * @param micros micro-seconds
     * @return milli-seconds.
     */
    public static long roundToMS(final long micros) {
        if (micros <= 0) {
            return 0;
        }
        final long rest = micros % MS2MUS;
        // Positive non-zero micros is never rounded to 0.
        if ((rest >= MS2MUS / 2) || (micros < MS2MUS / 2)) {
            return (micros / MS2MUS) + 1;
        }
        return (micros / MS2MUS);
    }

    /** @see schedule(TimerTask,java.util.Date) */
    private Task<Runnable> scheduleImpl(final Runnable task, final Date timeUTC) {
        final long delayMS = timeUTC.getTime()
                - clockService.currentTimeMillis();
        return scheduleOnceMUS(task, delayMS * MS2MUS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    private Task<Runnable> scheduleAtFixedPeriodImplMUS(final Runnable task,
            final Date firstTimeUTC, final long periodMUS) {
        final long delayMS = firstTimeUTC.getTime()
                - clockService.currentTimeMillis();
        return scheduleAtFixedPeriodMUS(task, delayMS * MS2MUS, periodMUS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    private Task<Runnable> scheduleAtFixedRate2(final Runnable task,
            final Date firstTimeUTC, final long periodMS) {
        final long delayMS = firstTimeUTC.getTime()
                - clockService.currentTimeMillis();
        return scheduleAtFixedRateMUS(task, delayMS * MS2MUS, periodMS * MS2MUS);
    }

    /**
     * Creates a LightweightSchedulerImpl with a CoreScheduler and an error handler.
     */
    public LightweightSchedulerImpl(final CoreScheduler theCoreScheduler,
            final Handler theErrorHandler, final ClockService theClockService,
            final String theName) {
        coreScheduler = Objects.requireNonNull(theCoreScheduler,
                "theCoreScheduler");
        errorHandler = Objects.requireNonNull(theErrorHandler,
                "theErrorHandler");
        clockService = Objects.requireNonNull(theClockService,
                "theClockService");
        if (theName == null) {
            throw new IllegalArgumentException("theName is null");
        }
        if (theName.isEmpty()) {
            throw new IllegalArgumentException("theName is empty");
        }
        name = theName;
    }

    /** toString() */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(name=" + name + ")";
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final Task<Runnable> scheduleOnce(final Runnable task,
            final Date timeUTC) {
        return scheduleImpl(task, toUTCDate(timeUTC));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final Task<Runnable> scheduleOnce(final Runnable task,
            final Instant timeUTC) {
        return scheduleImpl(task, toUTCDate(timeUTC));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final Task<Runnable> scheduleOnce(final Runnable task,
            final ZonedDateTime dateTime) {
        return scheduleImpl(task, toUTCDate(dateTime));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final Task<Runnable> scheduleOnce(final Runnable task,
            final LocalDateTime dateTime) {
        return scheduleImpl(task, toUTCDate(dateTime));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final Task<Runnable> scheduleOnce(final Runnable task,
            final LocalTime time) {
        return scheduleImpl(task, toUTCDate(time));
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriod(final Runnable task,
            final Date firstTimeUTC, final long periodMS) {
        return scheduleAtFixedPeriodImplMUS(task, toUTCDate(firstTimeUTC),
                periodMS * MS2MUS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriod(final Runnable task,
            final Instant firstTimeUTC, final long periodMS) {
        return scheduleAtFixedPeriodImplMUS(task, toUTCDate(firstTimeUTC),
                periodMS * MS2MUS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriod(final Runnable task,
            final ZonedDateTime firstTime, final long periodMS) {
        return scheduleAtFixedPeriodImplMUS(task, toUTCDate(firstTime),
                periodMS * MS2MUS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriod(final Runnable task,
            final LocalDateTime firstTime, final long periodMS) {
        return scheduleAtFixedPeriodImplMUS(task, toUTCDate(firstTime),
                periodMS * MS2MUS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriod(final Runnable task,
            final LocalTime firstTime, final long periodMS) {
        return scheduleAtFixedPeriodImplMUS(task, toUTCDate(firstTime),
                periodMS * MS2MUS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriodMUS(final Runnable task,
            final Date firstTimeUTC, final long periodMUS) {
        return scheduleAtFixedPeriodImplMUS(task, firstTimeUTC, periodMUS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriodMUS(final Runnable task,
            final Instant firstTimeUTC, final long periodMUS) {
        return scheduleAtFixedPeriodImplMUS(task, toUTCDate(firstTimeUTC),
                periodMUS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriodMUS(final Runnable task,
            final ZonedDateTime firstTime, final long periodMUS) {
        return scheduleAtFixedPeriodImplMUS(task, toUTCDate(firstTime),
                periodMUS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriodMUS(final Runnable task,
            final LocalDateTime firstTime, final long periodMUS) {
        return scheduleAtFixedPeriodImplMUS(task, toUTCDate(firstTime),
                periodMUS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriodMUS(final Runnable task,
            final LocalTime firstTime, final long periodMUS) {
        return scheduleAtFixedPeriodImplMUS(task, toUTCDate(firstTime),
                periodMUS);
    }

    /** @see schedule(TimerTask,long) */
    @Override
    public final Task<Runnable> scheduleOnce(final Runnable task,
            final long delayMS) {
        return scheduleOnceMUS(task, delayMS * MS2MUS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,long,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriod(final Runnable task,
            final long delayMS, final long periodMS) {
        return scheduleAtFixedPeriodMUS(task, delayMS * MS2MUS, periodMS
                * MS2MUS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRate(final Runnable task,
            final Date firstTimeUTC, final long periodMS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRate(final Runnable task,
            final Instant firstTimeUTC, final long periodMS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRate(final Runnable task,
            final ZonedDateTime firstTime, final long periodMS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRate(final Runnable task,
            final LocalDateTime firstTime, final long periodMS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRate(final Runnable task,
            final LocalTime firstTime, final long periodMS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRateMUS(final Runnable task,
            final Date firstTimeUTC, final long periodMUS) {
        return scheduleAtFixedRate2(task, firstTimeUTC, periodMUS / MS2MUS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRateMUS(final Runnable task,
            final Instant firstTimeUTC, final long periodMUS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodMUS
                / MS2MUS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRateMUS(final Runnable task,
            final ZonedDateTime firstTime, final long periodMUS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMUS
                / MS2MUS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRateMUS(final Runnable task,
            final LocalDateTime firstTime, final long periodMUS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMUS
                / MS2MUS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRateMUS(final Runnable task,
            final LocalTime firstTime, final long periodMUS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMUS
                / MS2MUS);
    }

    /** @see scheduleAtFixedRate(TimerTask,long,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRate(final Runnable task,
            final long delayMS, final long periodMS) {
        return scheduleAtFixedRateMUS(task, delayMS * MS2MUS, periodMS * MS2MUS);
    }

    /** @see scheduleAtFixedRate(TimerTask,long,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedRateMUS(final Runnable task,
            final long delayMUS, final long periodMUS) {
        return queue(coreScheduler.scheduleAtFixedRateMUS(task, errorHandler,
                delayMUS, periodMUS));
    }

    /** @see scheduleAtFixedPeriod(TimerTask,long,long) */
    @Override
    public final Task<Runnable> scheduleAtFixedPeriodMUS(final Runnable task,
            final long delayMUS, final long periodMUS) {
        return queue(coreScheduler.scheduleAtFixedPeriodMUS(task, errorHandler,
                delayMUS, periodMUS));
    }

    /** @see schedule(TimerTask,long) */
    @Override
    public final Task<Runnable> scheduleOnceMUS(final Runnable task,
            final long delayMUS) {
        return queue(coreScheduler.scheduleMUS(task, errorHandler, delayMUS));
    }

    /** Enqueues a task, so that they can be cancelled later. */
    @Override
    public <E extends AutoCloseable> E queue(final E task) {
        synchronized (this) {
            put(task, null);
        }
        return task;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Scheduler#close()
     */
    @Override
    public void close() {
        synchronized (this) {
            for (final AutoCloseable task : keySet()) {
                try {
                    task.close();
                } catch (final Throwable t) {
                    if (task instanceof Task<?>) {
                        errorHandler.onError(((Task<?>) task).task(), t);
                    } else {
                        errorHandler.onError(task, t);
                    }
                }
            }
            clear();
        }
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Scheduler#clockService()
     */
    @Override
    public ClockService clockService() {
        return clockService;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Scheduler#name()
     */
    @Override
    public String name() {
        return name;
    }
}
