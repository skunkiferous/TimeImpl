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

import java.net.InetAddress;
import java.util.Arrays;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blockwithme.time.ClockSynchronizer;

/**
 * A ClockSynchronizer that uses NTP servers, to get the time.
 *
 * @author monster
 */
public class NTPClockSynchronizer implements ClockSynchronizer {

    /** Logger. */
    private static final Logger LOG = LoggerFactory
            .getLogger(NTPClockSynchronizer.class);

    /** The default NTP time server pool. */
    private static final String[] NTP_POOL = new String[] { "0.pool.ntp.org",
            "1.pool.ntp.org", "2.pool.ntp.org" };

    /** The NTP time server pool */
    private final String[] ntpPool;

    /**
     * Creates a NTPClockSynchronizer using the default NTP servers pool.
     */
    public NTPClockSynchronizer() {
        this(NTP_POOL);
    }

    /** toString() */
    @Override
    public String toString() {
        return "NTPClockSynchronizer(ntpPool=" + Arrays.asList(ntpPool) + ")";
    }

    /**
     * Creates a NTPClockSynchronizer using the given NTP servers pool.
     */
    public NTPClockSynchronizer(final String[] theNtpPool) {
        if (theNtpPool == null) {
            throw new NullPointerException("theNtpPool");
        }
        if (theNtpPool.length == 0) {
            throw new IllegalArgumentException("theNtpPool empty!");
        }
        ntpPool = theNtpPool;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockSynchronizer#expectedPrecision()
     */
    @Override
    public long expectedPrecision() {
        return 500;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockSynchronizer#getLocalToUTCTimeOffset()
     */
    @Override
    public long getLocalToUTCTimeOffset() throws Exception {
        final NTPUDPClient client = new NTPUDPClient();
//      final Calendar cal = Calendar.getInstance(DEFAULT_LOCAL);
        // We want to timeout if a response takes longer than 10 seconds
        client.setDefaultTimeout(3000);
        long offsetSum = 0L;
        int offsetCount = 0;
        long bestDelay = Long.MAX_VALUE;
        long bestOffset = Long.MAX_VALUE;
        Throwable lastException = null;
        try {
            client.open();
            for (int i = 0; i < ntpPool.length; i++) {
                try {
                    final InetAddress hostAddr = InetAddress
                            .getByName(ntpPool[i]);
                    final TimeInfo info = client.getTime(hostAddr);
                    info.computeDetails();
                    final Long offsetValue = info.getOffset();
                    final Long delayValue = info.getDelay();
                    if ((delayValue != null) && (offsetValue != null)) {
//                      cal.setTimeInMillis(offsetValue
//                              + System.currentTimeMillis());
//                      final long local2UTC = -(cal.get(Calendar.ZONE_OFFSET) + cal
//                              .get(Calendar.DST_OFFSET));
                        if (delayValue <= 100L) {
                            offsetSum += offsetValue;// + local2UTC;
                            offsetCount++;
                        }
                        if (delayValue < bestDelay) {
                            bestDelay = delayValue;
                            bestOffset = offsetValue;// + local2UTC;
                        }
                    }
                } catch (final Throwable t) {
                    LOG.error("Error reading tiem through NTP", t);
                    lastException = t;
                }
            }
        } catch (final Throwable t) {
            LOG.error("Error reading tiem through NTP", t);
            lastException = t;
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
        throw new Exception("Failed to get NTP time", lastException);
    }
}
