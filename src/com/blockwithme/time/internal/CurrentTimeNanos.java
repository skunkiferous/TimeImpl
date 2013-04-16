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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import com.blockwithme.time.ClockService;

/**
 * Helper class, returns the *current (UTC and local) time* at nano precision
 * (but the nanos are estimated).
 *
 * This class tries to contact some public NTP time servers, so that it can
 * return a value as close as possible to the real time, even if the local
 * clock is skewed. If this fails, we try to get the time using webservers.
 *
 * Beware, that for everything to work correctly, we have to change the default
 * time zone to UTC. So if you use other APIs to manipulate the time except
 * this class and NanoClock, you might get strange results.
 *
 * I think this code is now immune to DST problems, but this must still be tested.
 *
 * @author monster
 */
public class CurrentTimeNanos {

    /** Contains all the data we need to compute the current UTC time, using System.nanoTime(). */
    private static final class TimeData {
        private final long nanoTimeQuery;
        private final long nanoTimeToUTCTimeOffsetInNS;

        // Last query, not previous of last (prevNanoTimeQuery)
        private final long t0;
        // *Next* query, which should not have happened yet!
        // nanoTimeQuery + (nanoTimeQuery - prevNanoTimeQuery)
        private final long t1;
        // Previous of last offset (nanoTimeToUTCTimeOffsetInNS)
        private final long o0;
        // Last offset (prev.nanoTimeToUTCTimeOffsetInNS)
        private final long o1;

        private final long o1_minus_o0;
        private final long t1_minus_t0;
        private final double o1_minus_o0_div_t1_minus_t0;
        /** Did we have a prev at all? */
        private final boolean prev;

        public TimeData(final long nanoTimeToUTCTimeOffsetInNS,
                final long nanoTimeQuery, final TimeData prev) {
            this.nanoTimeToUTCTimeOffsetInNS = nanoTimeToUTCTimeOffsetInNS;
            this.nanoTimeQuery = nanoTimeQuery;
            if (prev != null) {
                final long prevNanoTimeQuery = prev.nanoTimeQuery;
                final long prevNanoTimeToUTCTimeOffsetInNS = prev.nanoTimeToUTCTimeOffsetInNS;
                final long offsetDiff = nanoTimeToUTCTimeOffsetInNS
                        - prevNanoTimeToUTCTimeOffsetInNS;
                final long nanoDiff = nanoTimeQuery - prevNanoTimeQuery;
                if ((offsetDiff != 0) && (nanoDiff != 0)) {
                    this.prev = true;

                    // Time to do some good old linear interpolation ...
                    // Note that we don't know the future offset, so we do as if the
                    // previous offset was the *next* offset, and the one before was the previous.

                    // Last query, not previous of last
                    t0 = nanoTimeQuery;
                    // *Next* query, which should not have happened yet!
                    t1 = nanoTimeQuery + (nanoTimeQuery - prevNanoTimeQuery);
                    // Previous of last offset
                    o0 = prevNanoTimeToUTCTimeOffsetInNS;
                    // Last offset
                    o1 = nanoTimeToUTCTimeOffsetInNS;

                    o1_minus_o0 = o1 - o0;
                    t1_minus_t0 = t1 - t0;
                    o1_minus_o0_div_t1_minus_t0 = ((double) o1_minus_o0)
                            / t1_minus_t0;
                } else {
                    this.prev = false;
                    o0 = o1 = t0 = t1 = o1_minus_o0 = t1_minus_t0 = 0;
                    o1_minus_o0_div_t1_minus_t0 = 0.0;
                }
            } else {
                this.prev = false;
                o0 = o1 = t0 = t1 = o1_minus_o0 = t1_minus_t0 = 0;
                o1_minus_o0_div_t1_minus_t0 = 0.0;
            }
        }

        /** toString() */
        @Override
        public String toString() {
            return "TimeData(nanoTimeQuery=" + nanoTimeQuery
                    + ", nanoTimeToUTCTimeOffsetInNS="
                    + nanoTimeToUTCTimeOffsetInNS + ", t0=" + t0 + ", t1=" + t1
                    + ", o0=" + o0 + ", o1=" + o1 + ", o1_minus_o0="
                    + o1_minus_o0 + ", t1_minus_t0=" + t1_minus_t0
                    + ", o1_minus_o0_div_t1_minus_t0="
                    + o1_minus_o0_div_t1_minus_t0 + ")";
        }

