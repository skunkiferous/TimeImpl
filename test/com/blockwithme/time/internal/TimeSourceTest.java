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

import java.util.concurrent.atomic.AtomicLong;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Task;
import com.blockwithme.time.Time;
import com.blockwithme.time.TimeListener;
import com.blockwithme.time.TimeSource;

/**
 * @author monster
 *
 */
public class TimeSourceTest extends TestBase {
    public void testTimeSource() throws Exception {
        try (final ClockService impl = newClockService()) {
            try (final Scheduler sched = impl.newScheduler("sched", null)) {
                try (final TimeSource ts1 = sched.newTimeSource("ts1")) {
                    // With a default of 20 ticks per second, a ratio of 5
                    // gives you 4 ticks per second.
                    ts1.setParentRatio(5);
                    try (final TimeSource ts2 = ts1.newTimeSource("ts2")) {
                        // With a 4 ticks per second in the parent time source,
                        // a ratio of -2 gives you 2 ticks per second, going backward.
                        ts2.setParentRatio(-2);
                        final AtomicLong tl1LastTick = new AtomicLong();
                        final TimeListener tl1 = new TimeListener() {
                            @Override
                            public void onTimeChange(final Time tick) {
                                tl1LastTick.set(tick.ticks);
                                System.out.println("TS1: " + tick.ticks + " "
                                        + tick.tickTimeInstant());
                            }
                        };
                        try (final Task<TimeListener> task1 = ts1
                                .registerListener(tl1)) {
                            final AtomicLong tl2LastTick = new AtomicLong();
                            final TimeListener tl2 = new TimeListener() {
                                @Override
                                public void onTimeChange(final Time tick) {
                                    tl2LastTick.set(tick.ticks);
                                    System.out.println("TS2: " + tick.ticks
                                            + " " + tick.tickTimeInstant());
                                }
                            };
                            try (final Task<TimeListener> task2 = ts2
                                    .registerListener(tl2)) {
                                sleep(3000);
                                assertEquals(12, tl1LastTick.get());
                                assertEquals(-6, tl2LastTick.get());
                            }
                        }
                    }
                }
            }
        }
    }
}
