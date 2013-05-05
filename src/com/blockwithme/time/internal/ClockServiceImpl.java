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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.ZonedDateTime;

import com.blockwithme.time.CS;
import com.blockwithme.time.ClockSynchronizer;
import com.blockwithme.time.CoreScheduler;
import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Time;

/**
 * ClockServiceImpl is an implementation of ClockService.
 *
 * Helper class, returns the *current (UTC and local) time* at nano precision
 * (but the nanos are estimated).
 *
 * Using ClockSynchronizer, this class could try to contact some public NTP
 * time servers, so that it can return a value as close as possible to the real
 * time, even if the local clock is skewed. If this fails, we could try to get
 * the time using webservers.
 *
 * Beware, that for *everything* to work correctly, we also have to change the
 * default time zone to UTC. So if you use other APIs to manipulate the time
 * except this class and NanoClock, you might get strange results.
 *
 * I think this code is now immune to DST problems, but this must still be tested.
 *
 * @author monster
 */
@Singleton
public class ClockServiceImpl extends AbstractClockServiceImpl {

    /** Logger. */
    private static final Logger LOG = LoggerFactory
            .getLogger(ClockServiceImpl.class);

    /** Default number of clock ticks per second. */
    public static final int TICKS_PER_SECONDS = 60;

    /** Number of loops to do when comparing System.currentTimeMillis() and System.nanoTime(). */
    private static final int LOOPS = 20;

    /** The original local time zone. */
    private static final TimeZone DEFAULT_LOCAL = TimeZone.getDefault();

    /** Comparator of ClockSynchronizer */
    private static final Comparator<ClockSynchronizer> CS_CMP = new Comparator<ClockSynchronizer>() {
        @Override
        public int compare(final ClockSynchronizer o1,
                final ClockSynchronizer o2) {
            final long ep1 = o1.expectedPrecision();
            final long ep2 = o2.expectedPrecision();
            return Long.compare(ep1, ep2);
        }
    };

    /** The current TimeData. */
    private final AtomicReference<TimeData> timeData = new AtomicReference<>();

    /** UTC Time in nanoseconds, at last call. */
    private final AtomicLong lastNanoTime = new AtomicLong();

    /** Use Internet Time synchronization? */
    private final boolean useInternetTime;

    /** Use Internet Time synchronization? */
    private final Scheduler scheduler;

    /** The ClockSynchronizers */
    private final ClockSynchronizer[] clockSynchronizers;

    /** Creates a new TimeData instance. */
    private TimeData newTimeData(final TimeData prev) {
        final long nanoA = System.nanoTime();
        final Long offset = getNanoTimeToUTCTimeOffsetInNS();
        if (offset == null) {
            return null;
        }
        final long nanoB = System.nanoTime();
        final long duration1 = (nanoB - nanoA);
        final long timeInTheMiddle = nanoA + (duration1 / 2L);
        return new TimeData(offset, timeInTheMiddle, prev);
    }

    /**
     * Computes the difference between the local system clock, and the Internet
     * servers clock, in milliseconds. It will first try to get the offset
     * using the better NTP protocol, and if it fails, most likely because we
     * are behind a firewall, it will try to contact websites, to get an
     * approximation. It returns null on failure.
     *
     * The offset is on System.currentTimeMillis().
     *
     * Note that this methods will probably requires multiple seconds to terminate.
     *
     * @return An offset, in milliseconds, from the local time to the correct UTC/GMT time, or null on failure.
     */
    private Long getLocalToUTCTimeOffsetInMS() {
        for (final ClockSynchronizer cs : clockSynchronizers) {
            try {
                return cs.getLocalToUTCTimeOffset();
            } catch (final Throwable t) {
                LOG.error("Error calling " + cs, t);
            }
        }
        return null;
    }

    /**
     * Computes the difference between the System.nanoTime(), and the UTC/GMT time, in nanoseconds.
     *
     * @see getLocalToUTCTimeOffsetInMS()
     *
     * @return An offset, in nanoseconds, from the System.nanoTime() to the UTC/GMT time, or null on failure.
     */
    private Long getNanoTimeToUTCTimeOffsetInNS() {
        Long result = getLocalToUTCTimeOffsetInMS();
        if (result != null) {
            // OK, now find out, how to convert System.nanoTime() to
            // System.currentTimeMillis()
            result += getNanoTimeToCurrentTimeNanosOffsetInNS(result);
        }
        return result;
    }