        /** Computes the current time UTC in nanoseconds. */
        public long utcNanos() {
            final long nanos = System.nanoTime();
            if (!prev) {
                return nanos + nanoTimeToUTCTimeOffsetInNS;
            }
            // Actual time
            final long t = nanos;
            // interpolated offset
//            final long o = o0 + o1_minus_o0 * (t - t0) / t1_minus_t0;
            final long o = o0 + (long) (o1_minus_o0_div_t1_minus_t0 * (t - t0));
            // "Actual time" + "interpolated offset" = UTC nano time
            return nanos + o;
        }
    }

    /** Used by getLocalToUTCTimeDiffMSOverHTTP() */
    private static final class TimeDuration implements Comparable<TimeDuration> {
        public final long duration;
        public final long diff;

        public TimeDuration(final long duration, final long diff) {
            this.duration = duration;
            this.diff = diff;
        }

        @Override
        public int compareTo(final TimeDuration other) {
            return Long.compare(duration, other.duration);
        }
    }

    /** The NTP time server pool. */
    private static final String[] NTP_POOL = new String[] { "0.pool.ntp.org",
            "1.pool.ntp.org", "2.pool.ntp.org" };

    /** List of possible websites to query, when NTP does not work. */
    private static final String[] WEBSITES = new String[] {
            "http://google.com", "http://wikipedia.org", "http://amazon.com",
            "http://facebook.com", "http://bing.com", "http://youtube.com" };

    /** Number of loops to do when comparing System.currentTimeMillis() and System.nanoTime(). */
    private static final int LOOPS = 20;

    /** The current TimeData. */
    private static final AtomicReference<TimeData> TIME_DATA = new AtomicReference<>();

    /** UTC Time in nanoseconds, at last call. */
    private static final AtomicLong LAST_NANO_TIME = new AtomicLong();

    /** The original local time zone. */
    public static final TimeZone DEFAULT_LOCAL = TimeZone.getDefault();

    /** Use Internet Time synchronization? */
    private static volatile boolean useInternetTime;

    /** Was setup called? */
    private static final AtomicBoolean SETUP_WAS_CALLED = new AtomicBoolean();

    /** One Day in milliseconds. */
    private static final long ONE_DAY_MS = 24 * 3600 * 1000L;

    /** Initializes the Internet Time usage. */
    public static void setup(final boolean useInternetTime,
            final boolean setTimezoneToUTC, final ClockService clockService) {
        if (SETUP_WAS_CALLED.compareAndSet(false, true)) {
            if (setTimezoneToUTC) {
                TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            }
            if (useInternetTime) {
                CurrentTimeNanos.useInternetTime = useInternetTime;
                final TimeData first = newTimeData(null);
                if (first != null) {
                    TIME_DATA.set(first);
                    LAST_NANO_TIME.set(first.utcNanos());
                } else {
                    System.err.println("Failed to get Internet time!");
                }
                clockService.getDefaultRunnableScheduler().schedule(
                        new Runnable() {
                            @Override
                            public void run() {
                                final TimeData refresh = newTimeData(TIME_DATA.get());
                                if (refresh != null) {
                                    TIME_DATA.set(refresh);
                                } else {
                                    System.err
                                            .println("Failed to get Internet time!");
                                }
                            }
                        }, ONE_DAY_MS, ONE_DAY_MS);
            }
        } else {
            throw new IllegalStateException("setup was already called!");
        }
    }

