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
import java.util.TimeZone;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Clock;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Scheduler.Handler;

/**
 * ClockServiceImpl implements a ClockService.
 *
 * @author monster
 */
@Singleton
public class ClockServiceImpl implements ClockService {

    /** The UTC TimeZone. */
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    /** Logger */
    private static final Logger LOG = LoggerFactory
            .getLogger(ClockServiceImpl.class);

    /** The UTC clock instance. */
    private static final NanoClock UTC = new NanoClock(ZoneOffset.UTC);

    /** The local clock instance. */
    private static final NanoClock LOCAL = new NanoClock(
            ZoneId.of(CurrentTimeNanos.DEFAULT_LOCAL.getID()));

    /** Default error handler. */
    private static final Handler DEFAULT_HANDLER = new Handler() {
        @Override
        public void onError(final Runnable task, final Throwable error) {
            LOG.error("Error running task: " + task, error);
        }
    };

    /** Returns the start *UTC* time, in nanoseconds, when the service was created. */
    private final long startTimeNanos;

    /** Initialize a ClockService implementation, with the give parameters. */
    public ClockServiceImpl(final boolean useInternetTime,
            final boolean setTimezoneToUTC) {
        CurrentTimeNanos.setup(useInternetTime, setTimezoneToUTC, this);
        startTimeNanos = CurrentTimeNanos.currentTimeNanos();
    }

    /**
     * Initialize a ClockService implementation, to use Internet Time,
     * and UTC by default.
     */
    public ClockServiceImpl() {
        this(true, true);
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
        return CurrentTimeNanos.DEFAULT_LOCAL;
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

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#currentTimeNanos()
     */
    @Override
    public long currentTimeNanos() {
        return CurrentTimeNanos.currentTimeNanos();
    }

    /** Creates a new Scheduler, using the given Error Handler. */
    @Override
    public Scheduler createNewScheduler(final Handler errorHandler) {
        return new TimerSchedulerImpl(errorHandler == null ? DEFAULT_HANDLER
                : errorHandler);
    }

    public static void main(final String[] args) {
        final ClockServiceImpl impl = new ClockServiceImpl(true, false);
        com.blockwithme.time.Clock.setClockService(impl);

        System.out.println("System.currentTimeMillis(): "
                + System.currentTimeMillis());
        System.out.println("Clock.currentTimeMillis():  "
                + com.blockwithme.time.Clock.currentTimeMillis());

        System.out.println("clock():      "
                + ZonedDateTime.now(com.blockwithme.time.Clock.clock()));
        System.out.println("localClock(): "
                + ZonedDateTime.now(com.blockwithme.time.Clock.localClock()));

//        final SimpleDateFormat sdfLocal = new SimpleDateFormat(
//                "yyyy-MM-dd HH:mm:ss.SSS", Locale.GERMANY);
//        final SimpleDateFormat sdfUTC = new SimpleDateFormat(
//                "yyyy-MM-dd HH:mm:ss.SSS");
//        System.out.println("TZ " + com.blockwithme.time.Clock.localTimeZone());
//        System.out.println("LOCAL "
//                + sdfLocal.format(com.blockwithme.time.Clock.localCalendar()
//                        .getTime()));
//        System.out
//                .println("UTC   "
//                        + sdfUTC.format(com.blockwithme.time.Clock.calendar()
//                                .getTime()));
//        System.out.println("      " + new Date());
    }
}
