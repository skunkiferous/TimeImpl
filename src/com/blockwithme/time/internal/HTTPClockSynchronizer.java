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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blockwithme.time.ClockSynchronizer;

/**
 * A ClockSynchronizer that uses web servers, to get the time.
 *
 * @author monster
 */
public class HTTPClockSynchronizer implements ClockSynchronizer {

    /** Logger. */
    private static final Logger LOG = LoggerFactory
            .getLogger(HTTPClockSynchronizer.class);

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

    /** List of possible websites to query, when NTP does not work. */
    private static final String[] WEBSITES = new String[] {
            "http://google.com", "http://wikipedia.org", "http://amazon.com",
            "http://facebook.com", "http://bing.com", "http://youtube.com" };

    /** The web servers */
    private final String[] webServers;

    /**
     * Creates a HTTPClockSynchronizer using the default web servers.
     */
    public HTTPClockSynchronizer() {
        this(WEBSITES);
    }

    /** toString() */
    @Override
    public String toString() {
        return "HTTPClockSynchronizer(webServers=" + Arrays.asList(webServers)
                + ")";
    }

    /**
     * Creates a HTTPClockSynchronizer using the given web servers.
     */
    public HTTPClockSynchronizer(final String[] theWebServers) {
        if (theWebServers == null) {
            throw new NullPointerException("theWebServers");
        }
        if (theWebServers.length == 0) {
            throw new IllegalArgumentException("theWebServers empty!");
        }
        webServers = theWebServers;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockSynchronizer#expectedPrecision()
     */
    @Override
    public long expectedPrecision() {
        return 1000;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockSynchronizer#getLocalToUTCTimeOffset()
     */
    @Override
    public long getLocalToUTCTimeOffset() throws Exception {
        final List<TimeDuration> results = new ArrayList<>();
        final Calendar cal = Calendar.getInstance();
        Throwable lastException = null;
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
                LOG.error("Error reading tiem through HTTP", t);
                lastException = t;
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
            throw new Exception("Failed to get the time using webservers",
                    lastException);
        }
        return sumDiff / countDiff;
    }
}
