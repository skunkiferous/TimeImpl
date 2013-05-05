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

import org.threeten.bp.Clock;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.Time;

/**
 * NanoClock is a system clock with nano precision.
 *
 * It delegates to the ClockService for the current time.
 *
 * @author monster
 */
public class NanoClock extends Clock {

    /** The Clock's time zone. */
    private final ZoneId zone;

    /** The ClockService */
    private final ClockService clockService;

    public NanoClock(final ZoneId theZone, final ClockService theClockService) {
        zone = Objects.requireNonNull(theZone, "theZone");
        clockService = Objects.requireNonNull(theClockService,
                "theClockService");
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(final ZoneId _zone) {
        if (_zone.equals(zone)) {
            return this;
        }
        return new NanoClock(_zone, clockService);
    }

    @Override
    public long millis() {
        return clockService.currentTimeNanos() / Time.MILLI_NS;
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochSecond(0, clockService.currentTimeNanos());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof NanoClock) {
            return zone.equals(((NanoClock) obj).zone);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return zone.hashCode() + 1;
    }

    @Override
    public String toString() {
        return "NanoClock[" + zone + "]";
    }

    public static void main(final String[] args) {
        // Warmup!
        Clock.systemUTC().instant();
        NanoClock.systemUTC().instant();
        System.out.println("Clock.systemUTC().instant(): "
                + Clock.systemUTC().instant());
        System.out.println("NanoClock.systemUTC().instant():   "
                + NanoClock.systemUTC().instant());
        System.out.println("Clock.systemDefaultZone().instant(): "
                + Clock.systemDefaultZone().instant());
        System.out.println("NanoClock.systemDefaultZone().instant():   "
                + NanoClock.systemDefaultZone().instant());
        System.out.println("NanoClock.systemDefaultZone():   "
                + NanoClock.systemDefaultZone());
        System.out.println("DateTime UTC: "
                + ZonedDateTime.now(NanoClock.systemUTC()));
        System.out.println("DateTime Local: "
                + ZonedDateTime.now(NanoClock.systemDefaultZone()));
        System.out.println("toEpochMilli(): "
                + new Date(NanoClock.systemUTC().instant().toEpochMilli()));
    }
}
