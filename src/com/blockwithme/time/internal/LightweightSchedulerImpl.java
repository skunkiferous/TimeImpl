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
import com.blockwithme.time.CoreScheduler;
import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Task;

/**
 * Lightweight Scheduler implementation.
 *
 * @author monster
 */
public class LightweightSchedulerImpl extends WeakHashMap<Task, Object>
        implements Scheduler {

    /** NS im MN. */
    private static final long MS2NS = 1000000L;

    /** The error handler. */
    private final Handler errorHandler;

    /** The CoreScheduler */
    private final CoreScheduler coreScheduler;

    /** The ClockService */
    private final ClockService clockService;

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
     * Rounds a number of nano-seconds to a number of milli-seconds.
     * @param nanos nano-seconds
     * @return milli-seconds.
     */
    public static long roundToMS(final long nanos) {
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
    private Task scheduleImpl(final Runnable task, final Date timeUTC) {
        final long delayMS = timeUTC.getTime()
                - clockService.currentTimeMillis();
        return scheduleNS(task, delayMS * MS2NS);
    }

    /** @see schedule(TimerTask,java.util.Date,long) */
    private Task scheduleAtFixedPeriodImplNS(final Runnable task,
            final Date firstTimeUTC, final long periodNS) {
        final long delayMS = firstTimeUTC.getTime()
                - clockService.currentTimeMillis();
        return scheduleAtFixedPeriodNS(task, delayMS * MS2NS, periodNS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    private Task scheduleAtFixedRate2(final Runnable task,
            final Date firstTimeUTC, final long periodMS) {
        final long delayMS = firstTimeUTC.getTime()
                - clockService.currentTimeMillis();
        return scheduleAtFixedRateNS(task, delayMS * MS2NS, periodMS * MS2NS);
    }

    /**
     * Creates a LightweightSchedulerImpl with a CoreScheduler and an error handler.
     */
    protected LightweightSchedulerImpl(final CoreScheduler theCoreScheduler,
            final Handler theErrorHandler, final ClockService theClockService) {
        coreScheduler = Objects.requireNonNull(theCoreScheduler,
                "theCoreScheduler");
        errorHandler = Objects.requireNonNull(theErrorHandler,
                "theErrorHandler");
        clockService = Objects.requireNonNull(theClockService,
                "theClockService");
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final Task schedule(final Runnable task, final Date timeUTC) {
        return scheduleImpl(task, toUTCDate(timeUTC));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final Task schedule(final Runnable task, final Instant timeUTC) {
        return scheduleImpl(task, toUTCDate(timeUTC));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final Task schedule(final Runnable task, final ZonedDateTime dateTime) {
        return scheduleImpl(task, toUTCDate(dateTime));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final Task schedule(final Runnable task, final LocalDateTime dateTime) {
        return scheduleImpl(task, toUTCDate(dateTime));
    }

    /** @see schedule(TimerTask,java.util.Date) */
    @Override
    public final Task schedule(final Runnable task, final LocalTime time) {
        return scheduleImpl(task, toUTCDate(time));
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedPeriod(final Runnable task,
            final Date firstTimeUTC, final long periodMS) {
        return scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTimeUTC),
                periodMS * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedPeriod(final Runnable task,
            final Instant firstTimeUTC, final long periodMS) {
        return scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTimeUTC),
                periodMS * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedPeriod(final Runnable task,
            final ZonedDateTime firstTime, final long periodMS) {
        return scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodMS
                * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedPeriod(final Runnable task,
            final LocalDateTime firstTime, final long periodMS) {
        return scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodMS
                * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedPeriod(final Runnable task,
            final LocalTime firstTime, final long periodMS) {
        return scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodMS
                * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedPeriodNS(final Runnable task,
            final Date firstTimeUTC, final long periodNS) {
        return scheduleAtFixedPeriodImplNS(task, firstTimeUTC, periodNS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedPeriodNS(final Runnable task,
            final Instant firstTimeUTC, final long periodNS) {
        return scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTimeUTC),
                periodNS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedPeriodNS(final Runnable task,
            final ZonedDateTime firstTime, final long periodNS) {
        return scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodNS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedPeriodNS(final Runnable task,
            final LocalDateTime firstTime, final long periodNS) {
        return scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodNS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedPeriodNS(final Runnable task,
            final LocalTime firstTime, final long periodNS) {
        return scheduleAtFixedPeriodImplNS(task, toUTCDate(firstTime), periodNS);
    }

    /** @see schedule(TimerTask,long) */
    @Override
    public final Task schedule(final Runnable task, final long delayMS) {
        return scheduleNS(task, delayMS * MS2NS);
    }

    /** @see scheduleAtFixedPeriod(TimerTask,long,long) */
    @Override
    public final Task scheduleAtFixedPeriod(final Runnable task,
            final long delayMS, final long periodMS) {
        return scheduleAtFixedPeriodNS(task, delayMS * MS2NS, periodMS * MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedRate(final Runnable task,
            final Date firstTimeUTC, final long periodMS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedRate(final Runnable task,
            final Instant firstTimeUTC, final long periodMS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedRate(final Runnable task,
            final ZonedDateTime firstTime, final long periodMS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedRate(final Runnable task,
            final LocalDateTime firstTime, final long periodMS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedRate(final Runnable task,
            final LocalTime firstTime, final long periodMS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodMS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedRateNS(final Runnable task,
            final Date firstTimeUTC, final long periodNS) {
        return scheduleAtFixedRate2(task, firstTimeUTC, periodNS / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedRateNS(final Runnable task,
            final Instant firstTimeUTC, final long periodNS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTimeUTC), periodNS
                / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedRateNS(final Runnable task,
            final ZonedDateTime firstTime, final long periodNS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodNS
                / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedRateNS(final Runnable task,
            final LocalDateTime firstTime, final long periodNS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodNS
                / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,java.util.Date,long) */
    @Override
    public final Task scheduleAtFixedRateNS(final Runnable task,
            final LocalTime firstTime, final long periodNS) {
        return scheduleAtFixedRate2(task, toUTCDate(firstTime), periodNS
                / MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,long,long) */
    @Override
    public final Task scheduleAtFixedRate(final Runnable task,
            final long delayMS, final long periodMS) {
        return scheduleAtFixedRateNS(task, delayMS * MS2NS, periodMS * MS2NS);
    }

    /** @see scheduleAtFixedRate(TimerTask,long,long) */
    @Override
    public final Task scheduleAtFixedRateNS(final Runnable task,
            final long delayNS, final long periodNS) {
        return queue(coreScheduler.scheduleAtFixedRateNS(task, errorHandler,
                delayNS, periodNS));
    }

    /** @see scheduleAtFixedPeriod(TimerTask,long,long) */
    @Override
    public final Task scheduleAtFixedPeriodNS(final Runnable task,
            final long delayNS, final long periodNS) {
        return queue(coreScheduler.scheduleAtFixedPeriodNS(task, errorHandler,
                delayNS, periodNS));
    }

    /** @see schedule(TimerTask,long) */
    @Override
    public final Task scheduleNS(final Runnable task, final long delayNS) {
        return queue(coreScheduler.scheduleNS(task, errorHandler, delayNS));
    }

    /** Enqueues a task, so that they can be cancelled later. */
    private Task queue(final Task task) {
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
            for (final Task task : keySet()) {
                try {
                    task.close();
                } catch (final Throwable t) {
                    errorHandler.onError(task.task(), t);
                }
            }
            clear();
        }
    }
}
