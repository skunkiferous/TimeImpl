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
package com.blockwithme.time;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

/**
 * @author monster
 *
 */
public class Test {
    private static final int DAYS_OFFSET_TO_BE_BEFORE_DST_CHANGE = -16;

    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    private static final TimeZone LOCAL_TZ = TimeZone.getDefault();

    private static final ZoneId UTC_ID = ZoneId.of(UTC_TZ.getID());

    private static final ZoneId LOCAL_ZONE_ID = ZoneId.of(LOCAL_TZ.getID());

    public static long UTCtoLocalMillis(final long utcMillis) {
        final Instant instant = Instant.ofEpochMilli(utcMillis);
        final ZonedDateTime before = ZonedDateTime.ofInstant(instant, UTC_ID);
        final ZonedDateTime after = before.withZoneSameLocal(LOCAL_ZONE_ID);
        return after.toInstant().toEpochMilli();

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

    public static void main(final String[] args) {
        TimeZone.setDefault(UTC_TZ);
        System.out.println("LOCAL_TZ: " + LOCAL_TZ.getDisplayName());
        final Calendar cal = Calendar.getInstance(LOCAL_TZ);
        cal.add(Calendar.DATE, DAYS_OFFSET_TO_BE_BEFORE_DST_CHANGE);
        final Date start = cal.getTime();
        // DST Change: Sunday, 31 March 2013 01:59:59 (local time)
        final long oneHour = 3600000L;
        for (int i = 0; i < 12; i++) {
            final Date date = new Date(start.getTime() + i * oneHour);
            System.out.println("UTC: " + date + "   toLocal: "
                    + new Date(UTCtoLocalMillis(date.getTime())));
        }
    }
}
