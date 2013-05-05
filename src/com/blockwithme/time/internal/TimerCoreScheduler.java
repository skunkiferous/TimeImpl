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

import java.util.Timer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blockwithme.time.Scheduler.Handler;
import com.blockwithme.time.Task;
import com.blockwithme.time.Time;

/**
 * TimerCoreScheduler implements a CoreScheduler using a Timer.
 *
 * This implementation assumes, that despite a possible bad system time,
 * the java.util.Timer class can still handle relative delays, rather then
 * absolute date-times, correctly.
 *
 * @author monster
 */
@Singleton
public class TimerCoreScheduler extends AbstractCoreScheduler {

    /** Logger. */
    private static final Logger LOG = LoggerFactory
            .getLogger(TimerCoreScheduler.class);

    /** Implements the TimerTask wanted by the Timer. */
    private static final class MyTimerTask extends java.util.TimerTask
            implements Task<Runnable> {

        /** The task to run. */
        private final Runnable task;

        /** The error handler. */
        private final Handler errorHandler;

        /** Defines a MyTimerTask. */
        public MyTimerTask(final Runnable task, final Handler errorHandler) {
            if (task == null) {
                throw new NullPointerException("task");
            }
            if (errorHandler == null) {
                throw new NullPointerException("errorHandler");
            }
            this.task = task;
            this.errorHandler = errorHandler;
        }

        @Override
        public void run() {
            final long start = System.nanoTime();
            try {
                task.run();
            } catch (final Throwable t) {
                errorHandler.onError(task, t);
            } finally {
                final long duration = (System.nanoTime() - start) / 1000L;
                if (duration > Time.MILLI_MUS) {
                    LOG.warn("Task " + task + " took longer then 1ms: "
                            + duration / ((double) Time.MILLI_MUS) + " ms");
                }
            }
        }

        /* (non-Javadoc)
         * @see java.lang.AutoCloseable#close()
         */
        @Override
        public void close() throws Exception {
            cancel();
        }

        /* (non-Javadoc)
         * @see com.blockwithme.time.Task#task()
         */
        @Override
        public Runnable task() {
            return task;
        }
    }

    /** The Timer, implementing the nScheduler. */
    private final Timer timer;

    /**
     * @param tickDurationNanos The duration of one clock tick, in microseconds.
     */
    @Inject
    public TimerCoreScheduler(@Named("ticksPerSecond") final int ticksPerSecond) {
        super(ticksPerSecond);
        timer = new Timer(true);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Scheduler#close()
     */
    @Override
    public void close() {
        timer.cancel();
        super.close();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.internal.SchedulerImpl#schedule2(java.lang.Object, long)
     */
    @Override
    public Task<Runnable> scheduleMUS(final Runnable task,
            final Handler errorHandler, final long delayMUS) {
        final MyTimerTask result = new MyTimerTask(task, errorHandler);
        timer.schedule(result, LightweightSchedulerImpl.roundToMS(delayMUS));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled " + task + " with error handler "
                    + errorHandler + " and delay " + delayMUS + " mus");
        }
        return result;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.internal.SchedulerImpl#scheduleAtFixedPeriodMUS(java.lang.Object, long, long)
     */
    @Override
    public Task<Runnable> scheduleAtFixedPeriodMUS(final Runnable task,
            final Handler errorHandler, final long delayMUS,
            final long periodMUS) {
        final MyTimerTask result = new MyTimerTask(task, errorHandler);
        timer.schedule(result, LightweightSchedulerImpl.roundToMS(delayMUS),
                LightweightSchedulerImpl.roundToMS(periodMUS));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled " + task + " with error handler "
                    + errorHandler + " and delay " + delayMUS
                    + " ns at fixed periode of " + periodMUS + " mus");
        }
        return result;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.internal.SchedulerImpl#scheduleAtFixedRateMUS(java.lang.Object, long, long)
     */
    @Override
    public Task<Runnable> scheduleAtFixedRateMUS(final Runnable task,
            final Handler errorHandler, final long delayMUS,
            final long periodMUS) {
        final MyTimerTask result = new MyTimerTask(task, errorHandler);
        timer.scheduleAtFixedRate(result,
                LightweightSchedulerImpl.roundToMS(delayMUS),
                LightweightSchedulerImpl.roundToMS(periodMUS));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled " + task + " with error handler "
                    + errorHandler + " and delay " + delayMUS
                    + " ns at fixed rate of " + periodMUS + " mus");
        }
        return result;
    }
}