    /** Creates a new TimeData instnace. */
    private static TimeData newTimeData(final TimeData prev) {
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

    /** Computes the difference between the local system clock, and the web-servers clock, in MS. */
    public static Long getLocalToUTCTimeOffsetInMSOverHTTP() {
        final List<TimeDuration> results = new ArrayList<>();
        final Calendar cal = Calendar.getInstance();
        for (final String url : WEBSITES) {
            HttpURLConnection conn = null;
            try {
                final URL tmp = new URL(url);
                final long start = System.currentTimeMillis();
                conn = (HttpURLConnection) tmp.openConnection();
                final long end = System.currentTimeMillis();
                final String date = conn.getHeaderField("Date");
                if ((date != null)
                        && (date.endsWith("GMT") || date.endsWith("UTC"))) {
                    final long duration = end - start;
                    // Date.parse(date) converts the time to local, so we don't
                    // get UTC out, even if we put UTC in!
                    @SuppressWarnings("deprecation")
                    final long websiteTime = Date.parse(date) + duration / 2;
                    cal.setTimeInMillis(websiteTime);
                    final long local2UTC = -(cal.get(Calendar.ZONE_OFFSET) + cal
                            .get(Calendar.DST_OFFSET));
                    final long websiteUTCTime = websiteTime + local2UTC;
                    final long localTime = (start + end) / 2;
                    final long diff = websiteUTCTime - localTime;
                    results.add(new TimeDuration(duration, diff));
                }
            } catch (final Throwable t) {
                t.printStackTrace();
                // Ignore
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (final Throwable t) {
                        // Ignore
                    }
                }
            }
        }
        Collections.sort(results);
        long sumDiff = 0;
        int countDiff = 0;
        for (final TimeDuration timeDuration : results) {
            sumDiff += timeDuration.diff;
            countDiff++;
            if (countDiff == 3) {
                break;
            }
        }
        if (countDiff == 0) {
            // FAIL! No response!
            return null;
        }
        return sumDiff / countDiff;
    }

