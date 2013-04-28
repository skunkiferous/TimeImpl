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
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Clock;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.CoreScheduler;
import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Scheduler.Handler;

/**
 * AbstractClockServiceImpl serves as a base class to implements a ClockService.
 *
 * @author monster
 */
public abstract class AbstractClockServiceImpl implements ClockService {

    /** The UTC TimeZone. */
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    /** Logger */
    private static final Logger LOG = LoggerFactory
            .getLogger(AbstractClockServiceImpl.class);

    /** The average duration of the Thread.yield() method. */
    private static final long YIELD_DURATION = computeYieldDuration();

    /** Number of nanoseconds in one millisecond. */
    private static final long MS_AS_NANOS = 1000000;

    /** Minimum number of nanoseconds required to call sleep. */
    private static final long SLEEP_THRESHOLD = 2 * MS_AS_NANOS;

    /** Default error handler. */
    private static final Handler DEFAULT_HANDLER = new Handler() {
        @Override
        public void onError(final Runnable task, final Throwable error) {
            LOG.error("Error running task: " + task, error);
        }
    };

    /** The UTC clock instance. */
    private final NanoClock UTC;

    /** The local clock instance. */
    private final NanoClock LOCAL;

    /** The local TimeZone. */
    private final TimeZone localTimeZone;

    /** The CoreScheduler */
    private final CoreScheduler coreScheduler;

    /** The number of clock ticks per second. */
    private final int ticksPerSecond;

    /** Call Thread.yield() 100 times. */
    private static void yield100Times() {
        for (int i = 0; i < 100; i++) {
            Thread.yield();
        }
    }

    /** Computes the overhead of the Thread.yield() method. */
    private static long computeYieldDuration() {
        // Warmup ...
        yield100Times();
        final long before = System.nanoTime();
        yield100Times();
        final long after = System.nanoTime();
        final long duration = after - before;
        return duration / 100;
    }

    /**
     * Sleeps (approximately) for the given amount of nanoseconds.
     * The precision should be much better then Thread.sleep(), but we do
     * a busy-wait using yield in the last 2 milliseconds, which
     * consumes more CPU then a normal sleep.
     *
     * @throws InterruptedException
     */
    @Override
    public void sleepNanos(final long sleepNanos) throws InterruptedException {
        sleepNanosStatic(sleepNanos);
    }

    /**
     * Sleeps (approximately) for the given amount of nanoseconds.
     * The precision should be much better then Thread.sleep(), but we do
     * a busy-wait using yield in the last 2 milliseconds, which
     * consumes more CPU then a normal sleep.
     *
     * @throws InterruptedException
     */
    public static void sleepNanosStatic(final long sleepNanos)
            throws InterruptedException {
        long timeLeft = sleepNanos;
        final long end = System.nanoTime() + timeLeft;
        while (timeLeft >= SLEEP_THRESHOLD) {
            Thread.sleep(1);
            timeLeft = end - System.nanoTime();
        }
        while (timeLeft >= YIELD_DURATION) {
            Thread.yield();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            timeLeft = end - System.nanoTime();
        }
    }

    /** Initialize a ClockService implementation, with the give parameters. */
    protected AbstractClockServiceImpl(final TimeZone theLocalTimeZone,
            final CoreScheduler theCoreScheduler, final int theTicksPerSecond) {
        localTimeZone = Objects.requireNonNull(theLocalTimeZone);
        coreScheduler = Objects.requireNonNull(theCoreScheduler);
        UTC = new NanoClock(ZoneOffset.UTC, this);
        LOCAL = new NanoClock(ZoneId.of(localTimeZone.getID()), this);
        ticksPerSecond = theTicksPerSecond;
        coreScheduler.setClockService(this);
    }

    /** toString() */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#currentTimeMillis()
     */
    @Override
    public long currentTimeMillis() {
        return currentTimeNanos() / 1000000L;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#date()
     */
    @Override
    public Date date() {
        return new Date(currentTimeMillis());
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#calendar()
     */
    @Override
    public Calendar calendar() {
        final Calendar result = Calendar.getInstance(UTC_TZ);
        result.setTimeInMillis(currentTimeMillis());
        return result;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#clock()
     */
    @Override
    public Clock clock() {
        return UTC;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#localTimeZone()
     */
    @Override
    public TimeZone localTimeZone() {
        return localTimeZone;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#localCalendar()
     */
    @Override
    public Calendar localCalendar() {
        final Calendar result = Calendar.getInstance(localTimeZone());
        result.setTimeInMillis(currentTimeMillis());
        return result;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#localClock()
     */
    @Override
    public Clock localClock() {
        return LOCAL;
    }

    /** Creates a new Scheduler, using the given Error Handler. */
    @Override
    public Scheduler newScheduler(final String theName,
            final Handler errorHandler) {
        return new LightweightSchedulerImpl(coreScheduler,
                errorHandler == null ? DEFAULT_HANDLER : errorHandler, this,
                theName);
    }

    /* (non-Javadoc)
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        if (coreScheduler != null) {
            coreScheduler.close();
        }
        LOG.info(this + " closed.");
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#tickDurationNanos()
     */
    @Override
    public long tickDurationNanos() {
        return 1000000000L / ticksPerSecond;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#ticksPerSecond()
     */
    @Override
    public int ticksPerSecond() {
        return ticksPerSecond;
    }
}
