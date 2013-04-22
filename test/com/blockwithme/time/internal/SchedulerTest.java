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

import junit.framework.TestCase;

import org.threeten.bp.Clock;

import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Task;

/**
 * Tests the Scheduler.
 *
 * @author monster
 */
public class SchedulerTest extends TestCase {

    private volatile int task1;

    private volatile int task2;

    private volatile int task3;

    private volatile int task4;

    private volatile int task5;

    private volatile Task t1;

    private volatile Task t2;

    private volatile Task t3;

    private volatile Task t4;

    private volatile Task t5;

    private static void sleep(final long sleep) {
        try {
            Thread.sleep(sleep);
        } catch (final InterruptedException e) {
            // NOP
        }
    }

    private static void close(final AutoCloseable ac) {
        try {
            ac.close();
        } catch (final Exception e) {
            // NOP
        }
    }

    private void clear() {
        task1 = task2 = task3 = task4 = task5 = 0;
        t1 = t2 = t3 = t3 = t5 = null;
    }

    public void testSchedule() {
        clear();

        final ClockServiceImpl impl = new ClockServiceImpl(true, false,
                new TimerCoreScheduler(), new NTPClockSynchronizer(),
                new HTTPClockSynchronizer());

        final Clock clock = impl.clock();

        final Scheduler sched = impl.createNewScheduler(null);

        sched.schedule(new Runnable() {
            @Override
            public void run() {
                task1++;
            }
        }, 0);

        sched.schedule(new Runnable() {
            @Override
            public void run() {
                task2++;
            }
        }, 100);

        sched.schedule(new Runnable() {
            @Override
            public void run() {
                task3++;
            }
        }, clock.instant().plusMillis(100));

        sched.schedule(new Runnable() {
            @Override
            public void run() {
                task4++;
            }
        }, clock.instant().minusMillis(100));

        sched.schedule(new Runnable() {
            @Override
            public void run() {
                task5++;
            }
        }, impl.date());

        sleep(1000);

        assertEquals(1, task1);
        assertEquals(1, task2);
        assertEquals(1, task3);
        assertEquals(1, task4);
        assertEquals(1, task5);
    }

    public void testClose() throws Exception {
        clear();

        final ClockServiceImpl impl = new ClockServiceImpl(true, false,
                new TimerCoreScheduler(), new NTPClockSynchronizer(),
                new HTTPClockSynchronizer());

        final Clock clock = impl.clock();

        final Scheduler sched = impl.createNewScheduler(null);

        sched.schedule(new Runnable() {
            @Override
            public void run() {
                task1++;
            }
        }, 100);

        sched.schedule(new Runnable() {
            @Override
            public void run() {
                task2++;
            }
        }, clock.instant().plusMillis(100));

        sched.close();

        sleep(1000);

        assertEquals(0, task1);
        assertEquals(0, task2);
    }

    public void testScheduleAtFixedPeriod() {
        clear();

        final ClockServiceImpl impl = new ClockServiceImpl(true, false,
                new TimerCoreScheduler(), new NTPClockSynchronizer(),
                new HTTPClockSynchronizer());

        final Clock clock = impl.clock();

        final Scheduler sched = impl.createNewScheduler(null);

        t1 = sched.scheduleAtFixedPeriod(new Runnable() {
            @Override
            public void run() {
                task1++;
                if (task1 == 3) {
                    close(t1);
                }
            }
        }, 0, 100);

        t2 = sched.scheduleAtFixedPeriod(new Runnable() {
            @Override
            public void run() {
                task2++;
                if (task2 == 3) {
                    close(t2);
                }
            }
        }, 100, 100);

        t3 = sched.scheduleAtFixedPeriod(new Runnable() {
            @Override
            public void run() {
                task3++;
                if (task3 == 3) {
                    close(t3);
                }
            }
        }, clock.instant().plusMillis(100), 100);

        t4 = sched.scheduleAtFixedPeriod(new Runnable() {
            @Override
            public void run() {
                task4++;
                if (task4 == 3) {
                    close(t4);
                }
            }
        }, clock.instant().minusMillis(100), 100);

        t5 = sched.scheduleAtFixedPeriod(new Runnable() {
            @Override
            public void run() {
                task5++;
                if (task5 == 3) {
                    close(t5);
                }
            }
        }, impl.date(), 100);

        sleep(1000);

        assertEquals(3, task1);
        assertEquals(3, task2);
        assertEquals(3, task3);
        assertEquals(3, task4);
        assertEquals(3, task5);
    }

    public void testScheduleAtFixedRate() {
        clear();

        final ClockServiceImpl impl = new ClockServiceImpl(true, false,
                new TimerCoreScheduler(), new NTPClockSynchronizer(),
                new HTTPClockSynchronizer());

        final Clock clock = impl.clock();

        final Scheduler sched = impl.createNewScheduler(null);

        t1 = sched.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                task1++;
                if (task1 == 3) {
                    close(t1);
                }
            }
        }, 0, 100);

        t2 = sched.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                task2++;
                if (task2 == 3) {
                    close(t2);
                }
            }
        }, 100, 100);

        t3 = sched.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                task3++;
                if (task3 == 3) {
                    close(t3);
                }
            }
        }, clock.instant().plusMillis(100), 100);

        t4 = sched.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                task4++;
                if (task4 == 3) {
                    close(t4);
                }
            }
        }, clock.instant().minusMillis(100), 100);

        t5 = sched.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                task5++;
                if (task5 == 3) {
                    close(t5);
                }
            }
        }, impl.date(), 100);

        sleep(1000);

        assertEquals(3, task1);
        assertEquals(3, task2);
        assertEquals(3, task3);
        assertEquals(3, task4);
        assertEquals(3, task5);
    }
}
