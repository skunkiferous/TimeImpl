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
import org.threeten.bp.jdk8.Jdk8Methods;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.Time;

/**
 * MicroClock is a system clock with microsecond precision.
 *
 * It delegates to the ClockService for the current time.
 *
 * @author monster
 */
public class MicroClock extends Clock {

    /** The Clock's time zone. */
    private final ZoneId zone;

    /** The ClockService */
    private final ClockService clockService;

    public MicroClock(final ZoneId theZone, final ClockService theClockService) {
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
        return new MicroClock(_zone, clockService);
    }

    @Override
    public long millis() {
        return clockService.currentTimeMicros() / Time.MILLI_MUS;
    }

    @Override
    public Instant instant() {
        final long nowMUS = clockService.currentTimeMicros();
        final long secs = Jdk8Methods.floorDiv(nowMUS, Time.SECOND_MUS);
        final long nos = Jdk8Methods.floorMod(nowMUS, Time.SECOND_MUS) * 1000L;
        return Instant.ofEpochSecond(secs, nos);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MicroClock) {
            return zone.equals(((MicroClock) obj).zone);
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
        MicroClock.systemUTC().instant();
        System.out.println("Clock.systemUTC().instant(): "
                + Clock.systemUTC().instant());
        System.out.println("NanoClock.systemUTC().instant():   "
                + MicroClock.systemUTC().instant());
        System.out.println("Clock.systemDefaultZone().instant(): "
                + Clock.systemDefaultZone().instant());
        System.out.println("NanoClock.systemDefaultZone().instant():   "
                + MicroClock.systemDefaultZone().instant());
        System.out.println("NanoClock.systemDefaultZone():   "
                + MicroClock.systemDefaultZone());
        System.out.println("DateTime UTC: "
                + ZonedDateTime.now(MicroClock.systemUTC()));
        System.out.println("DateTime Local: "
                + ZonedDateTime.now(MicroClock.systemDefaultZone()));
        System.out.println("toEpochMilli(): "
                + new Date(MicroClock.systemUTC().instant().toEpochMilli()));
    }
}
