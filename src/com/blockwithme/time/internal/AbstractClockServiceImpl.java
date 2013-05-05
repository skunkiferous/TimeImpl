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
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Clock;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.CoreScheduler;
import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Scheduler.Handler;
import com.blockwithme.time.Time;
import com.blockwithme.time.Timeline;

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

    /** Minimum number of microseconds required to call sleep. */
    private static final long SLEEP_THRESHOLD = 2 * Time.MILLI_MUS;

    /** Default error handler. */
    private static final Handler DEFAULT_HANDLER = new Handler() {
        @Override
        public void onError(final Object task, final Throwable error) {
            LOG.error("Error running task: " + task, error);
        }
    };

    /** The UTC clock instance. */
    private final MicroClock UTC;

    /** The local clock instance. */
    private final MicroClock LOCAL;

    /** The local TimeZone. */
    private final TimeZone localTimeZone;

    /** The CoreScheduler */
    private final CoreScheduler coreScheduler;

    /** The number of clock ticks per second. */
    private final int ticksPerSecond;

    private final AtomicReference<CoreTimeline> coreTimeline = new AtomicReference<>();

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
        final long durationMUS = (after - before) / 1000L;
        return durationMUS / 100;
    }

    /**
     * Sleeps (approximately) for the given amount of microseconds.
     * The precision should be much better then Thread.sleep(), but we do
     * a busy-wait using yield in the last 2 milliseconds, which
     * consumes more CPU then a normal sleep.
     *
     * @throws InterruptedException
     */
    @Override
    public void sleepMicros(final long sleepMicros) throws InterruptedException {
        sleepMicrosStatic(sleepMicros);
    }

    /**
     * Sleeps (approximately) for the given amount of microseconds.
     * The precision should be much better then Thread.sleep(), but we do
     * a busy-wait using yield in the last 2 milliseconds, which
     * consumes more CPU then a normal sleep.
     *
     * @throws InterruptedException
     */
    public static void sleepMicrosStatic(final long sleepMicros)
            throws InterruptedException {
        long timeLeft = sleepMicros * 1000L;
        final long end = System.nanoTime() + timeLeft;
        while (timeLeft / 2 >= SLEEP_THRESHOLD) {
            Thread.sleep(timeLeft / (2 * Time.MILLI_MUS * 1000L));
            timeLeft = end - System.nanoTime();
        }
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
            final CoreScheduler theCoreScheduler) {
        localTimeZone = Objects.requireNonNull(theLocalTimeZone);
        coreScheduler = Objects.requireNonNull(theCoreScheduler);
        UTC = new MicroClock(ZoneOffset.UTC, this);
        LOCAL = new MicroClock(ZoneId.of(localTimeZone.getID()), this);
        ticksPerSecond = theCoreScheduler.ticksPerSecond();
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
        return currentTimeMicros() / Time.MILLI_MUS;
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
     * @see com.blockwithme.time.ClockService#tickDurationMicros()
     */
    @Override
    public long tickDurationMicros() {
        return Time.SECOND_MUS / ticksPerSecond;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#ticksPerSecond()
     */
    @Override
    public int ticksPerSecond() {
        return ticksPerSecond;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#coreTimeline()
     */
    @Override
    public Timeline coreTimeline() {
        CoreTimeline result = coreTimeline.get();
        while (result == null) {
            final CoreTimeline ct = new CoreTimeline(this, coreScheduler);
            if (coreTimeline.compareAndSet(null, ct)) {
                ct.unpause();
                LOG.info("Core Timeline created: " + ct);
                result = ct;
            } else {
                // Damn, multiple threads tried at the same time.
                try {
                    ct.close();
                } catch (final Exception e) {
                    LOG.error("Error closing temporary core timeline", e);
                }
                result = coreTimeline.get();
            }
        }
        return result;
    }
}
