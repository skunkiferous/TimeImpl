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
import com.blockwithme.time.Interval;
import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Task;
import com.blockwithme.time.Time;
import com.blockwithme.time.TimeListener;
import com.blockwithme.time.Timeline;

/**
 * @author monster
 *
 */
public class TimelineTest extends TestBase {
    public void testDerivedTimeline() throws Exception {
        try (final ClockService impl = newClockService()) {
            try (final Scheduler sched = impl.newScheduler("sched", null)) {
                try (final Timeline ts1 = impl.coreTimeline()
                        .newChildTimeline(false, sched).setTicksPerSecond(4)
                        .create("ts1")) {
                    try (final Timeline ts2 = ts1
                            .newChildTimeline(false, sched).setLocalTickStep(2)
                            .setLocalTickScaling(-1).create("ts2")) {
                        // With a 4 ticks per second in the parent timeline,
                        // a ratio of -2 gives you 2 ticks per second, going backward.
                        final AtomicLong tl1LastTick = new AtomicLong();
                        final AtomicLong tl1LastTickTime = new AtomicLong();
                        final TimeListener tl1 = new TimeListener() {
                            @Override
                            public void onTimeChange(final Time tick) {
                                tl1LastTick.set(tick.runningElapsedTicks);
                                tl1LastTickTime.set(Math.round(tick.time));
                                System.out.println("TS1: "
                                        + tick.runningElapsedTicks + " "
                                        + ((double) tick.tickDuration)
                                        / Time.MILLI_MUS + " " + tick.time
                                        + " " + tick.creationInstant());
                            }
                        };
                        try (final Task<TimeListener> task1 = ts1
                                .registerListener(tl1)) {
                            final AtomicLong tl2LastTick = new AtomicLong();
                            final AtomicLong tl2LastTickTime = new AtomicLong();
                            final TimeListener tl2 = new TimeListener() {
                                @Override
                                public void onTimeChange(final Time tick) {
                                    tl2LastTick.set(tick.runningElapsedTicks);
                                    tl2LastTickTime.set(Math.round(tick.time));
                                    System.out.println("TS2: "
                                            + tick.runningElapsedTicks + " "
                                            + ((double) tick.tickDuration)
                                            / Time.MILLI_MUS + " " + tick.time
                                            + " " + tick.creationInstant());
                                }
                            };
                            try (final Task<TimeListener> task2 = ts2
                                    .registerListener(tl2)) {
                                sleep(3020);
                                assertEquals(12, tl1LastTick.get());
                                assertEquals(6, tl2LastTick.get());
                                assertEquals(12, tl1LastTickTime.get());
                                assertEquals(-6, tl2LastTickTime.get());
                            }
                        }
                    }
                }
            }
        }
    }

    public void testPauseAndComputedRealTime() throws Exception {
        try (final ClockService impl = newClockService()) {
            try (final Scheduler sched = impl.newScheduler("sched", null)) {
                try (final Timeline t = impl.coreTimeline()
                        .newChildTimeline(false, sched).setTicksPerSecond(10)
                        .setLocalTickScaling(10.0).paused().create("t")) {
                    System.out.println(t);
                    final AtomicLong tLastTick = new AtomicLong();
                    final TimeListener tl = new TimeListener() {
                        @Override
                        public void onTimeChange(final Time tick) {
                            tLastTick.set(tick.runningElapsedTicks);
                            System.out.println("NOW:               "
                                    + impl.clock().instant());
                            System.out.println("TS1: "
                                    + tick.runningElapsedTicks + " "
                                    + ((double) tick.tickDuration)
                                    / Time.MILLI_MUS + " "
                                    + tick.creationInstant());
                        }
                    };
                    try (final Task<TimeListener> task = t.registerListener(tl)) {
                        t.unpause();
                        sleep(1050);
                    }
                    final Time lastTime = t.lastTick();
                    final long now = impl.currentTimeMicros();
                    final double diffTimeSec = ((double) lastTime.creationTime - now)
                            / Time.SECOND_MUS;
                    System.out
                            .println("Ticks: " + lastTime.runningElapsedTicks);
                    System.out.println("diffTimeSec: " + diffTimeSec);
                    System.out.println("diffTimeSec/tick: "
                            + (diffTimeSec / lastTime.runningElapsedTicks));
                }
            }
        }
    }

    public void testTimelineInterval() throws Exception {
        try (final ClockService impl = newClockService()) {
            try (final Scheduler sched = impl.newScheduler("sched", null)) {
                try (final Timeline t = impl.coreTimeline()
                        .newChildTimeline(false, sched)
                        .setFixedDurationTicks(100).paused().create("t")) {
                    final Interval i = t.toInterval();
                    System.out.println(i);
                    final long start = i.start();
                    final long end = i.end();
                    assertTrue(i.beforeInterval(start - 1));
                    assertTrue(i.inInterval(start));
                    assertTrue(i.inInterval(end));
                    assertTrue(i.afterInterval(end + 1));
                }
            }
        }
    }

    public void testRealTimeInterval() throws Exception {
        try (final ClockService impl = newClockService()) {
            final long before = impl.currentTimeMicros();
            impl.sleepMicros(1);
            final Interval i = impl.newInterval(100);
            final long in = impl.currentTimeMicros();
            impl.sleepMicros(100);
            final long after = impl.currentTimeMicros();
            System.out.println(i);
            System.out.println("before: " + before);
            System.out.println("in: " + in);
            System.out.println("after: " + after);
            assertTrue(i.beforeInterval(before));
            assertTrue(i.inInterval(in));
            assertTrue(i.afterInterval(after));
        }
    }
}
