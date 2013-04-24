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
import com.blockwithme.time.LogicalScheduler;
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

    /** The UTC clock instance. */
    private final NanoClock UTC;

    /** The local clock instance. */
    private final NanoClock LOCAL;

    /** The local TimeZone. */
    private final TimeZone localTimeZone;

    /** Default error handler. */
    private static final Handler DEFAULT_HANDLER = new Handler() {
        @Override
        public void onError(final Runnable task, final Throwable error) {
            LOG.error("Error running task: " + task, error);
        }
    };

    /** The CoreScheduler */
    private final CoreScheduler coreScheduler;

    /** Returns the start *UTC* time, in nanoseconds, when the service was created. */
    private long startTimeNanos;

    /** Sets the startTimeNanos. Must be called exactly one from sub-class constructor. */
    protected final void setStartTimeNanos(final long theStartTimeNanos) {
        startTimeNanos = theStartTimeNanos;
    }

    /** Initialize a ClockService implementation, with the give parameters. */
    protected AbstractClockServiceImpl(final TimeZone theLocalTimeZone,
            final CoreScheduler theCoreScheduler) {
        localTimeZone = Objects.requireNonNull(theLocalTimeZone);
        coreScheduler = Objects.requireNonNull(theCoreScheduler);
        UTC = new NanoClock(ZoneOffset.UTC, this);
        LOCAL = new NanoClock(ZoneId.of(localTimeZone.getID()), this);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#currentTimeMillis()
     */
    @Override
    public long currentTimeMillis() {
        return currentTimeNanos() / 1000000L;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#startTimeNanos()
     */
    @Override
    public long startTimeNanos() {
        return startTimeNanos;
    }

    @Override
    public long elapsedTimeNanos() {
        return currentTimeNanos() - startTimeNanos;
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
    public Scheduler newScheduler(final Handler errorHandler) {
        return new LightweightSchedulerImpl(coreScheduler,
                errorHandler == null ? DEFAULT_HANDLER : errorHandler, this);
    }

    @Override
    public LogicalScheduler newLogicalScheduler(final Handler errorHandler,
            final long cycleDuration, final boolean fixedRate) {
        return new LightweightLogicalSchedulerImpl(coreScheduler,
                errorHandler == null ? DEFAULT_HANDLER : errorHandler, this,
                cycleDuration, fixedRate);
    }

    /* (non-Javadoc)
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        if (coreScheduler != null) {
            coreScheduler.close();
        }
    }
}
