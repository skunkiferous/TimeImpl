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

/** Contains all the data we need to compute the current UTC time, using System.nanoTime(). */
final class TimeData {
    private final long microTimeQuery;
    private final long microTimeToUTCTimeOffsetInMUS;

    // Last query, not previous of last (prevMicroTimeQuery)
    private final long t0;
    // *Next* query, which should not have happened yet!
    // microTimeQuery + (microTimeQuery - prevMicroTimeQuery)
    private final long t1;
    // Previous of last offset (microTimeToUTCTimeOffsetInMUS)
    private final long o0;
    // Last offset (prev.microTimeToUTCTimeOffsetInMUS)
    private final long o1;

    private final long o1_minus_o0;
    private final long t1_minus_t0;
    private final double o1_minus_o0_div_t1_minus_t0;
    /** Did we have a prev at all? */
    private final boolean prev;

    public TimeData(final long microTimeToUTCTimeOffsetInMUS,
            final long microTimeQuery, final TimeData prev) {
        this.microTimeToUTCTimeOffsetInMUS = microTimeToUTCTimeOffsetInMUS;
        this.microTimeQuery = microTimeQuery;
        if (prev != null) {
            final long prevMicroTimeQuery = prev.microTimeQuery;
            final long prevMicroTimeToUTCTimeOffsetInMUS = prev.microTimeToUTCTimeOffsetInMUS;
            final long offsetDiff = microTimeToUTCTimeOffsetInMUS
                    - prevMicroTimeToUTCTimeOffsetInMUS;
            final long microDiff = microTimeQuery - prevMicroTimeQuery;
            if ((offsetDiff != 0) && (microDiff != 0)) {
                this.prev = true;

                // Time to do some good old linear interpolation ...
                // Note that we don't know the future offset, so we do as if the
                // previous offset was the *next* offset, and the one before was the previous.

                // Last query, not previous of last
                t0 = microTimeQuery;
                // *Next* query, which should not have happened yet!
                t1 = microTimeQuery + (microTimeQuery - prevMicroTimeQuery);
                // Previous of last offset
                o0 = prevMicroTimeToUTCTimeOffsetInMUS;
                // Last offset
                o1 = microTimeToUTCTimeOffsetInMUS;

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
        return "TimeData(microTimeQuery=" + microTimeQuery
                + ", microTimeToUTCTimeOffsetInMUS="
                + microTimeToUTCTimeOffsetInMUS + ", t0=" + t0 + ", t1=" + t1
                + ", o0=" + o0 + ", o1=" + o1 + ", o1_minus_o0=" + o1_minus_o0
                + ", t1_minus_t0=" + t1_minus_t0
                + ", o1_minus_o0_div_t1_minus_t0="
                + o1_minus_o0_div_t1_minus_t0 + ")";
    }

    /** Computes the current time UTC in microseconds. */
    public long utcMicros() {
        final long micros = System.nanoTime() / 1000L;
        if (!prev) {
            return micros + microTimeToUTCTimeOffsetInMUS;
        }
        // Actual time
        final long t = micros;
        // interpolated offset
//            final long o = o0 + o1_minus_o0 * (t - t0) / t1_minus_t0;
        final long o = o0 + (long) (o1_minus_o0_div_t1_minus_t0 * (t - t0));
        // "Actual time" + "interpolated offset" = UTC micro time
        return micros + o;
    }
}