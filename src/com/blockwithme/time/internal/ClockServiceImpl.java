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
import java.util.concurrent.atomic.AtomicReference;

import org.threeten.bp.Clock;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.Scheduler;

/**
 * ClockServiceImpl implements a ClockService.
 *
 * @author monster
 */
public class ClockServiceImpl implements ClockService {

    /** The UTC clock instance. */
    private static final NanoClock UTC = new NanoClock(ZoneOffset.UTC);

    /** The local clock instance. */
    private static final NanoClock LOCAL = new NanoClock(
            ZoneId.of(CurrentTimeNanos.DEFAULT_LOCAL.getID()));

    /** The UTC TimeZone. */
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    /** Executor used for Runnable tasks. */
    private static final Scheduler.Executor<Runnable> RUNNABLE_EXECUTOR = new Scheduler.Executor<Runnable>() {
        @Override
        public void run(final Runnable task) {
            task.run();
        }
    };

    /** The default Runnable Scheduler, if any. */
    private static final AtomicReference<Scheduler<Runnable>> DEFAULT_SCHEDULER = new AtomicReference<>();

    /** Initialize a ClockService implementation, with the give parameters. */
    public ClockServiceImpl(final boolean useInternetTime,
            final boolean setTimezoneToUTC) {
        CurrentTimeNanos.setup(useInternetTime, setTimezoneToUTC, this);
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

    /** Creates a new Scheduler<T>, using the given executor. */
    @Override
    public <T> Scheduler<T> createNewScheduler(
            final Scheduler.Executor<T> executor) {
        return new TimerSchedulerImpl<>(executor);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#currentTimeNanos()
     */
    @Override
    public Scheduler<Runnable> getDefaultRunnableScheduler() {
        Scheduler<Runnable> result = DEFAULT_SCHEDULER.get();
        if (result == null) {
            result = new TimerSchedulerImpl<>(RUNNABLE_EXECUTOR);
            if (!DEFAULT_SCHEDULER.compareAndSet(null, result)) {
                // Wow! Someone beat us to it!
                result.cancel();
                result = DEFAULT_SCHEDULER.get();
            }
        }
        return result;
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