    /** Computes the difference between System.currentTimeMillis() and System.nanoTime(). */
    private static long getNanoTimeToCurrentTimeNanosOffsetInNS(
            final long locatToUTC) {
        long sumOffsetInMS = 0;
        for (int i = 0; i < LOOPS; i++) {
            long bestDurationNS = Long.MAX_VALUE;
            long bestOffsetInMS = Long.MAX_VALUE;
            int loops = 0;
            while (loops < LOOPS) {
                final long startMS = System.currentTimeMillis() + locatToUTC;
                final long startNS = System.nanoTime();
                long nextMS = System.currentTimeMillis() + locatToUTC;
                long nextNS = System.nanoTime();
                long prevNS = startNS;
                while (startMS == nextMS) {
                    prevNS = nextNS;
                    nextMS = System.currentTimeMillis() + locatToUTC;
                    nextNS = System.nanoTime();
                }
                final long durationNS = (nextNS - prevNS);
                if (durationNS < bestDurationNS) {
                    bestOffsetInMS = nextMS - (nextNS / Time.MILLI_NS);
                    bestDurationNS = durationNS;
                    if (durationNS < 100) {
                        break;
                    }
                }
                loops++;
            }
            sumOffsetInMS += bestOffsetInMS;
        }
        // Times NS to MS conversion factor, divided by number of loops ...
        return sumOffsetInMS * (Time.MILLI_NS / LOOPS);
    }

//
//    /**
//     * Creates a default ClockServiceImpl.
//     */
//    public ClockServiceImpl() {
//        this(true, true, new TimerCoreScheduler(), new NTPClockSynchronizer(),
//                new HTTPClockSynchronizer());
//    }

    /**
     * Creates a ClockServiceImpl.
     *
     * @param setTimezoneToUTC
     * @param theCoreScheduler
     * @param theClockSynchronizers
     */
    @Inject
    public ClockServiceImpl(
            @Named("setTimezoneToUTC") final boolean setTimezoneToUTC,
            final CoreScheduler theCoreScheduler,
            final Set<ClockSynchronizer> theClockSynchronizers) {
        this(setTimezoneToUTC, theCoreScheduler, theClockSynchronizers
                .toArray(new ClockSynchronizer[theClockSynchronizers.size()]));
    }

    /**
     * Creates a ClockServiceImpl.
     *
     * @param useInternetTime
     * @param setTimezoneToUTC
     * @param theStartTimeNanos
     */
    public ClockServiceImpl(final boolean setTimezoneToUTC,
            final CoreScheduler theCoreScheduler,
            final ClockSynchronizer... theClockSynchronizers) {
        super(DEFAULT_LOCAL, theCoreScheduler);
        if (setTimezoneToUTC) {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        }
        final ClockSynchronizer[] cs = Objects.requireNonNull(
                theClockSynchronizers, "theClockSynchronizers");
        clockSynchronizers = cs.clone();
        Arrays.sort(clockSynchronizers, CS_CMP);
        useInternetTime = (clockSynchronizers.length > 0);

        if (useInternetTime) {
            final TimeData first = newTimeData(null);
            if (first != null) {
                timeData.set(first);
                lastNanoTime.set(first.utcNanos());
            } else {
                LOG.error("Failed to get Internet time!");
            }
            scheduler = newScheduler("Internet Time Synchronizer", null);
            scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    final TimeData refresh = newTimeData(timeData.get());
                    if (refresh != null) {
                        timeData.set(refresh);
                    } else {
                        LOG.error("Failed to get Internet time!");
                    }
                }
            }, Time.DAY_MS, Time.DAY_MS);
        } else {
            scheduler = null;
        }
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#currentTimeNanos()
     */
    @Override
    public long currentTimeNanos() {
        if (useInternetTime) {
            final TimeData td = timeData.get();
            if (td == null) {
                throw new IllegalStateException("No time data available!");
            }
            while (true) {
                final long last = lastNanoTime.get();
                final long now = td.utcNanos();
                if (now >= last) {
                    if (lastNanoTime.compareAndSet(last, now)) {
                        return now;
                    }
                } else {
                    // Ouch! utcTimeNanos() went backward!
                    final long diff = now - last;
                    if (diff < -Time.MILLI_NS) {
                        LOG.error("Time went backward by " + diff
                                + " nano-seconds.");
                    }
                    return last;
                }
            }
        }
        // Do NOT use Internet Time ...
        return System.currentTimeMillis() * Time.MILLI_NS;
    }

    /* (non-Javadoc)
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        if (scheduler != null) {
            scheduler.close();
        }
        super.close();
    }

    public static void main(final String[] args) {
        final ClockServiceImpl impl = new ClockServiceImpl(false,
                new TimerCoreScheduler(TICKS_PER_SECONDS),
                new NTPClockSynchronizer(), new HTTPClockSynchronizer());
        CS.setClockService(impl);

        System.out.println("System.currentTimeMillis(): "
                + System.currentTimeMillis());
        System.out.println("Clock.currentTimeMillis():  "
                + CS.currentTimeMillis());

        System.out.println("clock():      " + ZonedDateTime.now(CS.clock()));
        System.out.println("localClock(): "
                + ZonedDateTime.now(CS.localClock()));

        final SimpleDateFormat sdfLocal = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS", Locale.GERMANY);
        sdfLocal.setTimeZone(CS.localTimeZone());
        final SimpleDateFormat sdfUTC = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS");
        sdfUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        System.out.println("TZ " + CS.localTimeZone().getID());
        System.out.println("LOCAL "
                + sdfLocal.format(CS.localCalendar().getTime()));
        System.out.println("UTC   " + sdfUTC.format(CS.calendar().getTime()));
        System.out.println("      " + new Date());
    }
}
