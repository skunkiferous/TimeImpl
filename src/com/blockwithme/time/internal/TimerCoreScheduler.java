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
                final long duration = System.nanoTime() - start;
                if (duration > 1000000L) {
                    LOG.warn("Task " + task + " took longer then 1ms: "
                            + duration / 1000000.0 + " ms");
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
     * @param tickDurationNanos The duration of one clock tick, in nanoseconds.
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
    public Task<Runnable> scheduleNS(final Runnable task,
            final Handler errorHandler, final long delayNS) {
        final MyTimerTask result = new MyTimerTask(task, errorHandler);
        timer.schedule(result, LightweightSchedulerImpl.roundToMS(delayNS));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled " + task + " with error handler "
                    + errorHandler + " and delay " + delayNS + " ns");
        }
        return result;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.internal.SchedulerImpl#scheduleAtFixedPeriodNS(java.lang.Object, long, long)
     */
    @Override
    public Task<Runnable> scheduleAtFixedPeriodNS(final Runnable task,
            final Handler errorHandler, final long delayNS, final long periodNS) {
        final MyTimerTask result = new MyTimerTask(task, errorHandler);
        timer.schedule(result, LightweightSchedulerImpl.roundToMS(delayNS),
                LightweightSchedulerImpl.roundToMS(periodNS));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled " + task + " with error handler "
                    + errorHandler + " and delay " + delayNS
                    + " ns at fixed periode of " + periodNS + " ns");
        }
        return result;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.internal.SchedulerImpl#scheduleAtFixedRateNS(java.lang.Object, long, long)
     */
    @Override
    public Task<Runnable> scheduleAtFixedRateNS(final Runnable task,
            final Handler errorHandler, final long delayNS, final long periodNS) {
        final MyTimerTask result = new MyTimerTask(task, errorHandler);
        timer.scheduleAtFixedRate(result,
                LightweightSchedulerImpl.roundToMS(delayNS),
                LightweightSchedulerImpl.roundToMS(periodNS));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled " + task + " with error handler "
                    + errorHandler + " and delay " + delayNS
                    + " ns at fixed rate of " + periodNS + " ns");
        }
        return result;
    }
}