    /** Computes the difference between the local system clock, and the time-servers clock, in MS. */
    public static Long getLocalToUTCTimeOffsetInMSOverNTP() {
        final NTPUDPClient client = new NTPUDPClient();
//        final Calendar cal = Calendar.getInstance(DEFAULT_LOCAL);
        // We want to timeout if a response takes longer than 10 seconds
        client.setDefaultTimeout(3000);
        long offsetSum = 0L;
        int offsetCount = 0;
        long bestDelay = Long.MAX_VALUE;
        long bestOffset = Long.MAX_VALUE;
        try {
            client.open();
            for (int i = 0; i < NTP_POOL.length; i++) {
                try {
                    final InetAddress hostAddr = InetAddress
                            .getByName(NTP_POOL[i]);
                    final TimeInfo info = client.getTime(hostAddr);
                    info.computeDetails();
                    final Long offsetValue = info.getOffset();
                    final Long delayValue = info.getDelay();
                    if ((delayValue != null) && (offsetValue != null)) {
//                        cal.setTimeInMillis(offsetValue
//                                + System.currentTimeMillis());
//                        final long local2UTC = -(cal.get(Calendar.ZONE_OFFSET) + cal
//                                .get(Calendar.DST_OFFSET));
                        if (delayValue <= 100L) {
                            offsetSum += offsetValue;// + local2UTC;
                            offsetCount++;
                        }
                        if (delayValue < bestDelay) {
                            bestDelay = delayValue;
                            bestOffset = offsetValue;// + local2UTC;
                        }
                    }
                } catch (final IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } catch (final SocketException e) {
            e.printStackTrace();
            // NTPUDPClient can't even open at all!?!
        } finally {
            client.close();
        }
        if (offsetCount > 0) {
            return offsetSum / offsetCount;
        }
        // OK, not good result. Any result at all?
        if (bestDelay != Long.MAX_VALUE) {
            return bestOffset;
        }
        // FAIL!
        return null;
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
    public static Long getLocalToUTCTimeOffsetInMS() {
        // Try best source first: NTP
        Long result = getLocalToUTCTimeOffsetInMSOverNTP();
        if (result == null) {
            // Try alternate source: HTTP
            result = getLocalToUTCTimeOffsetInMSOverHTTP();
        }
        return result;
    }

    /**
     * Computes the difference between the System.nanoTime(), and the UTC/GMT time, in nanoseconds.
     *
     * @see getLocalToUTCTimeOffsetInMS()
     *
     * @return An offset, in nanoseconds, from the System.nanoTime() to the UTC/GMT time, or null on failure.
     */
    public static Long getNanoTimeToUTCTimeOffsetInNS() {
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
                    bestOffsetInMS = nextMS - nextNS / 1000000L;
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
        return sumOffsetInMS * (1000000L / LOOPS);
    }

    /**
     * Returns an approximation of the *current UTC time* at nano-seconds scale.
     */
    public static long currentTimeNanos() {
        if (useInternetTime) {
            final TimeData td = TIME_DATA.get();
            if (td == null) {
                throw new IllegalStateException("No time data available!");
            }
            while (true) {
                final long last = LAST_NANO_TIME.get();
                final long now = td.utcNanos();
                if (now >= last) {
                    if (LAST_NANO_TIME.compareAndSet(last, now)) {
                        return now;
                    }
                } else {
                    // Ouch! utcTimeNanos() went backward!
                    final long diff = now - last;
                    if (diff < -1000000L) {
                        System.err.println("Time went backward by " + diff
                                + " nano-seconds.");
                    }
                    return last;
                }
            }
        }
        if (!SETUP_WAS_CALLED.get()) {
            throw new IllegalStateException(
                    "setup() must be called before first usage!");
        }
        // Do NOT use Internet Time ...
        return System.currentTimeMillis() * 1000000L;
    }

    private CurrentTimeNanos() {
        // NOP
    }

//    public static void main(final String[] args) {
//        final long nanoA = System.nanoTime();
//        final Long offset1 = getNanoTimeToUTCTimeOffsetInNS();
//        final long nanoB = System.nanoTime();
//        final long duration1 = (nanoB - nanoA);
//        final TimeData td1 = new TimeData(offset1, nanoA + (duration1 / 2L),
//                null);
//        final long utc1 = td1.utcNanos();
//
//        try {
//            Thread.sleep(60000);
//        } catch (final InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        final long nanoC = System.nanoTime();
//        final Long offset2 = getNanoTimeToUTCTimeOffsetInNS();
//        final long nanoD = System.nanoTime();
//        final long duration2 = (nanoD - nanoC);
//        final TimeData td2 = new TimeData(offset2, nanoC + (duration2 / 2L),
//                td1);
//        final long utc2 = td2.utcNanos();
//
//        System.out.println("utc1:        " + utc1);
//        System.out.println("utc2:        " + utc2);
//        System.out.println("utc2 - utc1: " + (utc2 - utc1));
//        System.out.println("Expected:    " + (nanoC - nanoB));
//        System.out.println("offset1:     " + offset1);
//        System.out.println("offset2:     " + offset2);
//        System.out.println("duration1:   " + duration1);
//        System.out.println("duration2:   " + duration2);
//        System.out.println("utc1:        " + new Date(utc1 / 1000000L));
//        System.out.println("utc2:        " + new Date(utc2 / 1000000L));
//        System.out.println("toGMTString: " + new Date().toGMTString());
//        System.out.println("td1:         " + td1);
//        System.out.println("td2:         " + td2);

//        System.out.println("getLocalToUTCTimeDiffMSOverHTTP(): "
//                + getLocalToUTCTimeOffsetInMSOverHTTP());
//        System.out.println("getLocalToUTCTimeDiffMSOverNTP() : "
//                + getLocalToUTCTimeOffsetInMSOverNTP());
//        final long timeMS = System.currentTimeMillis();
//        final long timeNS = timeMS * 1000000L;
//        final long nanoTime = currentTimeNanos();
//        final long httpDiff = getLocalToUTCTimeDiffMSOverHTTP();
//        final long utcTimeNanos = utcTimeNanos();
//        System.out.println("timeMS:     " + timeMS);
//        System.out.println("timeNS:     " + timeNS);
//        System.out.println("nanoTime:   " + nanoTime);
//        System.out.println("UTC-nano:   " + utcTimeNanos);
//        System.out.println("UTC:        " + utcTimeNanos / 1000000L);
//        System.out.println("UTC-HTTP:   " + (timeMS + httpDiff));
//        System.out.println("diff:       "
//                + ((utcTimeNanos / 1000000L) - (timeMS + httpDiff)));
//        System.out.println("httpDiff:  " + httpDiff);
//        System.out.println("MAX_VALUE:  " + Long.MAX_VALUE);
//        System.out.println("UTC OFFSET: " + OFFSET_NS_LOCAL_TO_UTC
//                / 1000000000L);
//    }
}
