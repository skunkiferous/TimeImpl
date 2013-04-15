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
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.threeten.bp.Clock;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;

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

    /** The defualt local TimeZone. */
//    private static final TimeZone LOCAL_TZ = CurrentTimeNanos.DEFAULT_LOCAL;

    /** The default local ZoneId. */
//    private static final ZoneId LOCAL_ZONE_ID = ZoneId.of(LOCAL_TZ.getID());

//
//    /** The default local ZoneOffset. */
//    private static final ZoneOffset LOCAL_ZONE_OFFSET = LOCAL_ZONE_ID.;

    /** Creates an Instant, using the UTC current time in nano-seconds. */
    public static Instant instant(final long utcTimeNanos) {
        final long epochSecond = utcTimeNanos / 1000000000L;
        final long nanoAdjustment = utcTimeNanos - epochSecond * 1000000000L;
        return Instant.ofEpochSecond(epochSecond, nanoAdjustment);
    }

    /** Initialize a ClockService implementation. */
    public ClockServiceImpl() {
        // NOP
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
     * @see com.blockwithme.time.ClockService#localCurrentTimeMillis()
     */
    @Override
    public long localCurrentTimeMillis() {
        return localCurrentTimeNanos() / 1000000L;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#localDate()
     */
    @Override
    public Date localDate() {
        return new Date(localCurrentTimeMillis());
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#localCalendar()
     */
    @Override
    public Calendar localCalendar() {
        final Calendar result = Calendar.getInstance(localTimeZone());
        result.setTimeInMillis(localCurrentTimeMillis());
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
     * @see com.blockwithme.time.ClockService#nanosToInstant(long)
     */
    @Override
    public Instant nanosToInstant(final long utcTimeNanos) {
        return instant(utcTimeNanos);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#currentTimeNanos()
     */
    @Override
    public long currentTimeNanos() {
        return CurrentTimeNanos.utcTimeNanos();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#localCurrentTimeNanos()
     */
    @Override
    public long localCurrentTimeNanos() {
        return CurrentTimeNanos.localTimeNanos();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#toUTC(java.util.Date)
     */
    @Override
    public Date toUTC(final Date localDate) {
        if (localDate == null) {
            return null;
        }
        return new Date(toUTCMillis(localDate.getTime()));
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#toLocal(java.util.Date)
     */
    @Override
    public Date toLocal(final Date utcDate) {
        if (utcDate == null) {
            return null;
        }
        return new Date(toLocalMillis(utcDate.getTime()));
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#toUTCMillis(long)
     */
    @Override
    public long toUTCMillis(final long localMillis) {
//        final Instant instant = Instant.ofEpochMilli(localMillis);
//        final ZonedDateTime before = ZonedDateTime.ofInstant(instant,
//                LOCAL_ZONE_ID);
//        final ZonedDateTime after = before.with(ZoneOffset.UTC);
//        return after.toInstant().toEpochMilli();

        // TODO This code process the DST change one hour too early for Germany,
        // But seems otherwise correct, so I'll stick to it for now.
        final Calendar cal = Calendar.getInstance(localTimeZone());
        cal.setTimeInMillis(localMillis);
        return localMillis - cal.get(Calendar.ZONE_OFFSET)
                - cal.get(Calendar.DST_OFFSET);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#toLocalMillis(long)
     */
    @Override
    public long toLocalMillis(final long utcMillis) {
//        final Instant instant = Instant.ofEpochMilli(utcMillis);
//        final ZonedDateTime before = ZonedDateTime.ofInstant(instant,
//                ZoneOffset.UTC);
//        final ZonedDateTime after = before.withZoneSameLocal(LOCAL_ZONE_ID);
//        return after.toInstant().toEpochMilli();

        // TODO This code process the DST change one hour too early for Germany,
        // But seems otherwise correct, so I'll stick to it for now.
        final Calendar cal = Calendar.getInstance(localTimeZone());
        cal.setTimeInMillis(utcMillis);
        return utcMillis + cal.get(Calendar.ZONE_OFFSET)
                + cal.get(Calendar.DST_OFFSET);
    }

    public static Calendar convertCalendar(final Calendar calendar,
            final TimeZone timeZone) {
        final Calendar ret = new GregorianCalendar(timeZone);
        ret.setTimeInMillis(calendar.getTimeInMillis()
                + timeZone.getOffset(calendar.getTimeInMillis())
                - TimeZone.getDefault().getOffset(calendar.getTimeInMillis()));
        ret.getTime();
        return ret;
    }

    @SuppressWarnings("deprecation")
    public static void main(final String[] args) {
        final ClockServiceImpl service = new ClockServiceImpl();
        final Calendar cal = Calendar
                .getInstance(CurrentTimeNanos.DEFAULT_LOCAL);
        cal.add(Calendar.DATE, -16);
        final Date start = cal.getTime();
        // Sunday, 31 March 2013        01:59:59
        final long oneHour = 3600000L;
        for (int i = 0; i < 15; i++) {
            final Date date = new Date(start.getTime() + i * oneHour);
            System.out.println(date.toGMTString() + "   toLocal: "
                    + service.toLocal(date) + "   toUTC: "
                    + service.toUTC(date).toGMTString());
        }
    }

    /** Creates a new Scheduler<T>, using the given executor. */
    @Override
    public <T> Scheduler<T> createNewScheduler(
            final Scheduler.Executor<T> executor) {
        return new TimerSchedulerImpl<>(executor);
    }
}
